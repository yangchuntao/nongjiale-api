package com.suzhong.njl.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.cache.enabled=false")
@AutoConfigureMockMvc
class UnknownRouteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/api/no-such-route", "/api/payments", "/api/checkout", "/api/pay", "/api/wallet"})
    void unknownGetReturnsNotFoundEnvelope(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("接口不存在"))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()));
    }

    @Test
    void paymentPostHasNoControllerAndReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/payments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("接口不存在"))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()));
    }

    @Test
    void healthAndKnownApiStayAvailable() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("nongjiale-api"));

        mockMvc.perform(get("/api/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/regions"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405))
                .andExpect(jsonPath("$.message").value("不支持的请求"));
    }
}
