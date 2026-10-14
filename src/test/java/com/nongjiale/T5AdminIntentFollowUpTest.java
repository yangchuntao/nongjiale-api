package com.nongjiale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T5 管理端意向跟进：客服确认+备注时间线、乐观锁 409、非管理端/非法流转拒绝。
 * MockMvc，不占用 8080。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class T5AdminIntentFollowUpTest {

    private static final Long ON_SALE_HOMESTAY_ID = 1L;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

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
        return objectMapper.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8))
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
        return objectMapper.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8))
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

    /** 游客提交一条待跟进意向，返回 intentId。 */
    private long createPendingIntent(LocalDate checkIn) throws Exception {
        String touristToken = loginTourist();
        LocalDate checkOut = checkIn.plusDays(2);
        MvcResult created = mockMvc.perform(post("/api/booking-intents")
                        .header("Authorization", "Bearer " + touristToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(ON_SALE_HOMESTAY_ID, checkIn, checkOut, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("intentId").asLong();
    }

    @Test
    @Order(1)
    @DisplayName("T5: 客服登录 → 确认待跟进意向并写备注 → CONFIRMED，详情备注时间线可见")
    void t5_agent_confirm_with_note_and_timeline() throws Exception {
        long intentId = createPendingIntent(LocalDate.now().plusDays(100));
        String agentToken = loginAdmin("agent");

        MvcResult listed = mockMvc.perform(get("/api/admin/intents")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode listData = objectMapper.readTree(listed.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        assertThat(listData.path("list").isArray() || listData.path("items").isArray()
                || listData.isArray() || listData.path("content").isArray()
                || listData.has("total")).isTrue();

        MvcResult detailBefore = mockMvc.perform(get("/api/admin/intents/" + intentId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        long version = objectMapper.readTree(detailBefore.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("version").asLong();

        String noteText = "已电话确认，客人同意入住";
        MvcResult updated = mockMvc.perform(post("/api/admin/intents/" + intentId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "CONFIRMED",
                                "note", noteText,
                                "version", version))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.id").value(intentId))
                .andReturn();
        long newVersion = objectMapper.readTree(updated.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("version").asLong();
        assertThat(newVersion).isGreaterThan(version);

        MvcResult detailAfter = mockMvc.perform(get("/api/admin/intents/" + intentId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.notes").isArray())
                .andReturn();
        JsonNode notes = objectMapper.readTree(detailAfter.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("notes");
        assertThat(notes.size()).isGreaterThanOrEqualTo(1);
        JsonNode last = notes.get(notes.size() - 1);
        assertThat(last.path("action").asText()).isEqualTo("CONFIRMED");
        assertThat(last.path("note").asText()).isEqualTo(noteText);
        assertThat(last.path("createdAt").asText()).isNotBlank();
        assertThat(last.path("adminId").asLong()).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("T5b: 用旧 version 再次改状态 → 409，不 silent 覆盖")
    void t5b_stale_version_returns_409() throws Exception {
        long intentId = createPendingIntent(LocalDate.now().plusDays(110));
        String agentToken = loginAdmin("agent");

        MvcResult detail = mockMvc.perform(get("/api/admin/intents/" + intentId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        long version = objectMapper.readTree(detail.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("version").asLong();

        mockMvc.perform(post("/api/admin/intents/" + intentId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "CONFIRMED",
                                "note", "第一次确认",
                                "version", version))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        MvcResult conflict = mockMvc.perform(post("/api/admin/intents/" + intentId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "CLOSED",
                                "note", "用旧 version 覆盖应失败",
                                "version", version))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andReturn();

        JsonNode after = objectMapper.readTree(
                mockMvc.perform(get("/api/admin/intents/" + intentId)
                                .header("Authorization", "Bearer " + agentToken))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse().getContentAsString(StandardCharsets.UTF_8)
        ).path("data");
        assertThat(after.path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(conflict.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("刷新");
        boolean closedInTimeline = false;
        for (JsonNode n : after.path("notes")) {
            if ("CLOSED".equals(n.path("action").asText())
                    && n.path("note").asText().contains("用旧 version")) {
                closedInTimeline = true;
            }
        }
        assertThat(closedInTimeline).as("旧 version 冲突不得写入 CLOSED 备注").isFalse();
    }

    @Test
    @Order(3)
    @DisplayName("非管理端 token / 无 token 调跟进接口应失败；非法目标状态 400")
    void t5_auth_and_illegal_transition_rejected() throws Exception {
        long intentId = createPendingIntent(LocalDate.now().plusDays(120));
        String touristToken = loginTourist();
        String agentToken = loginAdmin("agent");

        mockMvc.perform(post("/api/admin/intents/" + intentId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED","note":"no auth","version":0}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/admin/intents")
                        .header("Authorization", "Bearer " + touristToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/admin/intents/" + intentId + "/status")
                        .header("Authorization", "Bearer " + touristToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED","note":"tourist","version":0}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        MvcResult detail = mockMvc.perform(get("/api/admin/intents/" + intentId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andReturn();
        long version = objectMapper.readTree(detail.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("version").asLong();

        mockMvc.perform(post("/api/admin/intents/" + intentId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "PENDING",
                                "note", "非法回退",
                                "version", version))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/admin/intents/" + intentId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}
