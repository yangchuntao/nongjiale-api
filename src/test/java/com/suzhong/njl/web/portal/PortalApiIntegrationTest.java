package com.suzhong.njl.web.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.domain.entity.Article;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.mapper.ArticleMapper;
import com.suzhong.njl.mapper.StoreMapper;

@SpringBootTest(properties = "app.cache.enabled=false")
@AutoConfigureMockMvc
@Transactional
class PortalApiIntegrationTest {

    private static final String COUSIN_PACKAGE = "含双早与农家晚餐，可住两晚，周末不加价。";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Test
    void touristRegistersLogsInAndReadsMe() throws Exception {
        String phone = "13900001111";
        JsonNode registered = postJson("/api/auth/register", """
                {"phone":"%s","password":"secret12"}
                """.formatted(phone), null);
        assertThat(registered.path("code").asInt()).isZero();
        assertThat(registered.path("data").path("role").asText()).isEqualTo("TOURIST");
        assertThat(registered.path("data").path("token").asText()).isNotBlank();

        JsonNode loggedIn = postJson("/api/auth/login", """
                {"phone":"%s","password":"secret12"}
                """.formatted(phone), null);
        String token = loggedIn.path("data").path("token").asText();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value(phone))
                .andExpect(jsonPath("$.data.role").value("TOURIST"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","password":"secret12"}
                                """.formatted(phone)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void publicConfigAndRegionsComeFromSeed() throws Exception {
        mockMvc.perform(get("/api/config/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.siteName").value("站点名称"))
                .andExpect(jsonPath("$.data.brandColor").value("#4BA3D9"))
                .andExpect(jsonPath("$.data.contactPhone").value("400-000-0000"));

        mockMvc.perform(get("/api/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.slug == 'guangling')].name").value(Matchers.contains("广陵区")));
    }

    @Test
    void homeFeedAndStoreListRespectShelfRecommendationAndPackageText() throws Exception {
        mockMvc.perform(get("/api/home/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stores[?(@.name == '表弟店')].testData").value(Matchers.contains(false)))
                .andExpect(jsonPath("$.data.stores[?(@.name == '崇川江边小院')]").isEmpty())
                .andExpect(jsonPath("$.data.articles[?(@.title == '苏中周末住哪里')].summary").isNotEmpty());

        mockMvc.perform(get("/api/stores").param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("崇川江边小院"))
                .andExpect(jsonPath("$.data.page").value(1));

        mockMvc.perform(get("/api/stores").param("priceMin", "200").param("priceMax", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("表弟店"));

        long guanglingId = regionId("guangling");
        mockMvc.perform(get("/api/stores").param("regionId", String.valueOf(guanglingId)))
                .andExpect(jsonPath("$.data.items[0].name").value("表弟店"));

        mockMvc.perform(get("/api/stores").param("tag", "river-food"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("表弟店"));

        long cousinId = storeId("表弟店");
        mockMvc.perform(get("/api/stores/" + cousinId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.packageText").value(COUSIN_PACKAGE))
                .andExpect(jsonPath("$.data.packages[0].description").value("仅床位，不含餐"))
                .andExpect(jsonPath("$.data.testData").value(false));

        long riversideId = storeId("海陵河畔农家");
        mockMvc.perform(get("/api/stores/" + riversideId))
                .andExpect(jsonPath("$.data.packages").isEmpty())
                .andExpect(jsonPath("$.data.packageText").value("临河一晚，含早餐与骑行路线。"));
    }

    @Test
    void offShelfDetailIsGoneAndTestDataStaysListed() throws Exception {
        Store cousin = storeMapper.selectById(storeId("表弟店"));
        Store hidden = new Store();
        hidden.setMerchantOrgId(cousin.getMerchantOrgId());
        hidden.setRegionId(cousin.getRegionId());
        hidden.setName("临时下架店");
        hidden.setShelfStatus(ShelfStatus.OFF_SHELF);
        hidden.setHomeRecommended(false);
        hidden.setHomeSort(90);
        hidden.setTestData(false);
        hidden.setVersion(0L);
        hidden.setPackageText("下架文案");
        storeMapper.insert(hidden);

        mockMvc.perform(get("/api/stores/" + hidden.getId()))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(410))
                .andExpect(jsonPath("$.message").value("已下架或不可用"));

        mockMvc.perform(get("/api/stores/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        Store marked = new Store();
        marked.setMerchantOrgId(cousin.getMerchantOrgId());
        marked.setRegionId(cousin.getRegionId());
        marked.setName("导入样例店");
        marked.setShelfStatus(ShelfStatus.ON_SHELF);
        marked.setHomeRecommended(false);
        marked.setHomeSort(80);
        marked.setTestData(true);
        marked.setVersion(0L);
        marked.setPriceFrom(new java.math.BigDecimal("210.00"));
        marked.setPackageText("导入套餐文案");
        storeMapper.insert(marked);

        mockMvc.perform(get("/api/stores").param("tag", "missing-tag"))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/stores/" + marked.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testData").value(true))
                .andExpect(jsonPath("$.data.testTag").value("测试"));
    }

    @Test
    void createIntentAndIsolateMine() throws Exception {
        String tokenA = register("13900003333");
        String tokenB = register("13900004444");
        long cousinId = storeId("表弟店");

        mockMvc.perform(post("/api/booking-intents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intentBody(cousinId, "2026-11-20", "2026-11-22", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        JsonNode created = postJson("/api/booking-intents", intentBody(cousinId, "2026-11-20", "2026-11-22", null), tokenA);
        assertThat(created.path("code").asInt()).isZero();
        assertThat(created.path("data").path("status").asText()).isEqualTo("PENDING");
        assertThat(created.path("data").path("intentNo").asText()).startsWith("NJL");
        long intentId = created.path("data").path("id").asLong();

        mockMvc.perform(post("/api/booking-intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intentBody(cousinId, "2026-11-22", "2026-11-20", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/api/booking-intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intentBody(999999L, "2026-11-20", "2026-11-22", null)))
                .andExpect(status().isNotFound());

        Store cousin = storeMapper.selectById(cousinId);
        Store hidden = new Store();
        hidden.setMerchantOrgId(cousin.getMerchantOrgId());
        hidden.setRegionId(cousin.getRegionId());
        hidden.setName("不可订下架店");
        hidden.setShelfStatus(ShelfStatus.OFF_SHELF);
        hidden.setHomeRecommended(false);
        hidden.setHomeSort(91);
        hidden.setTestData(false);
        hidden.setVersion(0L);
        storeMapper.insert(hidden);
        mockMvc.perform(post("/api/booking-intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intentBody(hidden.getId(), "2026-11-20", null, 2)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(410));

        mockMvc.perform(get("/api/me/intents").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].intentNo").value(created.path("data").path("intentNo").asText()));

        mockMvc.perform(get("/api/me/intents").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/me/intents/" + intentId).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(post("/api/booking-intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId":%d,"checkIn":"2026-11-20","checkOut":"2026-11-22","guests":2,"contactName":"甲","contactPhone":"13900003333","amount":99}
                                """.formatted(cousinId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void tokensDoNotCrossPortalAndAdmin() throws Exception {
        String tourist = register("13900005555");
        mockMvc.perform(get("/api/admin/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tourist))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        JsonNode admin = postJson("/api/admin/auth/login", """
                {"phone":"13800000000","password":"SeedAdmin#2026"}
                """, null);
        String adminToken = admin.path("data").path("token").asText();
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/api/booking-intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intentBody(storeId("表弟店"), "2026-11-20", "2026-11-21", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void publishedArticlesAndMerchantApply() throws Exception {
        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.title == '苏中周末住哪里')]").isNotEmpty());

        long articleId = objectMapper.readTree(mockMvc.perform(get("/api/articles"))
                .andReturn().getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asLong();
        mockMvc.perform(get("/api/articles/" + articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        Article draft = new Article();
        draft.setTitle("未发布草稿");
        draft.setStatus(ArticleStatus.DRAFT);
        draft.setCreatedBy(1L);
        draft.setSummary("草稿");
        draft.setBody("草稿正文");
        articleMapper.insert(draft);
        mockMvc.perform(get("/api/articles/" + draft.getId()))
                .andExpect(status().isNotFound());

        long regionId = regionId("yangzhou");
        mockMvc.perform(post("/api/merchant-applies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgName":"新农家","contactName":"王五","contactPhone":"13900006666","plannedStoreName":"新店","regionId":%d,"description":"想入驻"}
                                """.formatted(regionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    private String register(String phone) throws Exception {
        JsonNode body = postJson("/api/auth/register", """
                {"phone":"%s","password":"secret12","displayName":"游客%s"}
                """.formatted(phone, phone.substring(7)), null);
        return body.path("data").path("token").asText();
    }

    private JsonNode postJson(String path, String json, String token) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String intentBody(long storeId, String checkIn, String checkOut, Integer nights) {
        String checkOutJson = checkOut == null ? "null" : "\"" + checkOut + "\"";
        String nightsJson = nights == null ? "null" : nights.toString();
        return """
                {"storeId":%d,"checkIn":"%s","checkOut":%s,"nights":%s,"guests":2,"contactName":"甲","contactPhone":"13900003333"}
                """.formatted(storeId, checkIn, checkOutJson, nightsJson);
    }

    private long storeId(String name) throws Exception {
        JsonNode items = objectMapper.readTree(mockMvc.perform(get("/api/stores").param("pageSize", "100"))
                .andReturn().getResponse().getContentAsString()).path("data").path("items");
        for (JsonNode item : items) {
            if (name.equals(item.path("name").asText())) {
                return item.path("id").asLong();
            }
        }
        throw new AssertionError("找不到门店 " + name);
    }

    private long regionId(String slug) throws Exception {
        JsonNode items = objectMapper.readTree(mockMvc.perform(get("/api/regions"))
                .andReturn().getResponse().getContentAsString()).path("data");
        for (JsonNode item : items) {
            if (slug.equals(item.path("slug").asText())) {
                return item.path("id").asLong();
            }
        }
        throw new AssertionError("找不到地区 " + slug);
    }
}
