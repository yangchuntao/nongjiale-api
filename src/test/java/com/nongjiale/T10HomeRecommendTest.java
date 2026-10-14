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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B6：首页推荐写在房源字段上。运营可改，客服 403，商家忽略。
 */
@SpringBootTest
@AutoConfigureMockMvc
class T10HomeRecommendTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
    }

    private String loginAdmin(String username) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", "password"))))
                .andExpect(status().isOk())
                .andReturn();
        return data(r).path("token").asText();
    }

    private String loginMerchant() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", "13800001111", "password", "password"))))
                .andExpect(status().isOk())
                .andReturn();
        return data(r).path("token").asText();
    }

    private List<String> recommendedNames() throws Exception {
        MvcResult feed = mockMvc.perform(get("/api/home/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedHomestays").isArray())
                .andReturn();
        List<String> names = new ArrayList<>();
        for (JsonNode item : data(feed).path("recommendedHomestays")) {
            names.add(item.path("name").asText());
        }
        return names;
    }

    @Test
    @DisplayName("运营设置推荐后首页可见；客服 403；商家不能改推荐字段；未上架不进楼层")
    void operator_sets_recommend_agent_and_merchant_cannot() throws Exception {
        List<String> seeded = recommendedNames();
        assertThat(seeded).containsExactly("长兴鸿玖农家乐", "宜兴翠竹昀民宿");

        String operator = loginAdmin("operator");
        mockMvc.perform(patch("/api/admin/homestays/2")
                        .header("Authorization", "Bearer " + operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeRecommendSort":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.homeRecommended").value(true))
                .andExpect(jsonPath("$.data.homeRecommendSort").value(0));

        assertThat(recommendedNames()).containsExactly("宜兴翠竹昀民宿", "长兴鸿玖农家乐");

        mockMvc.perform(patch("/api/admin/homestays/3")
                        .header("Authorization", "Bearer " + operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeRecommended":true,"homeRecommendSort":0}
                                """))
                .andExpect(status().isOk());
        assertThat(recommendedNames()).doesNotContain("下架示例农家乐");

        String agent = loginAdmin("agent");
        mockMvc.perform(put("/api/admin/homestays/1")
                        .header("Authorization", "Bearer " + agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeRecommended":false,"homeRecommendSort":99}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限"));
        assertThat(recommendedNames()).contains("长兴鸿玖农家乐");

        String merchant = loginMerchant();
        MvcResult store = mockMvc.perform(get("/api/merchant/stores/1")
                        .header("Authorization", "Bearer " + merchant))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(data(store).has("homeRecommended")).isFalse();
        long version = data(store).path("version").asLong();

        mockMvc.perform(put("/api/merchant/stores/1")
                        .header("Authorization", "Bearer " + merchant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "长兴鸿玖农家乐",
                                "homeRecommended", false,
                                "homeRecommendSort", 99,
                                "version", version))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.homeRecommended").doesNotExist());

        MvcResult adminList = mockMvc.perform(get("/api/admin/homestays")
                        .header("Authorization", "Bearer " + operator))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode stay = null;
        for (JsonNode item : data(adminList).path("items")) {
            if (item.path("id").asLong() == 1L) stay = item;
        }
        assertThat(stay).isNotNull();
        assertThat(stay.path("homeRecommended").asBoolean()).isTrue();
        assertThat(stay.path("homeRecommendSort").asInt()).isEqualTo(1);
    }
}
