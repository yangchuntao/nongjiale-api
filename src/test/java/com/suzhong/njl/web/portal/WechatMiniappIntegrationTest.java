package com.suzhong.njl.web.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.PortalUser;
import com.suzhong.njl.domain.entity.PortalWechatIdentity;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;
import com.suzhong.njl.domain.enums.SeedMarket;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.domain.enums.WechatMessage;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.PortalUserMapper;
import com.suzhong.njl.mapper.PortalWechatIdentityMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.StoreMapper;
import com.suzhong.njl.wechat.WechatMiniappClient;
import com.suzhong.njl.wechat.WechatMiniappClient.JsCodeSession;
import com.suzhong.njl.wechat.WechatMiniappClient.PurePhoneNumber;

@SpringBootTest(properties = {
        "app.cache.enabled=false",
        "wechat.miniapp.app-id=wx-test-app",
        "wechat.miniapp.secret=test-secret"
})
@AutoConfigureMockMvc
@Transactional
class WechatMiniappIntegrationTest {

    private static final String APP_ID = "wx-test-app";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private PortalWechatIdentityMapper identityMapper;

    @Autowired
    private BookingIntentMapper bookingIntentMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private WechatMiniappClient wechatMiniappClient;

    @Test
    void loginSucceedsAndReusesTheSameOpenid() throws Exception {
        long before = portalUserMapper.selectCount(null);
        when(wechatMiniappClient.codeToSession("code-login-1")).thenReturn(new JsCodeSession("openid-login", null));
        MvcResult firstResult = performPost("/api/auth/wechat/login", "{\"code\":\"code-login-1\"}", null);
        JsonNode first = read(firstResult);
        assertThat(firstResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(firstResult.getResponse().getContentAsString()).doesNotContain("session_key").doesNotContain("sessionKey");
        assertThat(first.path("data").path("role").asText()).isEqualTo("TOURIST");
        assertThat(first.path("data").path("token").asText()).matches("[0-9a-f]{64}");
        assertThat(first.path("data").path("displayName").asText()).isEqualTo(WechatMessage.SHELL_DISPLAY_NAME);
        assertThat(first.path("data").path("phone").isNull()).isTrue();
        assertThat(first.path("data").path("phoneBound").asBoolean()).isFalse();
        assertThat(first.path("data").path("wechatBound").asBoolean()).isTrue();
        assertThat(first.path("data").path("accountLinked").asBoolean()).isFalse();
        long userId = first.path("data").path("userId").asLong();
        String token = first.path("data").path("token").asText();

        mockMvc.perform(get("/api/auth/wechat/link").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.phone").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.wechatBound").value(true))
                .andExpect(jsonPath("$.data.accountLinked").value(false));

        when(wechatMiniappClient.codeToSession("code-login-2")).thenReturn(new JsCodeSession("openid-login", "union-1"));
        JsonNode second = read(performPost("/api/auth/wechat/login", "{\"code\":\"code-login-2\"}", null));
        assertThat(second.path("data").path("userId").asLong()).isEqualTo(userId);
        assertThat(portalUserMapper.selectCount(null)).isEqualTo(before + 1);
        assertThat(identity("openid-login").getUnionid()).isEqualTo("union-1");

        when(wechatMiniappClient.codeToSession("code-login-3")).thenReturn(new JsCodeSession("openid-login", "union-2"));
        read(performPost("/api/auth/wechat/login", "{\"code\":\"code-login-3\"}", null));
        assertThat(identity("openid-login").getUnionid()).isEqualTo("union-1");
        assertThat(portalUserMapper.selectById(userId).getPhone()).isNull();
        assertThat(portalUserMapper.selectById(userId).getPasswordHash()).isNull();
    }

    @Test
    void invalidCodeAndOutageUseContractMessages() throws Exception {
        when(wechatMiniappClient.codeToSession("used")).thenThrow(WechatMessage.CODE_EXPIRED.toException());
        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"used\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("微信登录已失效，请重试"));

        when(wechatMiniappClient.codeToSession("down")).thenThrow(WechatMessage.UNAVAILABLE.toException());
        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"down\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("微信服务暂不可用"));

        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请提供微信登录凭证"));

        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + "c".repeat(129) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求体格式不正确"));

        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ok\",\"extra\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求体格式不正确"));
    }

    @Test
    void newPhoneRequiresPasswordThenEnablesWebLogin() throws Exception {
        JsonNode shell = login("code-new-phone", "openid-new-phone", null);
        String token = shell.path("data").path("token").asText();
        long userId = shell.path("data").path("userId").asLong();
        when(wechatMiniappClient.phoneNumber("phone-code-missing")).thenReturn(new PurePhoneNumber("13911110001"));

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneCode\":\"phone-code-missing\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请设置网页版登录密码"));
        assertThat(portalUserMapper.selectById(userId).getPhone()).isNull();

        when(wechatMiniappClient.phoneNumber("phone-code-short")).thenReturn(new PurePhoneNumber("13911110002"));
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneCode\":\"phone-code-short\",\"password\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("密码长度为 6 到 64 位"));
        assertThat(portalUserMapper.selectById(userId).getPhone()).isNull();

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110003\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求体格式不正确"));
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110003\",\"password\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("密码长度为 6 到 64 位"));

        JsonNode bound = read(performPost(
                "/api/auth/wechat/bind",
                "{\"phone\":\"13911110003\",\"password\":\"secret12\"}",
                token));
        assertThat(bound.path("code").asInt()).isZero();
        assertThat(bound.path("data").path("userId").asLong()).isEqualTo(userId);
        assertThat(bound.path("data").path("phone").asText()).isEqualTo("13911110003");
        assertThat(bound.path("data").path("accountLinked").asBoolean()).isTrue();
        assertThat(bound.path("data").path("phoneBound").asBoolean()).isTrue();
        PortalUser stored = portalUserMapper.selectById(userId);
        assertThat(stored.getPhone()).isEqualTo("13911110003");
        assertThat(passwordEncoder.matches("secret12", stored.getPasswordHash())).isTrue();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110003\",\"password\":\"secret12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId));

        JsonNode again = read(performPost(
                "/api/auth/wechat/bind",
                "{\"phone\":\"13911110003\",\"password\":\"secret12\"}",
                token));
        assertThat(again.path("code").asInt()).isZero();
        assertThat(again.path("data").path("userId").asLong()).isEqualTo(userId);
        when(wechatMiniappClient.phoneNumber("phone-code-same")).thenReturn(new PurePhoneNumber("13911110003"));
        JsonNode sameByCode = read(performPost(
                "/api/auth/wechat/bind",
                "{\"phoneCode\":\"phone-code-same\"}",
                token));
        assertThat(sameByCode.path("data").path("userId").asLong()).isEqualTo(userId);
        assertThat(sameByCode.path("data").path("accountLinked").asBoolean()).isTrue();

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110003\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("手机号或密码错误"));
        assertThat(portalUserMapper.selectById(userId).getPhone()).isEqualTo("13911110003");

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110009\",\"password\":\"secret12\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("当前微信已绑定其他手机号"));
        assertThat(portalUserMapper.selectById(userId).getPhone()).isEqualTo("13911110003");
    }

    @Test
    void bindingAnExistingPhoneMovesTheShellAndKeepsIntents() throws Exception {
        JsonNode registered = register("13911110011", "secret12", "网页甲");
        long webUserId = registered.path("data").path("userId").asLong();
        String webToken = registered.path("data").path("token").asText();
        long storeId = storeId("表弟店");
        JsonNode existingIntent = read(performPost("/api/booking-intents", intentBody(storeId), webToken));
        String existingNo = existingIntent.path("data").path("intentNo").asText();

        JsonNode shell = login("code-merge", "openid-merge", null);
        long shellId = shell.path("data").path("userId").asLong();
        String shellToken = shell.path("data").path("token").asText();
        mockMvc.perform(get("/api/me/intents").header(HttpHeaders.AUTHORIZATION, "Bearer " + shellToken))
                .andExpect(jsonPath("$.data.items[?(@.intentNo == '" + existingNo + "')]").isEmpty());

        JsonNode bound = read(performPost(
                "/api/auth/wechat/bind",
                "{\"phone\":\"13911110011\",\"password\":\"secret12\"}",
                shellToken));
        assertThat(bound.path("data").path("userId").asLong()).isEqualTo(webUserId);
        assertThat(bound.path("data").path("accountLinked").asBoolean()).isTrue();
        assertThat(bound.path("data").path("displayName").asText()).isEqualTo("网页甲");
        assertThat(portalUserMapper.selectById(shellId)).isNull();
        assertThat(identity("openid-merge").getPortalUserId()).isEqualTo(webUserId);
        assertThat(portalUserMapper.selectById(webUserId).getDisplayName()).isEqualTo("网页甲");

        mockMvc.perform(get("/api/me/intents").header(HttpHeaders.AUTHORIZATION, "Bearer " + shellToken))
                .andExpect(status().isUnauthorized());
        String miniToken = bound.path("data").path("token").asText();
        mockMvc.perform(get("/api/me/intents").header(HttpHeaders.AUTHORIZATION, "Bearer " + miniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.intentNo == '" + existingNo + "')]").isNotEmpty());

        when(wechatMiniappClient.phoneNumber("phone-code-merge")).thenReturn(new PurePhoneNumber("13911110012"));
        JsonNode anotherShell = login("code-phone-merge", "openid-phone-merge", null);
        long anotherShellId = anotherShell.path("data").path("userId").asLong();
        register("13911110012", "secret12", "网页乙");
        JsonNode phoneBound = read(performPost(
                "/api/auth/wechat/bind",
                "{\"phoneCode\":\"phone-code-merge\"}",
                anotherShell.path("data").path("token").asText()));
        assertThat(phoneBound.path("data").path("accountLinked").asBoolean()).isTrue();
        assertThat(portalUserMapper.selectById(anotherShellId)).isNull();
        assertThat(phoneBound.path("data").path("displayName").asText()).isEqualTo("网页乙");
    }

    @Test
    void bindConflictsLeaveBothSidesUnchanged() throws Exception {
        JsonNode occupied = register("13911110021", "secret12", "已绑手机");
        long occupiedId = occupied.path("data").path("userId").asLong();
        insertIdentity(occupiedId, "openid-occupied", APP_ID, null);
        JsonNode shell = login("code-occupied", "openid-newcomer", null);
        long shellId = shell.path("data").path("userId").asLong();
        long usersBefore = portalUserMapper.selectCount(null);
        long identitiesBefore = identityMapper.selectCount(null);

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shell.path("data").path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110021\",\"password\":\"secret12\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("该手机号已绑定其他微信"));
        assertThat(portalUserMapper.selectCount(null)).isEqualTo(usersBefore);
        assertThat(identityMapper.selectCount(null)).isEqualTo(identitiesBefore);
        assertThat(identity("openid-newcomer").getPortalUserId()).isEqualTo(shellId);
        assertThat(identity("openid-occupied").getPortalUserId()).isEqualTo(occupiedId);

        register("13911110022", "secret12", "尚未绑微信");
        JsonNode wrong = login("code-wrong-pass", "openid-wrong-pass", null);
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrong.path("data").path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110022\",\"password\":\"not-the-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("手机号或密码错误"));
        assertThat(portalUserMapper.selectById(wrong.path("data").path("userId").asLong()).getPhone()).isNull();
        assertThat(identity("openid-occupied").getPortalUserId()).isEqualTo(occupiedId);

        register("13911110031", "secret12", "有意向的网页用户");
        JsonNode dirty = login("code-dirty-shell", "openid-dirty-shell", null);
        long dirtyId = dirty.path("data").path("userId").asLong();
        insertIntent(dirtyId, "NJLDIRTYSHELL01");
        long intentsBefore = bookingIntentMapper.selectCount(null);
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dirty.path("data").path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110031\",\"password\":\"secret12\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("未打通账号下已有意向，不能合并"));
        assertThat(bookingIntentMapper.selectCount(null)).isEqualTo(intentsBefore);
        assertThat(portalUserMapper.selectById(dirtyId).getPhone()).isNull();
        assertThat(identity("openid-dirty-shell").getPortalUserId()).isEqualTo(dirtyId);

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shell.path("data").path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110021\",\"phoneCode\":\"both\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求体格式不正确"));
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shell.path("data").path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求体格式不正确"));
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shell.path("data").path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"12345\",\"password\":\"secret12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("手机号格式不正确"));
        when(wechatMiniappClient.phoneNumber("bad-pure")).thenReturn(new PurePhoneNumber("12345"));
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shell.path("data").path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneCode\":\"bad-pure\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("手机号格式不正确"));

        identityMapper.deleteById(identity("openid-newcomer").getId());
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shell.path("data").path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110041\",\"password\":\"secret12\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void unbindDropsWechatTokensAndKeepsPasswordTokens() throws Exception {
        JsonNode registered = register("13911110051", "secret12", "解绑用户");
        long userId = registered.path("data").path("userId").asLong();
        String passwordToken = registered.path("data").path("token").asText();
        long storeId = storeId("表弟店");
        String intentNo = read(performPost("/api/booking-intents", intentBody(storeId), passwordToken))
                .path("data").path("intentNo").asText();
        JsonNode shell = login("code-unbind", "openid-unbind", null);
        JsonNode bound = read(performPost(
                "/api/auth/wechat/bind",
                "{\"phone\":\"13911110051\",\"password\":\"secret12\"}",
                shell.path("data").path("token").asText()));
        String wechatToken = bound.path("data").path("token").asText();

        MvcResult unbound = performPost("/api/auth/wechat/unbind", null, wechatToken);
        assertThat(unbound.getResponse().getStatus()).isEqualTo(200);
        JsonNode link = read(unbound);
        assertThat(link.path("data").has("token")).isFalse();
        assertThat(link.path("data").path("wechatBound").asBoolean()).isFalse();
        assertThat(link.path("data").path("accountLinked").asBoolean()).isFalse();
        assertThat(link.path("data").path("phone").asText()).isEqualTo("13911110051");
        assertThat(identity("openid-unbind")).isNull();
        assertThat(portalUserMapper.selectById(userId).getPhone()).isEqualTo("13911110051");

        mockMvc.perform(get("/api/me/intents").header(HttpHeaders.AUTHORIZATION, "Bearer " + wechatToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
        mockMvc.perform(get("/api/me/intents").header(HttpHeaders.AUTHORIZATION, "Bearer " + passwordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.intentNo == '" + intentNo + "')]").isNotEmpty());

        MvcResult again = performPost("/api/auth/wechat/unbind", null, wechatToken);
        assertThat(again.getResponse().getStatus()).isEqualTo(401);

        JsonNode spare = login("code-unbind-spare", "openid-spare", null);
        identityMapper.deleteById(identity("openid-spare").getId());
        MvcResult idempotent = performPost("/api/auth/wechat/unbind", null, spare.path("data").path("token").asText());
        assertThat(idempotent.getResponse().getStatus()).isEqualTo(200);
        assertThat(read(idempotent).path("data").path("wechatBound").asBoolean()).isFalse();
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + spare.path("data").path("token").asText()))
                .andExpect(status().isUnauthorized());

        JsonNode relogin = login("code-unbind-2", "openid-unbind", null);
        assertThat(relogin.path("data").path("userId").asLong()).isNotEqualTo(userId);
        assertThat(relogin.path("data").path("accountLinked").asBoolean()).isFalse();
        mockMvc.perform(get("/api/me/intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + relogin.path("data").path("token").asText()))
                .andExpect(jsonPath("$.data.items[?(@.intentNo == '" + intentNo + "')]").isEmpty());
    }

    @Test
    void anotherAppIdCanBindTheSamePhone() throws Exception {
        JsonNode registered = register("13911110061", "secret12", "双应用");
        long userId = registered.path("data").path("userId").asLong();
        insertIdentity(userId, "openid-old-app", "wx-old-app", null);
        JsonNode shell = login("code-new-app", "openid-new-app", null);
        long shellId = shell.path("data").path("userId").asLong();

        JsonNode bound = read(performPost(
                "/api/auth/wechat/bind",
                "{\"phone\":\"13911110061\",\"password\":\"secret12\"}",
                shell.path("data").path("token").asText()));
        assertThat(bound.path("data").path("userId").asLong()).isEqualTo(userId);
        assertThat(portalUserMapper.selectById(shellId)).isNull();
        assertThat(identityFor("wx-old-app", "openid-old-app").getPortalUserId()).isEqualTo(userId);
        assertThat(identityFor(APP_ID, "openid-new-app").getPortalUserId()).isEqualTo(userId);
    }

    @Test
    void shellUserCannotCreateIntent() throws Exception {
        JsonNode shell = login("code-no-phone", "openid-no-phone", null);
        long before = bookingIntentMapper.selectCount(null);
        mockMvc.perform(post("/api/booking-intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shell.path("data").path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intentBody(storeId("表弟店"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("请先绑定手机号，完成与网页版账号打通后再提交意向"));
        assertThat(bookingIntentMapper.selectCount(null)).isEqualTo(before);

        PortalUser emptyPassword = new PortalUser();
        emptyPassword.setPhone("13911110071");
        emptyPassword.setDisplayName("空密码");
        portalUserMapper.insert(emptyPassword);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110071\",\"password\":\"secret12\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("手机号或密码错误"));
    }

    @Test
    void cmp2bBothDirectionsAndAnUnlinkedAccountStaySeparate() throws Exception {
        JsonNode registered = register("13911110081", "secret12", "打通甲");
        long userId = registered.path("data").path("userId").asLong();
        String webToken = registered.path("data").path("token").asText();
        long storeId = storeId("表弟店");
        String webIntentNo = read(performPost("/api/booking-intents", intentBody(storeId), webToken))
                .path("data").path("intentNo").asText();

        JsonNode other = register("13911110082", "secret12", "未打通乙");
        String otherIntentNo = read(performPost("/api/booking-intents", intentBody(storeId), other.path("data").path("token").asText()))
                .path("data").path("intentNo").asText();

        JsonNode shell = login("code-cmp", "openid-cmp", null);
        assertThat(shell.path("data").path("accountLinked").asBoolean()).isFalse();
        assertThat(shell.path("data").path("userId").asLong()).isNotEqualTo(userId);
        mockMvc.perform(get("/api/me/intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shell.path("data").path("token").asText()))
                .andExpect(jsonPath("$.data.items[?(@.intentNo == '" + webIntentNo + "')]").isEmpty())
                .andExpect(jsonPath("$.data.items[?(@.intentNo == '" + otherIntentNo + "')]").isEmpty());

        JsonNode bound = read(performPost(
                "/api/auth/wechat/bind",
                "{\"phone\":\"13911110081\",\"password\":\"secret12\"}",
                shell.path("data").path("token").asText()));
        assertThat(bound.path("data").path("userId").asLong()).isEqualTo(userId);
        assertThat(bound.path("data").path("accountLinked").asBoolean()).isTrue();
        String miniToken = bound.path("data").path("token").asText();
        String miniIntentNo = read(performPost("/api/booking-intents", intentBody(storeId), miniToken))
                .path("data").path("intentNo").asText();

        JsonNode webAgain = read(performPost(
                "/api/auth/login",
                "{\"phone\":\"13911110081\",\"password\":\"secret12\"}",
                null));
        String freshWebToken = webAgain.path("data").path("token").asText();
        mockMvc.perform(get("/api/me/intents").header(HttpHeaders.AUTHORIZATION, "Bearer " + freshWebToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.intentNo == '" + miniIntentNo + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data.items[?(@.intentNo == '" + otherIntentNo + "')]").isEmpty());

        String secondNo = read(performPost("/api/booking-intents", intentBody(storeId, "2026-11-23", "2026-11-24"), freshWebToken))
                .path("data").path("intentNo").asText();
        mockMvc.perform(get("/api/me/intents").header(HttpHeaders.AUTHORIZATION, "Bearer " + miniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.intentNo == '" + secondNo + "')].status").value(Matchers.contains("PENDING")))
                .andExpect(jsonPath("$.data.items[?(@.intentNo == '" + otherIntentNo + "')]").isEmpty());
        assertThat(IntentStatus.CHECKED_IN.label()).isEqualTo("已入住");
        assertThat(IntentStatus.CHECKED_IN.code()).isEqualTo("CHECKED_IN");
    }

    @Test
    void keywordIsOptionalAndCannotRevealClosedCities() throws Exception {
        MvcResult omitted = mockMvc.perform(get("/api/stores")).andReturn();
        MvcResult blank = mockMvc.perform(get("/api/stores").param("keyword", "   ")).andReturn();
        assertThat(names(read(omitted))).isEqualTo(names(read(blank)));
        assertThat(read(omitted).path("data").path("total").asLong()).isEqualTo(read(blank).path("data").path("total").asLong());

        mockMvc.perform(get("/api/stores").param("keyword", "表弟"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("表弟店"));

        mockMvc.perform(get("/api/stores").param("keyword", "a".repeat(65)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("参数不正确"));
        mockMvc.perform(get("/api/stores").param("keyword", "a".repeat(64)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        Store cousin = storeMapper.selectOne(Wrappers.<Store>lambdaQuery().eq(Store::getName, "表弟店"));
        Store wildcard = new Store();
        wildcard.setMerchantOrgId(cousin.getMerchantOrgId());
        wildcard.setRegionId(cousin.getRegionId());
        wildcard.setName("100%_特价院");
        wildcard.setShelfStatus(ShelfStatus.ON_SHELF);
        wildcard.setHomeRecommended(false);
        wildcard.setHomeSort(70);
        wildcard.setTestData(false);
        wildcard.setVersion(0L);
        wildcard.setPackageText("通配");
        storeMapper.insert(wildcard);

        mockMvc.perform(get("/api/stores").param("keyword", "%"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("100%_特价院"));
        mockMvc.perform(get("/api/stores").param("keyword", "_"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("100%_特价院"));
        mockMvc.perform(get("/api/stores").param("keyword", "表弟"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("表弟店"));

        Region province = regionMapper.selectOne(Wrappers.<Region>lambdaQuery().eq(Region::getSlug, "jiangsu"));
        Region closedCity = insertRegion("closed-city", "闭城市", RegionLevel.CITY, province.getId(), RegionOpenStatus.CLOSED);
        Region closedDistrict = insertRegion("closed-district", "闭城区", RegionLevel.DISTRICT, closedCity.getId(), null);
        Store hidden = new Store();
        hidden.setMerchantOrgId(cousin.getMerchantOrgId());
        hidden.setRegionId(closedDistrict.getId());
        hidden.setName("闭城关键词店");
        hidden.setShelfStatus(ShelfStatus.ON_SHELF);
        hidden.setHomeRecommended(false);
        hidden.setHomeSort(71);
        hidden.setTestData(false);
        hidden.setVersion(0L);
        hidden.setPackageText("闭城");
        storeMapper.insert(hidden);

        mockMvc.perform(get("/api/stores")
                        .param("regionId", String.valueOf(closedCity.getId()))
                        .param("keyword", "闭城关键词"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.destinationStatus").value("CLOSED"))
                .andExpect(jsonPath("$.data.emptyHint").value("该目的地暂未开通"));
        mockMvc.perform(get("/api/stores").param("keyword", "闭城关键词"))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void merchantAndWebTokensFollowTheAuthRules() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("接口不存在"));

        JsonNode merchant = read(performPost(
                "/api/merchant/auth/login",
                "{\"phone\":\"17606838921\",\"password\":\"123456\"}",
                null));
        String merchantToken = merchant.path("data").path("token").asText();
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110091\",\"password\":\"secret12\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权限"));

        JsonNode admin = read(performPost(
                "/api/admin/auth/login",
                "{\"phone\":\"17606838920\",\"password\":\"123456\"}",
                null));
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.path("data").path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110091\",\"password\":\"secret12\"}"))
                .andExpect(status().isForbidden());

        JsonNode web = register("13911110092", "secret12", "仅网页");
        String webToken = web.path("data").path("token").asText();
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + webToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13911110092\",\"password\":\"secret12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请先使用微信登录"));
        mockMvc.perform(post("/api/auth/wechat/unbind").header(HttpHeaders.AUTHORIZATION, "Bearer " + webToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请先使用微信登录"));
        mockMvc.perform(get("/api/auth/wechat/link").header(HttpHeaders.AUTHORIZATION, "Bearer " + webToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("13911110092"))
                .andExpect(jsonPath("$.data.wechatBound").value(false))
                .andExpect(jsonPath("$.data.accountLinked").value(false));
        mockMvc.perform(get("/api/auth/wechat/link"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    private JsonNode login(String code, String openid, String unionid) throws Exception {
        when(wechatMiniappClient.codeToSession(code)).thenReturn(new JsCodeSession(openid, unionid));
        MvcResult result = performPost("/api/auth/wechat/login", "{\"code\":\"" + code + "\"}", null);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return read(result);
    }

    private JsonNode register(String phone, String password, String displayName) throws Exception {
        MvcResult result = performPost("/api/auth/register", """
                {"phone":"%s","password":"%s","displayName":"%s"}
                """.formatted(phone, password, displayName), null);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return read(result);
    }

    private MvcResult performPost(String path, String json, String token) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON);
        if (json != null) {
            request.content(json);
        }
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request).andReturn();
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private List<String> names(JsonNode body) {
        List<String> names = new ArrayList<>();
        for (JsonNode item : body.path("data").path("items")) {
            names.add(item.path("name").asText());
        }
        return names;
    }

    private PortalWechatIdentity identity(String openid) {
        return identityFor(APP_ID, openid);
    }

    private PortalWechatIdentity identityFor(String appId, String openid) {
        return identityMapper.selectOne(Wrappers.<PortalWechatIdentity>lambdaQuery()
                .eq(PortalWechatIdentity::getAppId, appId)
                .eq(PortalWechatIdentity::getOpenid, openid));
    }

    private void insertIdentity(Long userId, String openid, String appId, String unionid) {
        PortalWechatIdentity row = new PortalWechatIdentity();
        row.setPortalUserId(userId);
        row.setAppId(appId);
        row.setOpenid(openid);
        row.setUnionid(unionid);
        identityMapper.insert(row);
    }

    private PortalUser findByPhone(String phone) {
        return portalUserMapper.selectOne(Wrappers.<PortalUser>lambdaQuery().eq(PortalUser::getPhone, phone));
    }

    private void insertIntent(Long userId, String intentNo) {
        Store store = storeMapper.selectOne(Wrappers.<Store>lambdaQuery().eq(Store::getName, "表弟店"));
        BookingIntent intent = new BookingIntent();
        intent.setIntentNo(intentNo);
        intent.setUserId(userId);
        intent.setStoreId(store.getId());
        intent.setMerchantOrgId(store.getMerchantOrgId());
        intent.setCheckIn(LocalDate.of(2026, 11, 20));
        intent.setGuests(2);
        intent.setContactName("甲");
        intent.setContactPhone("13900003333");
        intent.setStatus(IntentStatus.PENDING);
        intent.setVersion(0L);
        bookingIntentMapper.insert(intent);
    }

    private long storeId(String name) {
        return storeMapper.selectOne(Wrappers.<Store>lambdaQuery().eq(Store::getName, name)).getId();
    }

    private String intentBody(long storeId) {
        return intentBody(storeId, "2026-11-20", "2026-11-22");
    }

    private String intentBody(long storeId, String checkIn, String checkOut) {
        return """
                {"storeId":%d,"checkIn":"%s","checkOut":"%s","guests":2,"contactName":"甲","contactPhone":"13900003333"}
                """.formatted(storeId, checkIn, checkOut);
    }

    private Region insertRegion(String slug, String name, RegionLevel level, Long parentId, RegionOpenStatus status) {
        Region region = new Region();
        region.setParentId(parentId);
        region.setSlug(slug);
        region.setName(name);
        region.setLevel(level);
        region.setSortOrder(80);
        region.setEnabled(true);
        region.setSeedMarket(SeedMarket.SUZHONG);
        region.setOpenStatus(status);
        regionMapper.insert(region);
        return region;
    }
}
