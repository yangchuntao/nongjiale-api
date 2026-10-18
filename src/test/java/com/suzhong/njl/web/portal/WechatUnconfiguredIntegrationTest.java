package com.suzhong.njl.web.portal;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.wechat.WechatMiniappClient;

@SpringBootTest(properties = {
        "app.cache.enabled=false",
        "wechat.miniapp.app-id=",
        "wechat.miniapp.secret=   "
})
@AutoConfigureMockMvc
@Transactional
class WechatUnconfiguredIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WechatMiniappClient wechatMiniappClient;

    @Test
    void missingCredentialsFailWechatOnlyAndDoNotCallOut() throws Exception {
        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"any-code\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("微信登录未配置"));

        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110111\",\"password\":\"secret12\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(registered.getResponse().getContentAsString())
                .path("data").path("token").asText();

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110111\",\"password\":\"secret12\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("微信登录未配置"));
        mockMvc.perform(post("/api/auth/wechat/unbind").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("微信登录未配置"));
        mockMvc.perform(get("/api/auth/wechat/link").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("微信登录未配置"));

        mockMvc.perform(get("/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/stores")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/home/banners")).andExpect(status().isOk());
        verifyNoInteractions(wechatMiniappClient);
    }
}
