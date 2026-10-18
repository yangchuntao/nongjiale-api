package com.suzhong.njl.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.domain.entity.AdminUser;
import com.suzhong.njl.domain.entity.AuditLog;
import com.suzhong.njl.domain.entity.HomeBanner;
import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.AdminRole;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;
import com.suzhong.njl.mapper.AdminUserMapper;
import com.suzhong.njl.mapper.AuditLogMapper;
import com.suzhong.njl.mapper.HomeBannerMapper;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.mapper.RegionMapper;

@SpringBootTest(properties = "app.cache.enabled=false")
@AutoConfigureMockMvc
@Transactional
class BannerApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private HomeBannerMapper homeBannerMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private MerchantOrgMapper merchantOrgMapper;

    @Autowired
    private MerchantUserMapper merchantUserMapper;

    @Test
    void seedShowsAllAndOpenCityAndIgnoresTheVisitorCity() throws Exception {
        assertThat(IntentStatus.CHECKED_IN.label()).isEqualTo("已入住");
        assertThat(region(RegionLevel.CITY, "nantong").getOpenStatus()).isEqualTo(RegionOpenStatus.OPEN);
        assertThat(region(RegionLevel.CITY, "yangzhou").getOpenStatus()).isEqualTo(RegionOpenStatus.OPEN);
        assertThat(region(RegionLevel.CITY, "taizhou").getOpenStatus()).isEqualTo(RegionOpenStatus.OPEN);

        JsonNode banners = ok(getJson("/api/home/banners", null));
        JsonNode withCity = ok(getJson("/api/home/banners?cityId=1&slug=nantong&regionId=2", null));
        assertThat(titles(withCity)).isEqualTo(titles(banners));
        assertThat(titles(banners)).containsExactly("苏中逛一逛", "扬州周末");
        assertThat(banners.get(0).path("scope").asText()).isEqualTo("ALL");
        assertThat(banners.get(0).path("cityId").isNull()).isTrue();
        assertThat(banners.get(0).path("citySlug").isNull()).isTrue();
        assertThat(banners.get(0).path("imageUrl").asText()).isEqualTo("https://example.com/njl/banner-all.png");
        assertThat(banners.get(0).path("linkUrl").asText()).isEqualTo("/");
        assertThat(banners.get(0).path("enabled").asBoolean()).isTrue();
        assertThat(banners.get(0).path("sortOrder").asInt()).isEqualTo(10);
        assertThat(banners.get(1).path("scope").asText()).isEqualTo("CITY");
        assertThat(banners.get(1).path("citySlug").asText()).isEqualTo("yangzhou");
        assertThat(banners.get(1).path("cityName").asText()).isEqualTo("扬州市");
        assertThat(banners.get(1).path("linkUrl").asText()).isEqualTo("/?city=yangzhou");
        assertThat(banners.get(1).path("imageUrl").asText()).isEqualTo("https://example.com/njl/banner-yangzhou.png");
        assertThat(banners.get(1).has("cityOpen")).isFalse();

        String feed = exchange(get("/api/home/feed")).raw();
        assertThat(feed).doesNotContain("苏中逛一逛");
        assertThat(exchange(get("/api/payments")).status()).isEqualTo(404);
    }

    @Test
    void operatorAndAgentCanManageBannersWhileOthersCannot() throws Exception {
        exchange(get("/api/admin/banners")).satisfies(result -> {
            assertThat(result.status()).isEqualTo(401);
            assertThat(result.body().path("code").asInt()).isEqualTo(401);
        });

        String tourist = registerTourist("13900008101");
        exchange(get("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, bearer(tourist))).satisfies(result -> {
            assertThat(result.status()).isEqualTo(403);
            assertThat(result.body().path("code").asInt()).isEqualTo(403);
        });

        insertMerchant("13700008101");
        String merchant = login("/api/merchant/auth/login", "13700008101", "merchant12");
        exchange(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, bearer(merchant))
                .contentType(MediaType.APPLICATION_JSON)
                .content(allBody(null, null))).satisfies(result -> assertThat(result.status()).isEqualTo(403));

        insertAgent();
        String agent = login("/api/admin/auth/login", "13800000081", "SeedAdmin#2026");
        JsonNode agentList = ok(getJson("/api/admin/banners", agent));
        assertThat(titles(agentList.path("items"))).contains("苏中逛一逛", "扬州周末");

        String admin = adminToken();
        JsonNode created = ok(postJson("/api/admin/banners", allBody(null, null), admin));
        assertThat(created.path("sortOrder").asInt()).isEqualTo(100);
        assertThat(created.path("enabled").asBoolean()).isTrue();
        assertThat(created.path("title").isNull()).isTrue();
        assertThat(created.path("scope").asText()).isEqualTo("ALL");
        long createdId = created.path("id").asLong();
        assertThat(titles(ok(getJson("/api/home/banners", null)))).containsExactly("苏中逛一逛", "扬州周末", null);

        JsonNode ahead = ok(postJson("/api/admin/banners", allBody(5, "排头"), admin));
        JsonNode tieA = ok(postJson("/api/admin/banners", allBody(15, "同序甲"), admin));
        JsonNode tieB = ok(postJson("/api/admin/banners", allBody(15, "同序乙"), admin));
        assertThat(tieA.path("id").asLong()).isLessThan(tieB.path("id").asLong());
        assertThat(titles(ok(getJson("/api/home/banners", null))))
                .startsWith("排头", "苏中逛一逛", "同序甲", "同序乙", "扬州周末");

        long aheadId = ahead.path("id").asLong();
        ok(postJson("/api/admin/banners/" + aheadId + "/disable", "", admin));
        assertThat(auditCount(AuditAction.BANNER_DISABLE, aheadId)).isEqualTo(1);
        assertThat(titles(ok(getJson("/api/home/banners", null)))).doesNotContain("排头");
        ok(postJson("/api/admin/banners/" + aheadId + "/disable", "", admin));
        assertThat(auditCount(AuditAction.BANNER_DISABLE, aheadId)).isEqualTo(1);
        ok(postJson("/api/admin/banners/" + aheadId + "/enable", "", admin));
        assertThat(auditCount(AuditAction.BANNER_ENABLE, aheadId)).isEqualTo(1);
        ok(postJson("/api/admin/banners/" + aheadId + "/enable", "", admin));
        assertThat(auditCount(AuditAction.BANNER_ENABLE, aheadId)).isEqualTo(1);
        assertThat(titles(ok(getJson("/api/home/banners", null)))).contains("排头");

        JsonNode removed = ok(deleteJson("/api/admin/banners/" + createdId, admin));
        assertThat(removed.path("id").asLong()).isEqualTo(createdId);
        assertThat(auditCount(AuditAction.BANNER_DELETE, createdId)).isEqualTo(1);
        assertThat(homeBannerMapper.selectById(createdId)).isNull();

        AuditLog createdLog = auditLogMapper.selectOne(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, AuditAction.BANNER_CREATE)
                .eq(AuditLog::getTargetId, aheadId));
        assertThat(createdLog.getTargetType()).isEqualTo(AuditTarget.HOME_BANNER.code());
        assertThat(createdLog.getDetailJson()).contains("ALL").doesNotContain("click");
    }

    @Test
    void closedCityDropsFromThePortalAndCannotBeEnabledAgain() throws Exception {
        String admin = adminToken();
        long nantongId = region(RegionLevel.CITY, "nantong").getId();
        long yangzhouId = region(RegionLevel.CITY, "yangzhou").getId();
        JsonNode drafted = ok(postJson("/api/admin/banners", cityBody(nantongId, false, "南通草稿"), admin));
        long draftedId = drafted.path("id").asLong();
        assertThat(drafted.path("cityOpen").asBoolean()).isTrue();
        assertThat(drafted.path("citySlug").asText()).isEqualTo("nantong");

        closeCity(nantongId, admin);
        assertThat(titles(ok(getJson("/api/home/banners", null)))).contains("扬州周末").doesNotContain("南通草稿");

        HttpResult rejected = exchange(post("/api/admin/banners")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cityBody(nantongId, false, "南通再投")));
        assertThat(rejected.status()).isEqualTo(400);
        assertThat(rejected.body().path("message").asText()).isEqualTo("投放市未开通，请改选全站或其他已开通市");

        HttpResult enable = exchange(post("/api/admin/banners/" + draftedId + "/enable")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin)));
        assertThat(enable.status()).isEqualTo(409);
        assertThat(enable.body().path("message").asText()).isEqualTo("投放市未开通，请改选全站或其他已开通市");
        assertThat(enable.body().path("code").asInt()).isEqualTo(409);
        assertThat(homeBannerMapper.selectById(draftedId).getEnabled()).isFalse();
        assertThat(auditCount(AuditAction.BANNER_ENABLE, draftedId)).isZero();

        HttpResult stillClosed = exchange(put("/api/admin/banners/" + draftedId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cityBody(nantongId, false, "南通草稿")));
        assertThat(stillClosed.status()).isEqualTo(400);
        assertThat(stillClosed.body().path("message").asText()).isEqualTo("投放市未开通，请改选全站或其他已开通市");

        JsonNode moved = ok(putJson("/api/admin/banners/" + draftedId, """
                {"title":"改成全站","imageUrl":"https://img.example/moved.png","linkUrl":"/stores","scope":"ALL","sortOrder":8,"enabled":true}
                """, admin));
        assertThat(moved.path("cityId").isNull()).isTrue();
        assertThat(moved.path("cityOpen").isNull()).isTrue();
        assertThat(homeBannerMapper.selectById(draftedId).getCityId()).isNull();
        assertThat(titles(ok(getJson("/api/home/banners", null)))).startsWith("改成全站");
        assertThat(auditCount(AuditAction.BANNER_UPDATE, draftedId)).isEqualTo(1);

        long yangzhouBannerId = homeBannerMapper.selectOne(Wrappers.<HomeBanner>lambdaQuery()
                .eq(HomeBanner::getTitle, "扬州周末")).getId();
        closeCity(yangzhouId, admin);
        assertThat(titles(ok(getJson("/api/home/banners", null)))).doesNotContain("扬州周末").contains("苏中逛一逛", "改成全站");
        JsonNode hidden = ok(getJson("/api/admin/banners/" + yangzhouBannerId, admin));
        assertThat(hidden.path("enabled").asBoolean()).isTrue();
        assertThat(hidden.path("cityOpen").asBoolean()).isFalse();
        assertThat(homeBannerMapper.selectById(yangzhouBannerId).getEnabled()).isTrue();

        JsonNode filtered = ok(getJson("/api/admin/banners?enabled=true&scope=CITY&keyword=周末", admin));
        assertThat(titles(filtered.path("items"))).containsExactly("扬州周末");
    }

    @Test
    void rejectsBadBannerInput() throws Exception {
        String admin = adminToken();
        long districtId = region(RegionLevel.DISTRICT, "guangling").getId();
        expect(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageUrl":" ","linkUrl":"/","scope":"ALL"}
                        """), 400, "请填写图片地址");
        expect(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageUrl":"https://img.example/a.png","linkUrl":"//evil","scope":"ALL"}
                        """), 400, "跳转链接须为站内路径或 http(s) 外链");
        expect(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageUrl":"https://img.example/a.png","linkUrl":"/","scope":"ALL","cityCode":"yangzhou"}
                        """), 400, "请求体格式不正确");
        expect(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageUrl":"https://img.example/a.png","linkUrl":"/","scope":"WORLD"}
                        """), 400, "请求体格式不正确");
        expect(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageUrl":"https://img.example/a.png","linkUrl":"/","scope":"ALL","sortOrder":1.5}
                        """), 400, "请求体格式不正确");
        expect(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageUrl":"https://img.example/a.png","linkUrl":"/","scope":"CITY","cityId":%d,"enabled":false}
                        """.formatted(districtId)), 404, "城市不存在");
        expect(get("/api/admin/banners/abc").header(HttpHeaders.AUTHORIZATION, bearer(admin)), 400, "参数不正确");
        expect(get("/api/admin/banners?scope=NOPE").header(HttpHeaders.AUTHORIZATION, bearer(admin)), 400, "参数不正确");
        expect(get("/api/admin/banners?page=0").header(HttpHeaders.AUTHORIZATION, bearer(admin)), 400, "分页参数不正确");
        expect(get("/api/admin/banners?keyword=" + "k".repeat(65)).header(HttpHeaders.AUTHORIZATION, bearer(admin)), 400, "参数不正确");
        expect(get("/api/admin/banners/999999").header(HttpHeaders.AUTHORIZATION, bearer(admin)), 404, "Banner 不存在");
        expect(put("/api/admin/banners/" + seedId("苏中逛一逛"))
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageUrl":"https://img.example/a.png","linkUrl":"/","scope":"ALL","sortOrder":10}
                        """), 400, "请指定是否启用");

        Region disabled = region(RegionLevel.CITY, "yangzhou");
        disabled.setEnabled(false);
        regionMapper.updateById(disabled);
        expect(post("/api/admin/banners").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cityBody(disabled.getId(), true, "停用市")), 400, "地区不可用");
    }

    private long seedId(String title) {
        return homeBannerMapper.selectOne(Wrappers.<HomeBanner>lambdaQuery().eq(HomeBanner::getTitle, title)).getId();
    }

    private void closeCity(long cityId, String token) throws Exception {
        JsonNode detail = ok(getJson("/api/admin/cities/" + cityId, token));
        ok(postJson("/api/admin/cities/" + cityId + "/close", """
                {"version":%d}
                """.formatted(detail.path("version").asLong()), token));
    }

    private String allBody(Integer sortOrder, String title) {
        String sort = sortOrder == null ? "" : ",\"sortOrder\":" + sortOrder;
        String titleJson = title == null ? "" : ",\"title\":\"" + title + "\"";
        return """
                {"imageUrl":"https://img.example/all.png","linkUrl":"/articles","scope":"ALL"%s%s}
                """.formatted(titleJson, sort);
    }

    private String cityBody(long cityId, boolean enabled, String title) {
        return """
                {"title":"%s","imageUrl":"https://img.example/city.png","linkUrl":"/stores","scope":"CITY","cityId":%d,"sortOrder":30,"enabled":%s}
                """.formatted(title, cityId, enabled);
    }

    private void insertAgent() {
        AdminUser agent = new AdminUser();
        agent.setPhone("13800000081");
        agent.setPasswordHash(passwordEncoder.encode("SeedAdmin#2026"));
        agent.setDisplayName("客服");
        agent.setRole(AdminRole.AGENT);
        agent.setEnabled(true);
        adminUserMapper.insert(agent);
    }

    private void insertMerchant(String phone) {
        MerchantOrg org = new MerchantOrg();
        org.setName("隔离商家");
        org.setStatus(MerchantOrgStatus.ACTIVE);
        merchantOrgMapper.insert(org);
        MerchantUser user = new MerchantUser();
        user.setMerchantOrgId(org.getId());
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode("merchant12"));
        user.setDisplayName("商家");
        user.setEnabled(true);
        merchantUserMapper.insert(user);
    }

    private String adminToken() throws Exception {
        return login("/api/admin/auth/login", "17606838920", "123456");
    }

    private String registerTourist(String phone) throws Exception {
        JsonNode body = ok(postJson("/api/auth/register", """
                {"phone":"%s","password":"secret12","displayName":"游客"}
                """.formatted(phone), null));
        return body.path("token").asText();
    }

    private String login(String path, String phone, String password) throws Exception {
        JsonNode body = ok(postJson(path, """
                {"phone":"%s","password":"%s"}
                """.formatted(phone, password), null));
        return body.path("token").asText();
    }

    private long auditCount(AuditAction action, long targetId) {
        Long count = auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, action)
                .eq(AuditLog::getTargetType, AuditTarget.HOME_BANNER.code())
                .eq(AuditLog::getTargetId, targetId));
        return count == null ? 0 : count;
    }

    private Region region(RegionLevel level, String slug) {
        Region region = regionMapper.selectOne(Wrappers.<Region>lambdaQuery()
                .eq(Region::getSlug, slug)
                .eq(Region::getLevel, level));
        assertThat(region).isNotNull();
        return region;
    }

    private List<String> titles(JsonNode items) {
        List<String> titles = new ArrayList<>();
        for (JsonNode item : items) {
            titles.add(item.path("title").isNull() ? null : item.path("title").asText());
        }
        return titles;
    }

    private JsonNode ok(HttpResult result) {
        assertThat(result.status()).isEqualTo(200);
        assertThat(result.body().path("code").asInt()).isZero();
        return result.body().path("data");
    }

    private void expect(MockHttpServletRequestBuilder request, int status, String message) throws Exception {
        HttpResult result = exchange(request);
        assertThat(result.status()).isEqualTo(status);
        assertThat(result.body().path("code").asInt()).isEqualTo(status);
        assertThat(result.body().path("message").asText()).isEqualTo(message);
    }

    private HttpResult getJson(String path, String token) throws Exception {
        MockHttpServletRequestBuilder request = get(path);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        }
        return exchange(request);
    }

    private HttpResult postJson(String path, String json, String token) throws Exception {
        MockHttpServletRequestBuilder request = post(path).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        }
        return exchange(request);
    }

    private HttpResult putJson(String path, String json, String token) throws Exception {
        return exchange(put(path)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private HttpResult deleteJson(String path, String token) throws Exception {
        return exchange(delete(path).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private HttpResult exchange(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();
        String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode body = raw == null || raw.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(raw);
        return new HttpResult(result.getResponse().getStatus(), body, raw);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record HttpResult(int status, JsonNode body, String raw) {

        void satisfies(java.util.function.Consumer<HttpResult> assertion) {
            assertion.accept(this);
        }
    }
}
