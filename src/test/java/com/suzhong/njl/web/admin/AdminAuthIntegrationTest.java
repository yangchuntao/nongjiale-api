package com.suzhong.njl.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.cache.CacheName;
import com.suzhong.njl.config.AppProperties;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.SeedMarket;
import com.suzhong.njl.domain.enums.SiteConfigKey;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.SiteConfigMapper;
import com.suzhong.njl.security.InMemoryTokenStore;
import com.suzhong.njl.security.TokenStore;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAuthIntegrationTest {

    private static final String PHONE = "13800000000";
    private static final String PASSWORD = "SeedAdmin#2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private SiteConfigMapper siteConfigMapper;

    @Test
    void healthStaysPublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("nongjiale-api"));
    }

    @Test
    void adminLogsInWithPhoneAndReachesProtectedRoute() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","password":"%s"}
                                """.formatted(PHONE, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.role").value("OPERATOR"))
                .andExpect(jsonPath("$.data.displayName").value("苏中运营"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String body = login.getResponse().getContentAsString();
        String token = body.replaceAll("(?s).*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/admin/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.phone").value(PHONE))
                .andExpect(jsonPath("$.data.role").value("OPERATOR"));
    }

    @Test
    void wrongPasswordAndMissingTokenUseTheEnvelope() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","password":"wrong-password"}
                                """.formatted(PHONE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("手机号或密码错误"))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()));

        mockMvc.perform(get("/api/admin/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void usernameIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"operator","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void corsAllowsTheThreeLocalFrontendsOnly() throws Exception {
        for (String origin : new String[] {
                "http://localhost:43123",
                "http://localhost:43124",
                "http://localhost:43125"}) {
            mockMvc.perform(options("/api/admin/auth/login")
                            .header(HttpHeaders.ORIGIN, origin)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
        }

        mockMvc.perform(options("/api/admin/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void flywaySeedAndCacheStayOffRedisTokens() {
        assertThat(tokenStore).isInstanceOf(InMemoryTokenStore.class);
        assertThat(appProperties.getCache().getKeyPrefix()).isEqualTo("njl:");
        assertThat(appProperties.getCorsAllowedOrigins()).containsExactly(
                "http://localhost:43123",
                "http://localhost:43124",
                "http://localhost:43125");
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        assertThat(cacheManager.getCache(CacheName.SITE_CONFIG.key())).isNotNull();

        assertThat(regionMapper.selectCount(Wrappers.lambdaQuery(com.suzhong.njl.domain.entity.Region.class)
                .eq(com.suzhong.njl.domain.entity.Region::getSeedMarket, SeedMarket.SUZHONG))).isEqualTo(7);
        assertThat(regionMapper.selectOne(Wrappers.lambdaQuery(com.suzhong.njl.domain.entity.Region.class)
                .eq(com.suzhong.njl.domain.entity.Region::getSlug, "yangzhou")).getLevel()).isEqualTo(RegionLevel.CITY);
        assertThat(siteConfigMapper.selectById(SiteConfigKey.NAME.code()).getConfigValue()).isEqualTo("站点名称");
        assertThat(siteConfigMapper.selectById(SiteConfigKey.BRAND_COLOR.code()).getConfigValue()).isEqualTo("#4BA3D9");
        assertThat(siteConfigMapper.selectById(SiteConfigKey.CONTACT_PHONE.code()).getConfigValue()).isEqualTo("400-000-0000");
    }
}
