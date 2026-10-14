package com.nongjiale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongjiale.repository.BookingIntentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T1 对齐自动化：意向单鉴权 / 幂等 / 下架拒绝 / 客服改房源 403 / 校验 / 公开配置。
 * 使用 MockMvc，不占用 8080。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class T1BookingIntentApiTest {

    private static final Long ON_SALE_HOMESTAY_ID = 1L;
    private static final Long OFF_SALE_HOMESTAY_ID = 3L;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BookingIntentRepository intentRepository;

    private String loginTourist() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000001","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private String loginAdmin(String username) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private String bookingBody(Long homestayId, LocalDate checkIn, LocalDate checkOut, int guests) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "homestayId", homestayId,
                "checkIn", checkIn.toString(),
                "checkOut", checkOut.toString(),
                "guests", guests,
                "travelMode", "自驾",
                "contactName", "演示游客",
                "contactPhone", "13800000001"
        ));
    }

    @Test
    @Order(1)
    @DisplayName("T1b: 无 Authorization 调 POST booking-intents → 401，不入库")
    void t1b_unauthenticated_post_returns_401() throws Exception {
        long before = intentRepository.count();
        LocalDate in = LocalDate.now().plusDays(30);
        LocalDate out = in.plusDays(2);

        mockMvc.perform(post("/api/booking-intents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(ON_SALE_HOMESTAY_ID, in, out, 2)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        assertThat(intentRepository.count()).isEqualTo(before);
    }

    @Test
    @Order(2)
    @DisplayName("config: GET /api/config/public contactPhone=13651814168")
    void config_public_contact_phone() throws Exception {
        mockMvc.perform(get("/api/config/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.contactPhone").value("13651814168"));
    }

    @Test
    @Order(3)
    @DisplayName("T1 数据层: 登录后合法提交一条意向单成功")
    void t1_authenticated_create_succeeds() throws Exception {
        String token = loginTourist();
        LocalDate in = LocalDate.now().plusDays(40);
        LocalDate out = in.plusDays(2);
        long before = intentRepository.count();

        MvcResult r = mockMvc.perform(post("/api/booking-intents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(ON_SALE_HOMESTAY_ID, in, out, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.intentId").isNumber())
                .andExpect(jsonPath("$.data.intentNo").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.contactPhone").value("13651814168"))
                .andReturn();

        assertThat(intentRepository.count()).isEqualTo(before + 1);
        JsonNode data = objectMapper.readTree(r.getResponse().getContentAsString()).path("data");
        assertThat(data.path("intentId").asLong()).isPositive();
    }

    @Test
    @Order(4)
    @DisplayName("T2: 同 user+房源+checkIn 提交两次 → 不新增、同一 intentId")
    void t2_idempotent_same_user_homestay_checkin() throws Exception {
        String token = loginTourist();
        LocalDate in = LocalDate.now().plusDays(50);
        LocalDate out = in.plusDays(3);
        String body = bookingBody(ON_SALE_HOMESTAY_ID, in, out, 3);

        MvcResult first = mockMvc.perform(post("/api/booking-intents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long intentId1 = objectMapper.readTree(first.getResponse().getContentAsString())
                .path("data").path("intentId").asLong();
        long afterFirst = intentRepository.count();

        MvcResult second = mockMvc.perform(post("/api/booking-intents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long intentId2 = objectMapper.readTree(second.getResponse().getContentAsString())
                .path("data").path("intentId").asLong();

        assertThat(intentId2).isEqualTo(intentId1);
        assertThat(intentRepository.count()).isEqualTo(afterFirst);
    }

    @Test
    @Order(5)
    @DisplayName("T3b: 对已下架房源提交 → 拒绝不入库；详情 410")
    void t3b_off_sale_rejected() throws Exception {
        mockMvc.perform(get("/api/homestays/" + OFF_SALE_HOMESTAY_ID))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(410));

        String token = loginTourist();
        LocalDate in = LocalDate.now().plusDays(60);
        LocalDate out = in.plusDays(2);
        long before = intentRepository.count();

        mockMvc.perform(post("/api/booking-intents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(OFF_SALE_HOMESTAY_ID, in, out, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        assertThat(intentRepository.count()).isEqualTo(before);
    }

    @Test
    @Order(6)
    @DisplayName("T6: 客服 token 调管理端改房源 → 403")
    void t6_agent_cannot_update_homestay_status() throws Exception {
        String agentToken = loginAdmin("agent");

        mockMvc.perform(post("/api/admin/homestays/" + ON_SALE_HOMESTAY_ID + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"OFF_SALE","version":0}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @Order(7)
    @DisplayName("T4: guests=0 或非法日期 → 校验失败不入库")
    void t4_validation_rejects_bad_input() throws Exception {
        String token = loginTourist();
        LocalDate in = LocalDate.now().plusDays(70);
        LocalDate out = in.plusDays(2);
        long before = intentRepository.count();

        mockMvc.perform(post("/api/booking-intents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(ON_SALE_HOMESTAY_ID, in, out, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        LocalDate badOut = in; // not after checkIn
        mockMvc.perform(post("/api/booking-intents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(ON_SALE_HOMESTAY_ID, in, badOut, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(intentRepository.count()).isEqualTo(before);
    }
}
