package com.nongjiale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nongjiale.domain.ContactMessage;
import com.nongjiale.repository.ContactMessageRepository;
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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T8 咨询留言 API：登录提交成功 / 无 Token 401 不入库 / 空内容校验失败。
 * MockMvc，不占用 8080。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class T8ContactMessageApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ContactMessageRepository messageRepository;

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

    private String contactBody(Long homestayId, String content, String contactName, String contactPhone)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        if (homestayId != null) {
            body.put("homestayId", homestayId);
        }
        body.put("content", content);
        body.put("contactName", contactName);
        body.put("contactPhone", contactPhone);
        return objectMapper.writeValueAsString(body);
    }

    @Test
    @Order(1)
    @DisplayName("T8 鉴权: 无 Token 调 POST /api/contact-messages → 401，不入库")
    void t8_unauthenticated_post_returns_401() throws Exception {
        long before = messageRepository.count();

        mockMvc.perform(post("/api/contact-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactBody(1L, "想咨询周末空房", "演示游客", "13800000001")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        assertThat(messageRepository.count()).isEqualTo(before);
    }

    @Test
    @Order(2)
    @DisplayName("T8b: 已登录合法留言 → 成功；库中有 PENDING 待跟进留言")
    void t8b_authenticated_create_succeeds_with_pending() throws Exception {
        String token = loginTourist();
        long before = messageRepository.count();
        String content = "你好，想咨询下周双人房是否有空";

        MvcResult r = mockMvc.perform(post("/api/contact-messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactBody(1L, content, "演示游客", "13800000001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.messageId").isNumber())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.contactPhone").isNotEmpty())
                .andReturn();

        assertThat(messageRepository.count()).isEqualTo(before + 1);

        JsonNode data = objectMapper.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data");
        long messageId = data.path("messageId").asLong();
        assertThat(messageId).isPositive();

        ContactMessage saved = messageRepository.findById(messageId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getContent()).isEqualTo(content);
        assertThat(saved.getContactName()).isEqualTo("演示游客");
        assertThat(saved.getContactPhone()).isEqualTo("13800000001");
        assertThat(saved.getHomestayId()).isEqualTo(1L);
        assertThat(saved.getUserId()).isPositive();
    }

    @Test
    @Order(3)
    @DisplayName("T8 校验: 空 content / 空 contactName → 400，不入库")
    void t8_validation_rejects_blank_fields() throws Exception {
        String token = loginTourist();
        long before = messageRepository.count();

        mockMvc.perform(post("/api/contact-messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactBody(1L, "", "演示游客", "13800000001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/contact-messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactBody(1L, "有内容但无名", "", "13800000001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/contact-messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactBody(1L, "有内容", "演示游客", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(messageRepository.count()).isEqualTo(before);
    }
}
