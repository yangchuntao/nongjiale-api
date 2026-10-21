package com.suzhong.njl.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.domain.entity.Article;
import com.suzhong.njl.domain.entity.AuditLog;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.PortalUser;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.SiteConfig;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;
import com.suzhong.njl.domain.enums.SeedMarket;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.domain.enums.SiteConfigKey;
import com.suzhong.njl.mapper.ArticleMapper;
import com.suzhong.njl.mapper.AuditLogMapper;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.PortalUserMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.RegionOpenChecklistMapper;
import com.suzhong.njl.mapper.SiteConfigMapper;
import com.suzhong.njl.mapper.StoreMapper;

@SpringBootTest(properties = "app.cache.enabled=false")
@AutoConfigureMockMvc
@Transactional
class CityOpenIntegrationTest {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private RegionOpenChecklistMapper checklistMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SiteConfigMapper siteConfigMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private BookingIntentMapper bookingIntentMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Test
    void seedCitiesStayOpenWithManualChecksAndLiveCounts() throws Exception {
        String token = adminToken();
        JsonNode list = ok(getJson("/api/admin/cities", token)).path("items");
        assertThat(slugs(list)).contains("yangzhou", "taizhou", "nantong");
        JsonNode yangzhou = bySlug(list, "yangzhou");
        assertThat(yangzhou.path("provinceName").asText()).isEqualTo("江苏省");
        assertThat(yangzhou.path("openStatus").asText()).isEqualTo("OPEN");
        assertThat(yangzhou.path("name").asText()).isEqualTo("扬州市");
        assertThat(yangzhou.path("storeCount").asLong()).isEqualTo(1);
        assertThat(yangzhou.path("articleCount").asLong()).isEqualTo(0);
        assertThat(yangzhou.path("updatedAt").asText()).isNotBlank();

        JsonNode detail = ok(getJson("/api/admin/cities/" + yangzhou.path("id").asLong(), token));
        assertThat(detail.path("enabled").asBoolean()).isTrue();
        assertThat(detail.path("minStores").asInt()).isEqualTo(5);
        assertThat(detail.path("minArticles").asInt()).isEqualTo(1);
        assertThat(detail.path("version").asLong()).isZero();
        assertThat(detail.path("updatedBy").isNull()).isTrue();
        assertThat(detail.path("checklist").path("destinationReady").asBoolean()).isTrue();
        assertThat(detail.path("checklist").path("supportCovered").asBoolean()).isTrue();
        assertThat(detail.path("checklist").path("dirtyDataCleared").asBoolean()).isTrue();
        assertThat(detail.path("checklist").path("supplyReady").asBoolean()).isFalse();
        assertThat(detail.path("checklist").path("articlesReady").asBoolean()).isFalse();
        assertThat(detail.path("canOpen").asBoolean()).isFalse();
        assertThat(detail.path("canClose").asBoolean()).isTrue();
        assertThat(texts(detail.path("missingReasons"))).containsExactly(
                "供给未达标（当前 1 / 需要 5）",
                "攻略未达标（当前 0 / 需要 1）");

        JsonNode config = ok(getJson("/api/admin/site-config", token));
        assertThat(config.path("brandColor").asText()).isEqualTo("#4BA3D9");
        assertThat(config.has("minStores")).isFalse();

        JsonNode regions = ok(getJson("/api/regions", null));
        assertThat(slugs(regions)).contains("jiangsu", "yangzhou", "guangling", "taizhou", "nantong");
    }

    @Test
    void rejectsOpenWhenChecklistOrCountsAreShort() throws Exception {
        String token = adminToken();
        Region city = insertCity("yancheng", "盐城市", provinceId(), true, null, 40);
        JsonNode listed = bySlug(ok(getJson("/api/admin/cities", token)).path("items"), "yancheng");
        assertThat(listed.path("openStatus").asText()).isEqualTo("CLOSED");

        long before = auditCount(AuditAction.CITY_OPEN, city.getId());
        JsonNode rejected = call("POST", "/api/admin/cities/" + city.getId() + "/open", "{\"version\":0}", token, 409);
        assertThat(rejected.path("message").asText()).isEqualTo(
                "未达标，不能开通：目的地数据未齐；供给未达标（当前 0 / 需要 5）；攻略未达标（当前 0 / 需要 1）；客服覆盖未说明；测数或脏数据未确认清理");
        assertThat(rejected.path("data").isNull()).isTrue();
        assertThat(auditCount(AuditAction.CITY_OPEN, city.getId())).isEqualTo(before);
        assertThat(checklistMapper.selectById(city.getId())).isNull();

        long yangzhouId = regionId("yangzhou");
        JsonNode already = call("POST", "/api/admin/cities/" + yangzhouId + "/open", "{\"version\":0}", token, 409);
        assertThat(already.path("message").asText()).isEqualTo("该市已开通");

        JsonNode closed = ok(postJson("/api/admin/cities/" + yangzhouId + "/close", "{\"version\":0}", token));
        assertThat(closed.path("openStatus").asText()).isEqualTo("CLOSED");
        assertThat(closed.path("version").asLong()).isEqualTo(1);
        JsonNode stillShort = call(
                "POST",
                "/api/admin/cities/" + yangzhouId + "/open",
                "{\"version\":1}",
                token,
                409);
        assertThat(stillShort.path("message").asText())
                .isEqualTo("未达标，不能开通：供给未达标（当前 1 / 需要 5）；攻略未达标（当前 0 / 需要 1）");
        assertThat(regionMapper.selectById(yangzhouId).getOpenStatus()).isEqualTo(RegionOpenStatus.CLOSED);
    }

    @Test
    void opensWhenManualChecksAndLiveCountsMeet() throws Exception {
        String token = adminToken();
        long adminId = adminId(token);
        Region city = insertCity("yancheng", "盐城市", provinceId(), true, RegionOpenStatus.CLOSED, 40);
        Region district = insertRegion("tinghu", "亭湖区", RegionLevel.DISTRICT, city.getId(), true, null, 41);
        Region disabledDistrict = insertRegion("dongtai", "东台市", RegionLevel.DISTRICT, city.getId(), false, null, 42);
        long orgId = orgId();
        for (int i = 1; i <= 4; i++) {
            insertStore(orgId, city.getId(), "市区店" + i, ShelfStatus.ON_SHELF, false, false);
        }
        long testStoreId = insertStore(orgId, city.getId(), "导入店", ShelfStatus.ON_SHELF, true, false);
        insertStore(orgId, city.getId(), "下架店", ShelfStatus.OFF_SHELF, false, false);
        insertStore(orgId, disabledDistrict.getId(), "停用区店", ShelfStatus.ON_SHELF, false, false);

        JsonNode patched = ok(patchJson(
                "/api/admin/cities/" + city.getId() + "/checklist",
                "{\"destinationReady\":true,\"supportCovered\":true,\"dirtyDataCleared\":true,\"version\":0}",
                token));
        assertThat(patched.path("version").asLong()).isEqualTo(1);
        assertThat(patched.path("checklist").path("destinationReady").asBoolean()).isTrue();
        assertThat(patched.path("updatedBy").asLong()).isEqualTo(adminId);
        assertThat(patched.path("canOpen").asBoolean()).isFalse();
        JsonNode shortOpen = call("POST", "/api/admin/cities/" + city.getId() + "/open", "{\"version\":1}", token, 409);
        assertThat(shortOpen.path("message").asText())
                .isEqualTo("未达标，不能开通：供给未达标（当前 4 / 需要 5）；攻略未达标（当前 0 / 需要 1）");

        long districtStore = insertStore(orgId, district.getId(), "区县店", ShelfStatus.ON_SHELF, false, true);
        insertArticle(district.getId(), ArticleStatus.DRAFT, "草稿不算", adminId);
        insertArticle(district.getId(), ArticleStatus.PUBLISHED, "亭湖攻略", adminId);

        JsonNode ready = ok(getJson("/api/admin/cities/" + city.getId(), token));
        assertThat(ready.path("storeCount").asLong()).isEqualTo(5);
        assertThat(ready.path("articleCount").asLong()).isEqualTo(1);
        assertThat(ready.path("checklist").path("supplyReady").asBoolean()).isTrue();
        assertThat(ready.path("checklist").path("articlesReady").asBoolean()).isTrue();
        assertThat(ready.path("canOpen").asBoolean()).isTrue();
        assertThat(ready.path("missingReasons").size()).isZero();

        JsonNode opened = ok(postJson("/api/admin/cities/" + city.getId() + "/open", "{\"version\":1}", token));
        assertThat(opened.path("openStatus").asText()).isEqualTo("OPEN");
        assertThat(opened.path("canOpen").asBoolean()).isFalse();
        assertThat(opened.path("canClose").asBoolean()).isTrue();
        assertThat(opened.path("version").asLong()).isEqualTo(2);
        assertThat(auditCount(AuditAction.CITY_OPEN, city.getId())).isEqualTo(1);
        AuditLog audit = latestAudit(AuditAction.CITY_OPEN, city.getId());
        assertThat(audit.getTargetType()).isEqualTo(AuditTarget.REGION.code());
        assertThat(audit.getDetailJson()).contains("OPEN");

        JsonNode regions = ok(getJson("/api/regions", null));
        assertThat(slugs(regions)).contains("yancheng", "tinghu").doesNotContain("dongtai");

        JsonNode stores = ok(getJson("/api/stores?regionId=" + city.getId(), null));
        assertThat(stores.path("destinationStatus").asText()).isEqualTo("OPEN");
        assertThat(stores.path("emptyHint").isNull()).isTrue();
        assertThat(stores.path("total").asLong()).isEqualTo(6);
        assertThat(names(stores.path("items"))).contains("区县店", "导入店").doesNotContain("下架店", "停用区店");
        JsonNode marked = byName(stores.path("items"), "导入店");
        assertThat(marked.path("testData").asBoolean()).isTrue();
        assertThat(marked.path("testTag").asText()).isEqualTo("测试");

        JsonNode feed = ok(getJson("/api/home/feed", null));
        assertThat(names(feed.path("stores"))).contains("区县店");
        assertThat(titles(feed.path("articles"))).contains("亭湖攻略");

        Store districtRow = storeMapper.selectById(districtStore);
        districtRow.setShelfStatus(ShelfStatus.OFF_SHELF);
        storeMapper.updateById(districtRow);
        JsonNode dropped = ok(getJson("/api/admin/cities/" + city.getId(), token));
        assertThat(dropped.path("openStatus").asText()).isEqualTo("OPEN");
        assertThat(dropped.path("checklist").path("supplyReady").asBoolean()).isFalse();
        assertThat(dropped.path("canOpen").asBoolean()).isFalse();
        assertThat(dropped.path("canClose").asBoolean()).isTrue();
        assertThat(texts(dropped.path("missingReasons"))).containsExactly("供给未达标（当前 4 / 需要 5）");

        JsonNode again = call("POST", "/api/admin/cities/" + city.getId() + "/open", "{\"version\":2}", token, 409);
        assertThat(again.path("message").asText()).isEqualTo("该市已开通");
        assertThat(storeMapper.selectById(testStoreId)).isNotNull();
    }

    @Test
    void closedCityIsHiddenAndDirectStoreLinksAreGone() throws Exception {
        String token = adminToken();
        long adminId = adminId(token);
        long yangzhouId = regionId("yangzhou");
        long cousinId = storeId("表弟店");
        long articleId = insertArticle(regionId("guangling"), ArticleStatus.PUBLISHED, "广陵关城攻略", adminId);
        JsonNode closed = ok(postJson("/api/admin/cities/" + yangzhouId + "/close", "{\"version\":0}", token));
        assertThat(closed.path("checklist").path("destinationReady").asBoolean()).isTrue();

        JsonNode regions = ok(getJson("/api/regions", null));
        assertThat(slugs(regions)).doesNotContain("yangzhou", "guangling").contains("taizhou", "nantong", "jiangsu");

        JsonNode hidden = ok(getJson("/api/stores?regionId=" + yangzhouId, null));
        assertThat(hidden.path("items")).isEmpty();
        assertThat(hidden.path("total").asLong()).isZero();
        assertThat(hidden.path("destinationStatus").asText()).isEqualTo("CLOSED");
        assertThat(hidden.path("emptyHint").asText()).isEqualTo("该目的地暂未开通");

        JsonNode districtHidden = ok(getJson("/api/stores?regionId=" + regionId("guangling"), null));
        assertThat(districtHidden.path("destinationStatus").asText()).isEqualTo("CLOSED");
        assertThat(names(districtHidden.path("items"))).doesNotContain("表弟店", "海陵河畔农家");

        JsonNode defaults = ok(getJson("/api/stores", null));
        assertThat(defaults.path("destinationStatus").asText()).isEqualTo("NONE");
        assertThat(defaults.path("emptyHint").isNull()).isTrue();
        assertThat(names(defaults.path("items"))).doesNotContain("表弟店").contains("海陵河畔农家");

        MvcResult gone = mockMvc.perform(get("/api/stores/" + cousinId)).andReturn();
        assertThat(gone.getResponse().getStatus()).isEqualTo(410);
        JsonNode goneBody = body(gone);
        assertThat(goneBody.path("code").asInt()).isEqualTo(410);
        assertThat(goneBody.path("message").asText()).isEqualTo("已下架或不可用");
        assertThat(goneBody.path("data").isNull()).isTrue();

        String tourist = register("13900001234");
        MvcResult intent = mockMvc.perform(post("/api/booking-intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tourist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId":%d,"checkIn":"2026-12-01","nights":1,"guests":2,"contactName":"甲","contactPhone":"13900001234"}
                                """.formatted(cousinId)))
                .andReturn();
        assertThat(intent.getResponse().getStatus()).isEqualTo(410);
        assertThat(body(intent).path("message").asText()).isEqualTo("已下架或不可用");

        mockMvc.perform(get("/api/admin/stores/" + cousinId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode articles = ok(getJson("/api/articles?pageSize=100", null));
        assertThat(titles(articles.path("items"))).doesNotContain("广陵关城攻略").contains("苏中周末住哪里");
        mockMvc.perform(get("/api/articles/" + articleId)).andExpect(status().isOk());
        JsonNode feed = ok(getJson("/api/home/feed", null));
        assertThat(names(feed.path("stores"))).doesNotContain("表弟店");
        assertThat(titles(feed.path("articles"))).doesNotContain("广陵关城攻略").contains("苏中周末住哪里");

        mockMvc.perform(post("/api/merchant-applies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgName":"扬州新农家","contactPhone":"13900006666","regionId":%d}
                                """.formatted(yangzhouId)))
                .andExpect(status().isOk());

        JsonNode second = call("POST", "/api/admin/cities/" + yangzhouId + "/close", "{\"version\":1}", token, 409);
        assertThat(second.path("message").asText()).isEqualTo("该市未开通");
        assertThat(storeMapper.selectById(cousinId)).isNotNull();
    }

    @Test
    void checklistRejectsAutoFieldsAndStaleVersion() throws Exception {
        String token = adminToken();
        long yangzhouId = regionId("yangzhou");
        JsonNode auto = call(
                "PATCH",
                "/api/admin/cities/" + yangzhouId + "/checklist",
                "{\"destinationReady\":false,\"supplyReady\":true,\"version\":0}",
                token,
                400);
        assertThat(auto.path("message").asText()).isEqualTo("供给达标与攻略达标按实时计数自动勾选，不能手改");
        JsonNode articles = call(
                "PATCH",
                "/api/admin/cities/" + yangzhouId + "/checklist",
                "{\"articlesReady\":false,\"version\":0}",
                token,
                400);
        assertThat(articles.path("message").asText()).isEqualTo("供给达标与攻略达标按实时计数自动勾选，不能手改");

        JsonNode unchanged = ok(getJson("/api/admin/cities/" + yangzhouId, token));
        assertThat(unchanged.path("version").asLong()).isZero();
        assertThat(unchanged.path("checklist").path("destinationReady").asBoolean()).isTrue();

        JsonNode empty = call(
                "PATCH",
                "/api/admin/cities/" + yangzhouId + "/checklist",
                "{\"version\":0}",
                token,
                400);
        assertThat(empty.path("message").asText()).isEqualTo("请指定要更新的项");
        JsonNode missingVersion = call(
                "PATCH",
                "/api/admin/cities/" + yangzhouId + "/checklist",
                "{\"destinationReady\":true}",
                token,
                400);
        assertThat(missingVersion.path("message").asText()).isEqualTo("版本不能为空");

        JsonNode conflict = call(
                "PATCH",
                "/api/admin/cities/" + yangzhouId + "/checklist",
                "{\"supportCovered\":false,\"version\":9}",
                token,
                409);
        assertThat(conflict.path("message").asText()).isEqualTo("数据已被修改");

        JsonNode saved = ok(patchJson(
                "/api/admin/cities/" + yangzhouId + "/checklist",
                "{\"destinationReady\":false,\"version\":0}",
                token));
        assertThat(saved.path("version").asLong()).isEqualTo(1);
        assertThat(saved.path("checklist").path("destinationReady").asBoolean()).isFalse();
        assertThat(saved.path("checklist").path("supportCovered").asBoolean()).isTrue();
        assertThat(saved.path("checklist").path("dirtyDataCleared").asBoolean()).isTrue();
        assertThat(auditCount(AuditAction.CITY_CHECKLIST, yangzhouId)).isEqualTo(1);

        JsonNode staleOpen = call("POST", "/api/admin/cities/" + yangzhouId + "/open", "{\"version\":0}", token, 409);
        assertThat(staleOpen.path("message").asText()).isEqualTo("该市已开通");
        Region fresh = insertCity("huaian", "淮安市", provinceId(), true, RegionOpenStatus.CLOSED, 50);
        JsonNode stale = call("POST", "/api/admin/cities/" + fresh.getId() + "/open", "{\"version\":4}", token, 409);
        assertThat(stale.path("message").asText()).isEqualTo("数据已被修改");
        assertThat(regionMapper.selectById(fresh.getId()).getOpenStatus()).isEqualTo(RegionOpenStatus.CLOSED);

        JsonNode forced = call(
                "POST",
                "/api/admin/cities/" + fresh.getId() + "/open",
                "{\"version\":0,\"force\":true}",
                token,
                400);
        assertThat(forced.path("message").asText()).isEqualTo("请求体格式不正确");
    }

    @Test
    void metricsWindowCountsCurrentStatusAndExportsCsv() throws Exception {
        String token = adminToken();
        Region city = insertCity("metric-city", "=盐城,指标", provinceId(), true, RegionOpenStatus.CLOSED, 60);
        long orgId = orgId();
        long storeId = insertStore(orgId, city.getId(), "指标店", ShelfStatus.ON_SHELF, false, false);
        long userId = insertPortalUser("13900009999");
        LocalDateTime now = LocalDateTime.now(SHANGHAI);
        insertIntent(orgId, storeId, userId, IntentStatus.PENDING, now, "NJL-M-1");
        insertIntent(orgId, storeId, userId, IntentStatus.CONFIRMED, now, "NJL-M-2");
        insertIntent(orgId, storeId, userId, IntentStatus.RESCHEDULED, now, "NJL-M-3");
        insertIntent(orgId, storeId, userId, IntentStatus.CHECKED_IN, now, "NJL-M-4");
        insertIntent(orgId, storeId, userId, IntentStatus.CLOSED, now, "NJL-M-5");
        insertIntent(orgId, storeId, userId, IntentStatus.CHECKED_IN, now.minusDays(10), "NJL-M-6");
        insertIntent(orgId, storeId, userId, IntentStatus.CHECKED_IN, now.minusDays(40), "NJL-M-7");
        long provinceStore = insertStore(orgId, provinceId(), "省店", ShelfStatus.ON_SHELF, false, false);
        insertIntent(orgId, provinceStore, userId, IntentStatus.CONFIRMED, now, "NJL-M-8");

        JsonNode week = ok(getJson("/api/admin/metrics/city-intents?cityId=" + city.getId(), token));
        assertThat(week.path("window").asText()).isEqualTo("7");
        assertThat(week.path("from").asText()).contains("+08:00");
        assertRow(week.path("rows").get(0), null, "合计", 5, 1, 1, true);
        assertRow(week.path("rows").get(1), city.getId(), "=盐城,指标", 5, 1, 1, false);
        assertThat(week.path("rows").get(1).has("intentId")).isFalse();
        assertThat(week.path("rows").get(1).has("storeId")).isFalse();
        assertThat(week.path("rows").get(1).has("href")).isFalse();

        JsonNode month = ok(getJson("/api/admin/metrics/city-intents?window=30&cityId=" + city.getId(), token));
        assertThat(month.path("window").asText()).isEqualTo("30");
        assertRow(month.path("rows").get(1), city.getId(), "=盐城,指标", 6, 1, 2, false);

        JsonNode all = ok(getJson("/api/admin/metrics/city-intents?window=ALL&cityId=" + city.getId(), token));
        assertThat(all.path("from").isNull()).isTrue();
        assertRow(all.path("rows").get(0), null, "合计", 7, 1, 3, true);
        assertRow(all.path("rows").get(1), city.getId(), "=盐城,指标", 7, 1, 3, false);

        JsonNode defaults = ok(getJson("/api/admin/metrics/city-intents", token));
        assertThat(defaults.path("window").asText()).isEqualTo("7");
        long summed = 0;
        for (int i = 1; i < defaults.path("rows").size(); i++) {
            summed += defaults.path("rows").get(i).path("intentCount").asLong();
            assertThat(defaults.path("rows").get(i).path("total").asBoolean()).isFalse();
        }
        assertThat(defaults.path("rows").get(0).path("intentCount").asLong()).isEqualTo(summed);
        assertThat(defaults.path("rows").get(0).path("cityName").asText()).isEqualTo("合计");

        // 按省筛选：该省下全部启用市，目标市行计数不变
        JsonNode byProvince = ok(getJson("/api/admin/metrics/city-intents?window=ALL&province=江苏省", token));
        boolean found = false;
        for (int i = 1; i < byProvince.path("rows").size(); i++) {
            JsonNode row = byProvince.path("rows").get(i);
            if ("=盐城,指标".equals(row.path("cityName").asText())) {
                found = true;
                assertRow(row, city.getId(), "=盐城,指标", 7, 1, 3, false);
            }
        }
        assertThat(found).as("按省筛选后应包含指标市").isTrue();
        assertThat(byProvince.path("rows").get(0).path("cityName").asText()).isEqualTo("合计");

        MvcResult csvResult = mockMvc.perform(get("/api/admin/metrics/city-intents/export")
                        .param("window", "ALL")
                        .param("cityId", String.valueOf(city.getId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn();
        assertThat(csvResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(csvResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("city-intents.csv");
        assertThat(csvResult.getResponse().getContentType()).contains("text/csv");
        String csv = new String(csvResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF市,意向数,已确认,已入住\n");
        assertThat(csv).contains("合计,7,1,3\n");
        assertThat(csv).contains("\"'=盐城,指标\",7,1,3\n");

        JsonNode badWindow = call("GET", "/api/admin/metrics/city-intents?window=1", null, token, 400);
        assertThat(badWindow.path("message").asText()).isEqualTo("参数不正确");
        JsonNode missing = call("GET", "/api/admin/metrics/city-intents?cityId=999999", null, token, 404);
        assertThat(missing.path("message").asText()).isEqualTo("城市不存在");
        JsonNode province = call(
                "GET", "/api/admin/metrics/city-intents?cityId=" + provinceId(), null, token, 404);
        assertThat(province.path("message").asText()).isEqualTo("城市不存在");

        mockMvc.perform(get("/api/stores/" + provinceStore))
                .andExpect(status().isGone());
    }

    @Test
    void disabledCityAndBadThresholdStayOutOfThePortal() throws Exception {
        String token = adminToken();
        long adminId = adminId(token);
        Region disabled = insertCity("disabled-city", "停用市", provinceId(), false, RegionOpenStatus.OPEN, 70);
        assertThat(slugs(ok(getJson("/api/admin/cities", token)).path("items"))).doesNotContain("disabled-city");
        JsonNode detail = call("GET", "/api/admin/cities/" + disabled.getId(), null, token, 400);
        assertThat(detail.path("message").asText()).isEqualTo("地区不可用");
        JsonNode open = call("POST", "/api/admin/cities/" + disabled.getId() + "/open", "{\"version\":0}", token, 400);
        assertThat(open.path("message").asText()).isEqualTo("地区不可用");
        JsonNode metrics = call("GET", "/api/admin/metrics/city-intents?cityId=" + disabled.getId(), null, token, 404);
        assertThat(metrics.path("message").asText()).isEqualTo("城市不存在");
        JsonNode notCity = call("GET", "/api/admin/cities/" + provinceId(), null, token, 404);
        assertThat(notCity.path("message").asText()).isEqualTo("城市不存在");
        JsonNode missing = call("GET", "/api/admin/cities/999999", null, token, 404);
        assertThat(missing.path("message").asText()).isEqualTo("城市不存在");

        Region orphan = insertCity("orphan-city", "无省城市", null, true, RegionOpenStatus.CLOSED, 71);
        JsonNode orphanRow = bySlug(ok(getJson("/api/admin/cities", token)).path("items"), "orphan-city");
        assertThat(orphanRow.path("provinceName").isNull()).isTrue();

        SiteConfig stores = siteConfigMapper.selectById(SiteConfigKey.OPEN_CITY_MIN_STORES.code());
        stores.setConfigValue("xx");
        siteConfigMapper.updateById(stores);
        JsonNode fallback = ok(getJson("/api/admin/cities/" + regionId("yangzhou"), token));
        assertThat(fallback.path("minStores").asInt()).isEqualTo(5);

        stores.setConfigValue("1");
        siteConfigMapper.updateById(stores);
        SiteConfig articles = siteConfigMapper.selectById(SiteConfigKey.OPEN_CITY_MIN_ARTICLES.code());
        articles.setConfigValue("1");
        siteConfigMapper.updateById(articles);
        Region easy = insertCity("easy-city", "低阈值市", provinceId(), true, RegionOpenStatus.CLOSED, 72);
        insertStore(orgId(), easy.getId(), "一店", ShelfStatus.ON_SHELF, false, false);
        insertArticle(easy.getId(), ArticleStatus.PUBLISHED, "一篇", adminId);
        ok(patchJson(
                "/api/admin/cities/" + easy.getId() + "/checklist",
                "{\"destinationReady\":true,\"supportCovered\":true,\"dirtyDataCleared\":true,\"version\":0}",
                token));
        JsonNode opened = ok(postJson("/api/admin/cities/" + easy.getId() + "/open", "{\"version\":1}", token));
        assertThat(opened.path("openStatus").asText()).isEqualTo("OPEN");
        assertThat(opened.path("minStores").asInt()).isEqualTo(1);

        mockMvc.perform(get("/api/admin/cities")).andExpect(status().isUnauthorized());
        JsonNode unknownRegion = call("GET", "/api/stores?regionId=999999", null, null, 400);
        assertThat(unknownRegion.path("message").asText()).isEqualTo("地区不存在");
        Region switchedOff = regionMapper.selectById(regionId("hailing"));
        switchedOff.setEnabled(false);
        regionMapper.updateById(switchedOff);
        JsonNode unavailable = call("GET", "/api/stores?regionId=" + switchedOff.getId(), null, null, 400);
        assertThat(unavailable.path("message").asText()).isEqualTo("地区不可用");

        JsonNode provinceStores = ok(getJson("/api/stores?regionId=" + provinceId(), null));
        assertThat(provinceStores.path("destinationStatus").asText()).isEqualTo("NONE");
        assertThat(provinceStores.path("emptyHint").isNull()).isTrue();
        assertThat(names(provinceStores.path("items"))).contains("表弟店");
    }

    @Test
    void cityListPaginatesFiltersAndQueueCounts() throws Exception {
        String token = adminToken();

        // 分页：pageSize=2 切两页，total 一致且两页不重叠
        JsonNode page1 = ok(getJson("/api/admin/cities?page=1&pageSize=2", token));
        assertThat(page1.path("page").asInt()).isEqualTo(1);
        assertThat(page1.path("pageSize").asInt()).isEqualTo(2);
        assertThat(page1.path("items").size()).isEqualTo(2);
        long total = page1.path("total").asLong();
        assertThat(total).isGreaterThanOrEqualTo(3);
        JsonNode page2 = ok(getJson("/api/admin/cities?page=2&pageSize=2", token));
        assertThat(page2.path("total").asLong()).isEqualTo(total);
        assertThat(slugs(page2.path("items")).stream().distinct().count()).isEqualTo(2);
        assertThat(slugs(page1.path("items"))).doesNotContainAnyElementsOf(slugs(page2.path("items")));
        // 越界分页参数返回 400
        JsonNode badPage = call("GET", "/api/admin/cities?page=0", null, token, 400);
        assertThat(badPage.path("message").asText()).isEqualTo("分页参数不正确");

        // 状态筛选
        JsonNode openOnly = ok(getJson("/api/admin/cities?status=OPEN&pageSize=100", token));
        assertThat(openOnly.path("items").size()).isGreaterThanOrEqualTo(1);
        for (JsonNode row : openOnly.path("items")) {
            assertThat(row.path("openStatus").asText()).isEqualTo("OPEN");
        }

        // 省筛选与关键字筛选（中文直接写进模板，由 MockMvc 统一编码；不能用 URLEncoder 预编码，会被二次编码）
        JsonNode jiangsu = ok(getJson("/api/admin/cities?province=江苏省&pageSize=100", token));
        assertThat(slugs(jiangsu.path("items"))).contains("yangzhou");
        for (JsonNode row : jiangsu.path("items")) {
            assertThat(row.path("provinceName").asText()).isEqualTo("江苏省");
        }
        JsonNode keyword = ok(getJson("/api/admin/cities?keyword=yangzhou&pageSize=100", token));
        assertThat(slugs(keyword.path("items"))).containsExactly("yangzhou");

        // 待办队列计数：与直接按状态 count 库内的口径一致
        JsonNode counts = ok(getJson("/api/admin/queue-counts", token));
        assertThat(counts.path("intents").asLong()).isEqualTo(countOrZero(
                bookingIntentMapper.selectCount(Wrappers.<BookingIntent>lambdaQuery().eq(BookingIntent::getStatus, IntentStatus.PENDING))));
        assertThat(counts.path("merchantApplies").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(counts.path("shelfApplications").asLong()).isGreaterThanOrEqualTo(0);

        mockMvc.perform(get("/api/admin/queue-counts")).andExpect(status().isUnauthorized());
    }

    private long countOrZero(Long value) {
        return value == null ? 0 : value;
    }

    private void assertRow(
            JsonNode row,
            Long cityId,
            String cityName,
            long intentCount,
            long confirmedCount,
            long checkedInCount,
            boolean total) {
        if (cityId == null) {
            assertThat(row.path("cityId").isNull()).isTrue();
        } else {
            assertThat(row.path("cityId").asLong()).isEqualTo(cityId);
        }
        assertThat(row.path("cityName").asText()).isEqualTo(cityName);
        assertThat(row.path("intentCount").asLong()).isEqualTo(intentCount);
        assertThat(row.path("confirmedCount").asLong()).isEqualTo(confirmedCount);
        assertThat(row.path("checkedInCount").asLong()).isEqualTo(checkedInCount);
        assertThat(row.path("total").asBoolean()).isEqualTo(total);
    }

    private String adminToken() throws Exception {
        JsonNode body = ok(postJson("/api/admin/auth/login", """
                {"phone":"17606838920","password":"123456"}
                """, null));
        return body.path("token").asText();
    }

    private long adminId(String token) throws Exception {
        return ok(getJson("/api/admin/auth/me", token)).path("adminId").asLong();
    }

    private String register(String phone) throws Exception {
        JsonNode body = ok(postJson("/api/auth/register", """
                {"phone":"%s","password":"secret12","displayName":"游客"}
                """.formatted(phone), null));
        return body.path("token").asText();
    }

    private long provinceId() {
        return regionId("jiangsu");
    }

    private long regionId(String slug) {
        Region region = regionMapper.selectOne(Wrappers.<Region>lambdaQuery().eq(Region::getSlug, slug));
        assertThat(region).isNotNull();
        return region.getId();
    }

    private long orgId() {
        return storeMapper.selectOne(Wrappers.<Store>lambdaQuery().eq(Store::getName, "表弟店")).getMerchantOrgId();
    }

    private long storeId(String name) {
        Store store = storeMapper.selectOne(Wrappers.<Store>lambdaQuery().eq(Store::getName, name));
        assertThat(store).isNotNull();
        return store.getId();
    }

    private Region insertCity(String slug, String name, Long parentId, boolean enabled, RegionOpenStatus status, int sort) {
        return insertRegion(slug, name, RegionLevel.CITY, parentId, enabled, status, sort);
    }

    private Region insertRegion(
            String slug,
            String name,
            RegionLevel level,
            Long parentId,
            boolean enabled,
            RegionOpenStatus status,
            int sort) {
        Region region = new Region();
        region.setParentId(parentId);
        region.setSlug(slug);
        region.setName(name);
        region.setLevel(level);
        region.setSortOrder(sort);
        region.setEnabled(enabled);
        region.setSeedMarket(SeedMarket.SUZHONG);
        region.setOpenStatus(status);
        regionMapper.insert(region);
        return region;
    }

    private long insertStore(long orgId, long regionId, String name, ShelfStatus shelf, boolean testData, boolean home) {
        Store store = new Store();
        store.setMerchantOrgId(orgId);
        store.setRegionId(regionId);
        store.setName(name);
        store.setShelfStatus(shelf);
        store.setHomeRecommended(home);
        store.setHomeSort(3);
        store.setTestData(testData);
        store.setVersion(0L);
        store.setPackageText("套餐");
        storeMapper.insert(store);
        return store.getId();
    }

    private long insertArticle(Long regionId, ArticleStatus status, String title, long adminId) {
        Article article = new Article();
        article.setTitle(title);
        article.setStatus(status);
        article.setRegionId(regionId);
        article.setSummary("摘要");
        article.setBody("正文");
        article.setCreatedBy(adminId);
        if (status == ArticleStatus.PUBLISHED) {
            article.setPublishedAt(LocalDateTime.now(SHANGHAI));
        }
        articleMapper.insert(article);
        return article.getId();
    }

    private long insertPortalUser(String phone) {
        PortalUser user = new PortalUser();
        user.setPhone(phone);
        user.setPasswordHash("hash");
        user.setDisplayName("游客");
        portalUserMapper.insert(user);
        return user.getId();
    }

    private void insertIntent(
            long orgId,
            long storeId,
            long userId,
            IntentStatus status,
            LocalDateTime createdAt,
            String intentNo) {
        BookingIntent intent = new BookingIntent();
        intent.setIntentNo(intentNo);
        intent.setUserId(userId);
        intent.setStoreId(storeId);
        intent.setMerchantOrgId(orgId);
        intent.setCheckIn(LocalDate.of(2026, 12, 1));
        intent.setNights(1);
        intent.setGuests(2);
        intent.setContactName("甲");
        intent.setContactPhone("13900009999");
        intent.setStatus(status);
        intent.setVersion(0L);
        intent.setCreatedAt(createdAt);
        bookingIntentMapper.insert(intent);
    }

    private long auditCount(AuditAction action, long cityId) {
        Long count = auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, action)
                .eq(AuditLog::getTargetId, cityId));
        return count == null ? 0 : count;
    }

    private AuditLog latestAudit(AuditAction action, long cityId) {
        return auditLogMapper.selectOne(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, action)
                .eq(AuditLog::getTargetId, cityId)
                .orderByDesc(AuditLog::getId)
                .last("LIMIT 1"));
    }

    private JsonNode getJson(String path, String token) throws Exception {
        return call("GET", path, null, token, 200);
    }

    private JsonNode postJson(String path, String json, String token) throws Exception {
        return call("POST", path, json, token, 200);
    }

    private JsonNode patchJson(String path, String json, String token) throws Exception {
        return call("PATCH", path, json, token, 200);
    }

    private JsonNode ok(JsonNode envelope) {
        assertThat(envelope.path("code").asInt()).isZero();
        return envelope.path("data");
    }

    private JsonNode call(String method, String path, String json, String token, int status) throws Exception {
        var builder = switch (method) {
            case "POST" -> post(path);
            case "PATCH" -> patch(path);
            default -> get(path);
        };
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        MvcResult result = mockMvc.perform(builder).andReturn();
        assertThat(result.getResponse().getStatus())
                .withFailMessage(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo(status);
        return body(result);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode bySlug(JsonNode array, String slug) {
        for (JsonNode node : array) {
            if (slug.equals(node.path("slug").asText())) {
                return node;
            }
        }
        throw new AssertionError("找不到 " + slug);
    }

    private JsonNode byName(JsonNode array, String name) {
        for (JsonNode node : array) {
            if (name.equals(node.path("name").asText())) {
                return node;
            }
        }
        throw new AssertionError("找不到 " + name);
    }

    private List<String> slugs(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.path("slug").asText());
        }
        return values;
    }

    private List<String> names(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.path("name").asText());
        }
        return values;
    }

    private List<String> titles(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.path("title").asText());
        }
        return values;
    }

    private List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.asText());
        }
        return values;
    }
}
