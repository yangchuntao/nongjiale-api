package com.nongjiale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v1.1：商家隔离、上下架申请审核、意向只读 403、客服不能审入驻。
 */
@SpringBootTest
@AutoConfigureMockMvc
class T9MerchantV11ApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(body(result)).path("data");
    }

    private String loginTourist(String phone) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", phone, "password", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andReturn();
        return data(r).path("token").asText();
    }

    private String loginAdmin(String username) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "password"))))
                .andExpect(status().isOk())
                .andReturn();
        return data(r).path("token").asText();
    }

    private String loginMerchant(String phone, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", phone, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.role").value("MERCHANT"))
                .andReturn();
        return data(r).path("token").asText();
    }

    private long createIntent(String token, long homestayId, LocalDate checkIn) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/booking-intents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "homestayId", homestayId,
                                "checkIn", checkIn.toString(),
                                "checkOut", checkIn.plusDays(2).toString(),
                                "guests", 2,
                                "travelMode", "自驾",
                                "contactName", "演示游客",
                                "contactPhone", "13800000001"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return data(r).path("intentId").asLong();
    }

    @Test
    @DisplayName("商家 A 看不到商家 B 的门店，查询串 merchantId 不一致则 403")
    void merchant_isolation_stores() throws Exception {
        String tokenA = loginMerchant("13800001111", "password");
        String tokenB = loginMerchant("13800002222", "password");

        MvcResult listA = mockMvc.perform(get("/api/merchant/stores")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String rawA = body(listA);
        assertThat(rawA).contains("长兴鸿玖农家乐");
        assertThat(rawA).doesNotContain("宜兴翠竹昀民宿");

        mockMvc.perform(get("/api/merchant/stores/2")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.message").value("门店不存在"));

        mockMvc.perform(get("/api/merchant/stores/2")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("宜兴翠竹昀民宿"))
                .andExpect(jsonPath("$.data.merchantId").value(2));

        mockMvc.perform(get("/api/merchant/stores").param("merchantId", "2")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限"));

        mockMvc.perform(get("/api/merchant/stores")
                        .header("Authorization", "Bearer " + loginTourist("13800000001")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("商家只读自家意向；改状态/写跟进 403 且状态不变；手机号脱敏")
    void merchant_intents_are_read_only_and_isolated() throws Exception {
        String tourist = loginTourist("13800000001");
        LocalDate dayA = LocalDate.now().plusDays(220);
        LocalDate dayB = LocalDate.now().plusDays(221);
        long intentA = createIntent(tourist, 1L, dayA);
        long intentB = createIntent(tourist, 2L, dayB);

        String tokenA = loginMerchant("13800001111", "password");
        MvcResult list = mockMvc.perform(get("/api/merchant/intents")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = data(list).path("items");
        boolean sawA = false;
        boolean sawB = false;
        for (JsonNode item : items) {
            if (item.path("id").asLong() == intentA) sawA = true;
            if (item.path("id").asLong() == intentB) sawB = true;
            assertThat(item.has("contactPhone")).isFalse();
            assertThat(item.has("adminId")).isFalse();
        }
        assertThat(sawA).isTrue();
        assertThat(sawB).isFalse();
        assertThat(body(list)).doesNotContain("13800000001");
        assertThat(body(list)).contains("138****0001");

        mockMvc.perform(get("/api/merchant/intents/" + intentB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("意向单不存在"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        String mutate = """
                {"status":"CONFIRMED","note":"商家偷偷改"}
                """;
        mockMvc.perform(post("/api/merchant/intents/" + intentA + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mutate))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("商家不可修改意向或写跟进"));

        mockMvc.perform(post("/api/merchant/intents/" + intentB + "/notes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"探测"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("商家不可修改意向或写跟进"));

        mockMvc.perform(post("/api/merchant/intents/999999/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mutate))
                .andExpect(status().isForbidden());

        String agent = loginAdmin("agent");
        mockMvc.perform(get("/api/admin/intents/" + intentA)
                        .header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.contactPhone").value("13800000001"));
    }

    @Test
    @DisplayName("商家申请上架后游客仍不可订，运营通过后可订；客服不能审")
    void listing_apply_then_operator_approve() throws Exception {
        String tokenA = loginMerchant("13800001111", "password");
        String tokenB = loginMerchant("13800002222", "password");
        MvcResult created = mockMvc.perform(post("/api/merchant/stores")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"甲店待上架山庄","regionId":1,"address":"安吉测试路 1 号","summary":"草稿简介","merchantId":2,"status":"ON_SALE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.merchantId").value(1))
                .andReturn();
        long storeId = data(created).path("id").asLong();

        mockMvc.perform(post("/api/merchant/stores/" + storeId + "/packages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"周末套餐","description":"含早餐","priceFrom":188,"sortOrder":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(storeId));

        mockMvc.perform(get("/api/homestays/" + storeId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(410));

        mockMvc.perform(get("/api/merchant/stores/" + storeId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        MvcResult applied = mockMvc.perform(post("/api/merchant/stores/" + storeId + "/listing-applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"PUBLISH","note":"请上架"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.action").value("PUBLISH"))
                .andReturn();
        long applicationId = data(applied).path("id").asLong();

        mockMvc.perform(get("/api/homestays/" + storeId))
                .andExpect(status().isGone());

        String agent = loginAdmin("agent");
        mockMvc.perform(post("/api/admin/listing-applications/" + applicationId + "/approve")
                        .header("Authorization", "Bearer " + agent))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/api/homestays/" + storeId))
                .andExpect(status().isGone());

        String operator = loginAdmin("operator");
        mockMvc.perform(post("/api/admin/listing-applications/" + applicationId + "/approve")
                        .header("Authorization", "Bearer " + operator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/homestays/" + storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.name").value("甲店待上架山庄"));
    }

    @Test
    @DisplayName("客服不能审入驻；运营通过后首次登录必须改密，初始密码不出现在列表")
    void agent_cannot_review_merchant_application_and_first_login_must_change_password() throws Exception {
        mockMvc.perform(post("/api/merchant-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectName":"新入驻农庄","contactName":"张三","phone":"13900008888","proposedStoreName":"张三农庄","regionId":1,"description":"想入驻"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(post("/api/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13900008888","password":"password"}
                                """))
                .andExpect(status().isUnauthorized());

        String agent = loginAdmin("agent");
        mockMvc.perform(get("/api/admin/merchant-applications")
                        .header("Authorization", "Bearer " + agent))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限"));

        MvcResult queue = mockMvc.perform(get("/api/admin/merchant-applications").param("status", "PENDING")
                        .header("Authorization", "Bearer " + loginAdmin("operator")))
                .andExpect(status().isOk())
                .andReturn();
        long applicationId = -1;
        for (JsonNode item : data(queue).path("items")) {
            if ("13900008888".equals(item.path("phone").asText())) {
                applicationId = item.path("applicationId").asLong();
            }
        }
        assertThat(applicationId).isPositive();

        mockMvc.perform(post("/api/admin/merchant-applications/" + applicationId + "/approve")
                        .header("Authorization", "Bearer " + agent))
                .andExpect(status().isForbidden());

        MvcResult approved = mockMvc.perform(post("/api/admin/merchant-applications/" + applicationId + "/approve")
                        .header("Authorization", "Bearer " + loginAdmin("operator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andExpect(jsonPath("$.data.loginPhone").value("13900008888"))
                .andReturn();
        String initial = data(approved).path("initialPassword").asText();
        assertThat(initial).isNotBlank();

        MvcResult merchants = mockMvc.perform(get("/api/admin/merchants")
                        .header("Authorization", "Bearer " + loginAdmin("operator")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(body(merchants)).doesNotContain(initial);

        String fresh = loginMerchant("13900008888", initial);
        mockMvc.perform(get("/api/merchant/stores")
                        .header("Authorization", "Bearer " + fresh))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("请先修改初始密码"));

        mockMvc.perform(post("/api/merchant/auth/password")
                        .header("Authorization", "Bearer " + fresh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", initial,
                                "newPassword", "newpass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(false));

        mockMvc.perform(get("/api/merchant/stores")
                        .header("Authorization", "Bearer " + fresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingIntents").exists())
                .andExpect(jsonPath("$.data.pendingMerchantApplications").doesNotExist());
    }

    @Test
    @DisplayName("门户我的意向只含本人；公开配置含站名和主色")
    void portal_me_intents_and_site_config() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13911112222","password":"password","displayName":"另一游客"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));

        String owner = loginTourist("13800000001");
        String other = loginTourist("13911112222");
        long mine = createIntent(owner, 1L, LocalDate.now().plusDays(230));
        long theirs = createIntent(other, 1L, LocalDate.now().plusDays(231));

        MvcResult list = mockMvc.perform(get("/api/me/intents")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andReturn();
        boolean sawMine = false;
        boolean sawTheirs = false;
        for (JsonNode item : data(list).path("items")) {
            if (item.path("id").asLong() == mine) sawMine = true;
            if (item.path("id").asLong() == theirs) sawTheirs = true;
        }
        assertThat(sawMine).isTrue();
        assertThat(sawTheirs).isFalse();

        mockMvc.perform(get("/api/me/intents/" + theirs)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/config/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactPhone").value("13651814168"))
                .andExpect(jsonPath("$.data.siteName").exists())
                .andExpect(jsonPath("$.data.primaryColor").exists());

        String agent = loginAdmin("agent");
        mockMvc.perform(put("/api/admin/site-config")
                        .header("Authorization", "Bearer " + agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"siteName":"不该写成","primaryColor":"#112233","contactPhone":"10000000000"}
                                """))
                .andExpect(status().isForbidden());

        try {
            mockMvc.perform(put("/api/admin/site-config")
                            .header("Authorization", "Bearer " + loginAdmin("operator"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"siteName":"印程测试","primaryColor":"#336699","contactPhone":"13600001111"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.siteName").value("印程测试"));
            mockMvc.perform(get("/api/config/public"))
                    .andExpect(jsonPath("$.data.primaryColor").value("#336699"))
                    .andExpect(jsonPath("$.data.contactPhone").value("13600001111"));
        } finally {
            mockMvc.perform(put("/api/admin/site-config")
                            .header("Authorization", "Bearer " + loginAdmin("operator"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"siteName":"","primaryColor":"","contactPhone":"13651814168"}
                                    """))
                    .andExpect(status().isOk());
        }
    }
}
