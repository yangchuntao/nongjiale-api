package com.suzhong.njl.web.merchant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.entity.PortalUser;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.mapper.PortalUserMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.StoreMapper;

@SpringBootTest(properties = "app.cache.enabled=false")
@AutoConfigureMockMvc
@Transactional
class MerchantApiIntegrationTest {

    private static final String PASSWORD = "merchant12";
    private static final String RAW_PHONE = "13812348000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MerchantOrgMapper merchantOrgMapper;

    @Autowired
    private MerchantUserMapper merchantUserMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private BookingIntentMapper bookingIntentMapper;

    @Test
    void loginRejectsDisabledOrgOrUserAndTokenStaysOnMerchant() throws Exception {
        long orgId = insertOrg("甲商家", MerchantOrgStatus.ACTIVE);
        insertUser(orgId, "13700001001", true);
        JsonNode loggedIn = postJson("/api/merchant/auth/login", """
                {"phone":"13700001001","password":"%s"}
                """.formatted(PASSWORD), null);
        assertThat(loggedIn.path("code").asInt()).isZero();
        assertThat(loggedIn.path("data").path("role").asText()).isEqualTo("MERCHANT");
        assertThat(loggedIn.path("data").path("merchantOrgId").asLong()).isEqualTo(orgId);
        assertThat(loggedIn.path("data").path("merchantUserId").asLong()).isPositive();
        String token = loggedIn.path("data").path("token").asText();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/api/admin/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/api/booking-intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/merchant/stores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String tourist = registerTourist("13900007001");
        mockMvc.perform(get("/api/merchant/stores").header(HttpHeaders.AUTHORIZATION, "Bearer " + tourist))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        JsonNode admin = postJson("/api/admin/auth/login", """
                {"phone":"13800000000","password":"SeedAdmin#2026"}
                """, null);
        mockMvc.perform(get("/api/merchant/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.path("data").path("token").asText()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        long disabledUserOrg = insertOrg("停用账号主体", MerchantOrgStatus.ACTIVE);
        insertUser(disabledUserOrg, "13700001002", false);
        mockMvc.perform(post("/api/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13700001002","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("账号已停用"));

        long disabledOrg = insertOrg("停用主体", MerchantOrgStatus.DISABLED);
        insertUser(disabledOrg, "13700001003", true);
        mockMvc.perform(post("/api/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13700001003","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("商家已停用"));

        mockMvc.perform(post("/api/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13700001001","password":"wrong-pass"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void twoMerchantsCannotSeeEachOthersStoresAndCannotWriteShelfOrHome() throws Exception {
        long regionId = regionId();
        long orgA = insertOrg("甲商家", MerchantOrgStatus.ACTIVE);
        long orgB = insertOrg("乙商家", MerchantOrgStatus.ACTIVE);
        insertUser(orgA, "13700002001", true);
        insertUser(orgB, "13700002002", true);
        long storeA = insertStore(orgA, regionId, "甲店", ShelfStatus.ON_SHELF, true, 3);
        long storeB = insertStore(orgB, regionId, "乙店", ShelfStatus.OFF_SHELF, false, 8);
        String tokenA = login("13700002001");
        String tokenB = login("13700002002");

        mockMvc.perform(get("/api/merchant/stores").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("甲店"))
                .andExpect(jsonPath("$.data.items[0].merchantOrgId").value((int) orgA));

        mockMvc.perform(get("/api/merchant/stores/" + storeB).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("门店不存在"));
        mockMvc.perform(get("/api/merchant/stores/" + storeA).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/merchant/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));

        String forbiddenHome = updateBody(regionId, 0, "甲店改名", "新套餐", ",\"homeRecommended\":false,\"homeSort\":1");
        mockMvc.perform(put("/api/merchant/stores/" + storeA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forbiddenHome))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        String forbiddenShelf = updateBody(regionId, 0, "甲店改名", "新套餐", ",\"shelfStatus\":\"OFF_SHELF\"");
        mockMvc.perform(put("/api/merchant/stores/" + storeA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forbiddenShelf))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        Store untouched = storeMapper.selectById(storeA);
        assertThat(untouched.getName()).isEqualTo("甲店");
        assertThat(untouched.getShelfStatus()).isEqualTo(ShelfStatus.ON_SHELF);
        assertThat(untouched.getHomeRecommended()).isTrue();
        assertThat(untouched.getHomeSort()).isEqualTo(3);

        mockMvc.perform(put("/api/merchant/stores/" + storeA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(regionId, 0, "甲店改名", "新套餐", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("甲店改名"))
                .andExpect(jsonPath("$.data.packageText").value("新套餐"))
                .andExpect(jsonPath("$.data.shelfStatus").value("ON_SHELF"))
                .andExpect(jsonPath("$.data.homeRecommended").value(true))
                .andExpect(jsonPath("$.data.homeSort").value(3));

        mockMvc.perform(get("/api/stores/" + storeA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.packageText").value("新套餐"));
        mockMvc.perform(get("/api/stores/" + storeB))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(410));
    }

    @Test
    void shelfApplicationIsPendingOncePerStore() throws Exception {
        long regionId = regionId();
        long orgA = insertOrg("甲商家", MerchantOrgStatus.ACTIVE);
        long orgB = insertOrg("乙商家", MerchantOrgStatus.ACTIVE);
        insertUser(orgA, "13700003001", true);
        insertUser(orgB, "13700003002", true);
        long storeA = insertStore(orgA, regionId, "甲店", ShelfStatus.OFF_SHELF, false, 9);
        long storeB = insertStore(orgB, regionId, "乙店", ShelfStatus.ON_SHELF, false, 9);
        String tokenA = login("13700003001");
        String tokenB = login("13700003002");

        mockMvc.perform(post("/api/merchant/stores/" + storeA + "/shelf-applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"PUBLISH","reason":"准备接待"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.action").value("PUBLISH"))
                .andExpect(jsonPath("$.data.merchantOrgId").value((int) orgA));

        assertThat(storeMapper.selectById(storeA).getShelfStatus()).isEqualTo(ShelfStatus.OFF_SHELF);

        mockMvc.perform(post("/api/merchant/stores/" + storeA + "/shelf-applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"UNPUBLISH","reason":"再申请一次"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("已有待审的上下架申请"));

        mockMvc.perform(post("/api/merchant/stores/" + storeB + "/shelf-applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"UNPUBLISH"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/merchant/shelf-applications").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.items[0].storeId").value((int) storeA));
        mockMvc.perform(get("/api/merchant/shelf-applications").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void intentsAreReadOnlyMaskedAndIsolated() throws Exception {
        long regionId = regionId();
        long orgA = insertOrg("甲商家", MerchantOrgStatus.ACTIVE);
        long orgB = insertOrg("乙商家", MerchantOrgStatus.ACTIVE);
        insertUser(orgA, "13700004001", true);
        insertUser(orgB, "13700004002", true);
        long storeA = insertStore(orgA, regionId, "甲店", ShelfStatus.ON_SHELF, false, 4);
        long storeB = insertStore(orgB, regionId, "乙店", ShelfStatus.ON_SHELF, false, 4);
        long portalUserId = insertPortalUser("13600004001");
        long intentA = insertIntent(orgA, storeA, portalUserId, "NJL-M-A", RAW_PHONE);
        long intentB = insertIntent(orgB, storeB, portalUserId, "NJL-M-B", "13900008888");
        String tokenA = login("13700004001");

        String listBody = mockMvc.perform(get("/api/merchant/intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value((int) intentA))
                .andExpect(jsonPath("$.data.items[0].contactPhone").value("138****8000"))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        assertThat(listBody).doesNotContain(RAW_PHONE);

        String detailBody = mockMvc.perform(get("/api/merchant/intents/" + intentA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactPhone").value("138****8000"))
                .andExpect(jsonPath("$.data.storeName").value("甲店"))
                .andReturn().getResponse().getContentAsString();
        assertThat(detailBody).doesNotContain(RAW_PHONE);

        mockMvc.perform(get("/api/merchant/intents/" + intentB)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("意向不存在"));

        mockMvc.perform(post("/api/merchant/intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
        mockMvc.perform(put("/api/merchant/intents/" + intentA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED","contactPhone":"13800000000"}
                                """))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));

        BookingIntent stored = bookingIntentMapper.selectById(intentA);
        assertThat(stored.getStatus()).isEqualTo(IntentStatus.PENDING);
        assertThat(stored.getContactPhone()).isEqualTo(RAW_PHONE);
    }

    private String login(String phone) throws Exception {
        JsonNode body = postJson("/api/merchant/auth/login", """
                {"phone":"%s","password":"%s"}
                """.formatted(phone, PASSWORD), null);
        assertThat(body.path("code").asInt()).isZero();
        return body.path("data").path("token").asText();
    }

    private String registerTourist(String phone) throws Exception {
        JsonNode body = postJson("/api/auth/register", """
                {"phone":"%s","password":"secret12","displayName":"游客"}
                """.formatted(phone), null);
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

    private long insertOrg(String name, MerchantOrgStatus status) {
        MerchantOrg org = new MerchantOrg();
        org.setName(name);
        org.setContactName("联系人");
        org.setContactPhone("13700000000");
        org.setStatus(status);
        merchantOrgMapper.insert(org);
        return org.getId();
    }

    private void insertUser(long orgId, String phone, boolean enabled) {
        MerchantUser user = new MerchantUser();
        user.setMerchantOrgId(orgId);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setDisplayName(phone);
        user.setEnabled(enabled);
        merchantUserMapper.insert(user);
    }

    private long insertStore(long orgId, long regionId, String name, ShelfStatus shelfStatus, boolean homeRecommended, int homeSort) {
        Store store = new Store();
        store.setMerchantOrgId(orgId);
        store.setRegionId(regionId);
        store.setName(name);
        store.setShelfStatus(shelfStatus);
        store.setHomeRecommended(homeRecommended);
        store.setHomeSort(homeSort);
        store.setTestData(false);
        store.setVersion(0L);
        store.setPackageText("原套餐");
        store.setAddress("原地址");
        storeMapper.insert(store);
        return store.getId();
    }

    private long insertPortalUser(String phone) {
        PortalUser user = new PortalUser();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode("secret12"));
        user.setDisplayName("游客");
        portalUserMapper.insert(user);
        return user.getId();
    }

    private long insertIntent(long orgId, long storeId, long userId, String intentNo, String phone) {
        BookingIntent intent = new BookingIntent();
        intent.setIntentNo(intentNo);
        intent.setUserId(userId);
        intent.setStoreId(storeId);
        intent.setMerchantOrgId(orgId);
        intent.setCheckIn(LocalDate.of(2026, 11, 20));
        intent.setCheckOut(LocalDate.of(2026, 11, 22));
        intent.setGuests(2);
        intent.setContactName("游客甲");
        intent.setContactPhone(phone);
        intent.setStatus(IntentStatus.PENDING);
        intent.setVersion(0L);
        bookingIntentMapper.insert(intent);
        return intent.getId();
    }

    private long regionId() {
        Region region = regionMapper.selectOne(Wrappers.<Region>lambdaQuery().eq(Region::getSlug, "guangling"));
        assertThat(region).isNotNull();
        return region.getId();
    }

    private String updateBody(long regionId, long version, String name, String packageText, String extra) {
        return """
                {"name":"%s","regionId":%d,"version":%d,"packageText":"%s","address":"新地址"%s}
                """.formatted(name, regionId, version, packageText, extra);
    }
}
