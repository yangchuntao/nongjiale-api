package com.suzhong.njl.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suzhong.njl.domain.entity.AuditLog;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.IntentNote;
import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.entity.PortalUser;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.entity.Store;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.IntentStatus;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.domain.enums.ShelfStatus;
import com.suzhong.njl.mapper.AuditLogMapper;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.IntentNoteMapper;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.mapper.PortalUserMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.mapper.StoreMapper;

@SpringBootTest(properties = "app.cache.enabled=false")
@AutoConfigureMockMvc
@Transactional
class AdminApiIntegrationTest {

    private static final String ADMIN_PHONE = "17606838920";
    private static final String ADMIN_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private MerchantOrgMapper merchantOrgMapper;

    @Autowired
    private MerchantUserMapper merchantUserMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private BookingIntentMapper bookingIntentMapper;

    @Autowired
    private IntentNoteMapper intentNoteMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void merchantAndAnonymousCannotCallAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/merchant-applies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        long orgId = insertOrg("隔离商家", MerchantOrgStatus.ACTIVE);
        insertUser(orgId, "13700008001", true, "merchant12");
        String merchantToken = merchantLogin("13700008001", "merchant12");
        mockMvc.perform(get("/api/admin/merchant-applies").header(HttpHeaders.AUTHORIZATION, "Bearer " + merchantToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/api/admin/stores").header(HttpHeaders.AUTHORIZATION, "Bearer " + merchantToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        String tourist = registerTourist("13900008001");
        mockMvc.perform(get("/api/admin/intents").header(HttpHeaders.AUTHORIZATION, "Bearer " + tourist))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void adminCanCreateApplyOnBehalfOfMerchant() throws Exception {
        String admin = adminToken();
        long regionId = regionId();

        JsonNode created = postJson("/api/admin/merchant-applies", """
                {"orgName":"代录农庄","contactName":"赵六","contactPhone":"13700008222",
                 "plannedStoreName":"代录门店","regionId":%d,"description":"电话口头登记"}
                """.formatted(regionId), admin);
        assertThat(created.path("code").asInt()).isZero();
        assertThat(created.path("data").path("status").asText()).isEqualTo("PENDING");
        long applyId = created.path("data").path("id").asLong();
        assertThat(applyId).isPositive();

        // 来源标记 ADMIN，其余字段与提交一致
        mockMvc.perform(get("/api/admin/merchant-applies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("status", "PENDING")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == " + applyId + ")].source").value(hasItem("ADMIN")))
                .andExpect(jsonPath("$.data.items[?(@.id == " + applyId + ")].orgName").value(hasItem("代录农庄")));

        // 代录的申请与自填申请走同一审核流
        JsonNode approved = postJson("/api/admin/merchant-applies/" + applyId + "/approve", """
                {"password":"merchant12"}
                """, admin);
        assertThat(approved.path("code").asInt()).isZero();
        assertThat(approved.path("data").path("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    void adminCreateApplyValidatesSameRulesAsPortal() throws Exception {
        String admin = adminToken();
        long regionId = regionId();

        // 主体名称必填
        mockMvc.perform(post("/api/admin/merchant-applies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgName":"  ","contactPhone":"13700008333"}
                                """))
                .andExpect(status().isBadRequest());

        // 电话格式不对
        mockMvc.perform(post("/api/admin/merchant-applies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgName":"格式错误农庄","contactPhone":"12345"}
                                """))
                .andExpect(status().isBadRequest());

        // 地区不存在
        mockMvc.perform(post("/api/admin/merchant-applies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgName":"地区错误农庄","regionId":999999}
                                """))
                .andExpect(status().isBadRequest());

        // 只有主体名称也可以提交（其余字段均可空），且地区合法时通过
        JsonNode minimal = postJson("/api/admin/merchant-applies", """
                {"orgName":"最小农庄","regionId":%d}
                """.formatted(regionId), admin);
        assertThat(minimal.path("code").asInt()).isZero();
        assertThat(minimal.path("data").path("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void merchantAndAnonymousCannotCreateApplyOnBehalf() throws Exception {
        // 未登录
        mockMvc.perform(post("/api/admin/merchant-applies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgName":"越权农庄"}
                                """))
                .andExpect(status().isUnauthorized());

        // 商家令牌
        long orgId = insertOrg("代录越权商家", MerchantOrgStatus.ACTIVE);
        insertUser(orgId, "13700008444", true, "merchant12");
        String merchantToken = merchantLogin("13700008444", "merchant12");
        mockMvc.perform(post("/api/admin/merchant-applies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgName":"越权农庄"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveCreatesMerchantLoginAndSecondReviewConflicts() throws Exception {        String admin = adminToken();
        long regionId = regionId();
        long pendingId = submitApply(regionId, "13700008111", "待开通农庄");

        mockMvc.perform(get("/api/admin/merchant-applies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[?(@.id == " + pendingId + ")].status").value(hasItem("PENDING")));

        JsonNode approved = postJson("/api/admin/merchant-applies/" + pendingId + "/approve", """
                {"password":"merchant12"}
                """, admin);
        assertThat(approved.path("code").asInt()).isZero();
        assertThat(approved.path("data").path("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.path("data").path("plainPassword").asText()).isEqualTo("merchant12");
        assertThat(approved.path("data").path("phone").asText()).isEqualTo("13700008111");
        long orgId = approved.path("data").path("merchantOrgId").asLong();
        assertThat(merchantOrgMapper.selectById(orgId).getStatus()).isEqualTo(MerchantOrgStatus.ACTIVE);
        assertThat(merchantUserMapper.selectOne(Wrappers.<MerchantUser>lambdaQuery().eq(MerchantUser::getPhone, "13700008111")))
                .isNotNull();
        merchantLogin("13700008111", "merchant12");
        assertThat(auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, AuditAction.APPLY_APPROVE)
                .eq(AuditLog::getTargetId, pendingId))).isEqualTo(1);

        mockMvc.perform(post("/api/admin/merchant-applies/" + pendingId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void rejectRequiresReasonAndBlocksMerchantLogin() throws Exception {
        String admin = adminToken();
        long regionId = regionId();
        long rejectId = submitApply(regionId, "13700008112", "待驳回农庄");
        long generatedId = submitApply(regionId, "13700008113", "临时密码农庄");

        mockMvc.perform(post("/api/admin/merchant-applies/" + rejectId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        JsonNode rejected = postJson("/api/admin/merchant-applies/" + rejectId + "/reject", """
                {"reason":"证照不清晰"}
                """, admin);
        assertThat(rejected.path("data").path("status").asText()).isEqualTo("REJECTED");
        assertThat(rejected.path("data").path("rejectReason").asText()).isEqualTo("证照不清晰");
        mockMvc.perform(post("/api/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13700008112","password":"merchant12"}
                                """))
                .andExpect(status().isUnauthorized());
        assertThat(auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, AuditAction.APPLY_REJECT)
                .eq(AuditLog::getTargetId, rejectId))).isEqualTo(1);

        JsonNode generated = postJson("/api/admin/merchant-applies/" + generatedId + "/approve", "{}", admin);
        String plain = generated.path("data").path("plainPassword").asText();
        assertThat(plain).hasSizeGreaterThanOrEqualTo(8);
        merchantLogin("13700008113", plain);

        mockMvc.perform(post("/api/admin/merchant-applies/" + rejectId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void disableOrgAndResetPasswordMatchMerchantLoginCopy() throws Exception {
        String admin = adminToken();
        long orgId = insertOrg("可停用商家", MerchantOrgStatus.ACTIVE);
        insertUser(orgId, "13700008201", true, "merchant12");
        String merchantToken = merchantLogin("13700008201", "merchant12");

        JsonNode disabled = patchJson("/api/admin/merchants/" + orgId, """
                {"status":"DISABLED"}
                """, admin);
        assertThat(disabled.path("data").path("status").asText()).isEqualTo("DISABLED");
        mockMvc.perform(get("/api/merchant/stores").header(HttpHeaders.AUTHORIZATION, "Bearer " + merchantToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(post("/api/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13700008201","password":"merchant12"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("商家已停用"));

        patchJson("/api/admin/merchants/" + orgId, """
                {"status":"ACTIVE","enabled":false}
                """, admin);
        mockMvc.perform(post("/api/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13700008201","password":"merchant12"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("账号已停用"));

        patchJson("/api/admin/merchants/" + orgId, """
                {"enabled":true}
                """, admin);
        JsonNode reset = postJson("/api/admin/merchants/" + orgId + "/reset-password", """
                {"password":"newpass12"}
                """, admin);
        assertThat(reset.path("data").path("plainPassword").asText()).isEqualTo("newpass12");
        mockMvc.perform(post("/api/merchant/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13700008201","password":"merchant12"}
                                """))
                .andExpect(status().isUnauthorized());
        merchantLogin("13700008201", "newpass12");

        assertThat(auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, AuditAction.MERCHANT_DISABLE)
                .eq(AuditLog::getTargetId, orgId))).isPositive();
        assertThat(auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, AuditAction.MERCHANT_ENABLE)
                .eq(AuditLog::getTargetId, orgId))).isPositive();
    }

    @Test
    void adminCreatesStoreAndCanWriteHomeAndShelf() throws Exception {
        String admin = adminToken();
        long regionId = regionId();
        long orgId = insertOrg("代建商家", MerchantOrgStatus.ACTIVE);
        insertUser(orgId, "13700008301", true, "merchant12");
        String merchantToken = merchantLogin("13700008301", "merchant12");

        mockMvc.perform(post("/api/merchant/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        JsonNode created = postJson("/api/admin/merchants/" + orgId + "/stores", """
                {"name":"代建农庄","regionId":%d,"address":"广陵路1号","packageText":"含早","shelfStatus":"OFF_SHELF","homeRecommended":false,"homeSort":100}
                """.formatted(regionId), admin);
        assertThat(created.path("code").asInt()).isZero();
        long storeId = created.path("data").path("id").asLong();
        assertThat(created.path("data").path("merchantOrgId").asLong()).isEqualTo(orgId);
        assertThat(created.path("data").path("shelfStatus").asText()).isEqualTo("OFF_SHELF");
        mockMvc.perform(get("/api/stores/" + storeId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(410));

        mockMvc.perform(get("/api/merchant/stores/" + storeId).header(HttpHeaders.AUTHORIZATION, "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("代建农庄"));

        JsonNode updated = putJson("/api/admin/stores/" + storeId, storeBody(regionId, 0, "代建农庄", "ON_SHELF", true, 1), admin);
        assertThat(updated.path("data").path("homeRecommended").asBoolean()).isTrue();
        assertThat(updated.path("data").path("homeSort").asInt()).isEqualTo(1);
        assertThat(updated.path("data").path("shelfStatus").asText()).isEqualTo("ON_SHELF");
        assertThat(updated.path("data").path("address").asText()).isEqualTo("广陵路1号");

        mockMvc.perform(get("/api/stores/" + storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.homeRecommended").value(true))
                .andExpect(jsonPath("$.data.homeSort").value(1));
        mockMvc.perform(get("/api/home/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stores[?(@.id == " + storeId + ")].homeRecommended").value(hasItem(true)));

        long version = updated.path("data").path("version").asLong();
        putJson("/api/admin/stores/" + storeId, storeBody(regionId, version, "代建农庄", "OFF_SHELF", true, 1), admin);
        mockMvc.perform(get("/api/stores/" + storeId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(410));

        assertThat(auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, AuditAction.SHELF_FORCE)
                .eq(AuditLog::getTargetId, storeId))).isEqualTo(2);

        mockMvc.perform(get("/api/admin/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("merchantOrgId", String.valueOf(orgId))
                        .param("shelfStatus", "OFF_SHELF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == " + storeId + ")].name").value(hasItem("代建农庄")));

        mockMvc.perform(put("/api/admin/stores/" + storeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storeBody(regionId, 0, "冲突改名", "ON_SHELF", true, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
        assertThat(storeMapper.selectById(storeId).getName()).isEqualTo("代建农庄");
    }

    @Test
    void shelfReviewChangesVisibilityOnlyWhenApproved() throws Exception {
        String admin = adminToken();
        long regionId = regionId();
        long orgId = insertOrg("上下架商家", MerchantOrgStatus.ACTIVE);
        insertUser(orgId, "13700008401", true, "merchant12");
        long storeId = insertStore(orgId, regionId, "待上架店", ShelfStatus.OFF_SHELF, false, 20);
        String merchantToken = merchantLogin("13700008401", "merchant12");

        JsonNode apply = postJson("/api/merchant/stores/" + storeId + "/shelf-applications", """
                {"action":"PUBLISH","reason":"可以接待"}
                """, merchantToken);
        long applicationId = apply.path("data").path("id").asLong();

        mockMvc.perform(get("/api/admin/shelf-applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == " + applicationId + ")].action").value(hasItem("PUBLISH")));

        mockMvc.perform(post("/api/admin/shelf-applications/" + applicationId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        postJson("/api/admin/shelf-applications/" + applicationId + "/reject", """
                {"reason":"图片不足"}
                """, admin);
        assertThat(storeMapper.selectById(storeId).getShelfStatus()).isEqualTo(ShelfStatus.OFF_SHELF);
        mockMvc.perform(get("/api/stores/" + storeId)).andExpect(status().isGone());

        JsonNode again = postJson("/api/merchant/stores/" + storeId + "/shelf-applications", """
                {"action":"PUBLISH","reason":"已补图"}
                """, merchantToken);
        long secondId = again.path("data").path("id").asLong();
        JsonNode approved = postJson("/api/admin/shelf-applications/" + secondId + "/approve", "{}", admin);
        assertThat(approved.path("data").path("status").asText()).isEqualTo("APPROVED");
        assertThat(storeMapper.selectById(storeId).getShelfStatus()).isEqualTo(ShelfStatus.ON_SHELF);
        mockMvc.perform(get("/api/stores/" + storeId)).andExpect(status().isOk());

        JsonNode unpublish = postJson("/api/merchant/stores/" + storeId + "/shelf-applications", """
                {"action":"UNPUBLISH","reason":"装修"}
                """, merchantToken);
        long unpublishId = unpublish.path("data").path("id").asLong();
        postJson("/api/admin/shelf-applications/" + unpublishId + "/approve", "{}", admin);
        assertThat(storeMapper.selectById(storeId).getShelfStatus()).isEqualTo(ShelfStatus.OFF_SHELF);
        mockMvc.perform(get("/api/stores/" + storeId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(410));

        assertThat(auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, AuditAction.SHELF_REVIEW)
                .eq(AuditLog::getTargetId, secondId))).isEqualTo(1);

        mockMvc.perform(post("/api/admin/shelf-applications/" + applicationId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void intentStatusMachineNotesAndExport() throws Exception {
        String admin = adminToken();
        long regionId = regionId();
        long orgId = insertOrg("意向商家", MerchantOrgStatus.ACTIVE);
        insertUser(orgId, "13700008501", true, "merchant12");
        String merchantToken = merchantLogin("13700008501", "merchant12");
        long storeId = insertStore(orgId, regionId, "意向店", ShelfStatus.ON_SHELF, false, 30);
        long portalUserId = insertPortalUser("13600008501");
        long confirmedId = insertIntent(orgId, storeId, portalUserId, "NJL-BE6-C", "13811112222", IntentStatus.PENDING);
        long landedId = insertIntent(orgId, storeId, portalUserId, "NJL-BE6-L", "13933334444", IntentStatus.PENDING);
        long pendingId = insertIntent(orgId, storeId, portalUserId, "NJL-BE6-P", "13755556666", IntentStatus.PENDING);

        JsonNode confirmed = postJson("/api/admin/intents/" + confirmedId + "/status", """
                {"targetStatus":"CONFIRMED","version":0,"note":"电话确认"}
                """, admin);
        assertThat(confirmed.path("data").path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(confirmed.path("data").path("contactPhone").asText()).isEqualTo("13811112222");
        assertThat(confirmed.path("data").path("notes")).hasSize(1);

        JsonNode landed = postJson("/api/admin/intents/" + landedId + "/status", """
                {"targetStatus":"CONFIRMED","version":0,"note":"先确认"}
                """, admin);
        long landedVersion = landed.path("data").path("version").asLong();
        JsonNode checkedIn = postJson("/api/admin/intents/" + landedId + "/status", """
                {"targetStatus":"CHECKED_IN","version":%d,"note":"客人已到"}
                """.formatted(landedVersion), admin);
        assertThat(checkedIn.path("data").path("status").asText()).isEqualTo("CHECKED_IN");
        assertThat(checkedIn.path("data").path("statusLabel").asText()).isEqualTo("已入住");

        JsonNode noted = postJson("/api/admin/intents/" + confirmedId + "/notes", """
                {"note":"补一句"}
                """, admin);
        assertThat(noted.path("data").path("notes")).hasSize(2);
        assertThat(intentNoteMapper.selectCount(Wrappers.<IntentNote>lambdaQuery().eq(IntentNote::getIntentId, confirmedId)))
                .isEqualTo(2);

        mockMvc.perform(get("/api/admin/intents/" + confirmedId).header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactPhone").value("13811112222"))
                .andExpect(jsonPath("$.data.notes[0].note").value("电话确认"))
                .andExpect(jsonPath("$.data.notes[1].note").value("补一句"));

        mockMvc.perform(get("/api/merchant/intents/" + confirmedId).header(HttpHeaders.AUTHORIZATION, "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.contactPhone").value("138****2222"));

        mockMvc.perform(get("/api/admin/intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("status", "CONFIRMED")
                        .param("storeId", String.valueOf(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].intentNo").value("NJL-BE6-C"));

        mockMvc.perform(get("/api/admin/intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("status", "CHECKED_IN")
                        .param("storeId", String.valueOf(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].statusLabel").value("已入住"));

        String csv = mockMvc.perform(get("/api/admin/intents/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("storeId", String.valueOf(storeId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("已确认");
        assertThat(csv).contains("已入住");
        assertThat(csv).doesNotContain("已达成");
        assertThat(csv).doesNotContain("已落地");
        assertThat(csv).doesNotContain("已落实");
        assertThat(csv).doesNotContain("已成交");
        assertThat(csv).contains("NJL-BE6-C");
        assertThat(csv).contains("NJL-BE6-L");
        assertThat(csv).contains("意向店");
        assertThat(csv).contains("13811112222");
        assertThat(csv).contains("13933334444");
        assertThat(csv).doesNotContain("佣金");

        String confirmedCsv = mockMvc.perform(get("/api/admin/intents/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("storeId", String.valueOf(storeId))
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(confirmedCsv).contains("NJL-BE6-C");
        assertThat(confirmedCsv).doesNotContain("NJL-BE6-L");

        assertThat(auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, AuditAction.INTENT_STATUS)
                .eq(AuditLog::getTargetId, landedId))).isEqualTo(2);

        mockMvc.perform(post("/api/admin/intents/" + pendingId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus":"CHECKED_IN","version":0,"note":"还没确认"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("不能从待跟进改为已入住"));
        assertThat(bookingIntentMapper.selectById(pendingId).getStatus()).isEqualTo(IntentStatus.PENDING);
    }

    @Test
    void intentVersionConflictDoesNotOverwrite() throws Exception {
        String admin = adminToken();
        long regionId = regionId();
        long orgId = insertOrg("版本商家", MerchantOrgStatus.ACTIVE);
        long storeId = insertStore(orgId, regionId, "版本店", ShelfStatus.ON_SHELF, false, 31);
        long portalUserId = insertPortalUser("13600008502");
        long intentId = insertIntent(orgId, storeId, portalUserId, "NJL-BE6-V", "13800001111", IntentStatus.PENDING);
        JsonNode confirmed = postJson("/api/admin/intents/" + intentId + "/status", """
                {"targetStatus":"CONFIRMED","version":0}
                """, admin);
        assertThat(confirmed.path("data").path("version").asLong()).isPositive();

        mockMvc.perform(post("/api/admin/intents/" + intentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus":"CHECKED_IN","version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("数据已被修改"));
        assertThat(bookingIntentMapper.selectById(intentId).getStatus()).isEqualTo(IntentStatus.CONFIRMED);
    }

    @Test
    void unfilteredIntentListKeepsEveryKnownStatusAndRejectsUnknownCodes() throws Exception {
        String admin = adminToken();
        long regionId = regionId();
        long orgId = insertOrg("全部状态商家", MerchantOrgStatus.ACTIVE);
        long storeId = insertStore(orgId, regionId, "全部状态店", ShelfStatus.ON_SHELF, false, 32);
        long portalUserId = insertPortalUser("13600008503");
        for (IntentStatus status : IntentStatus.values()) {
            insertIntent(orgId, storeId, portalUserId, "NJL-ALL-" + status.code(), "1380000" + status.ordinal(), status);
        }
        long dirtyId = insertIntent(orgId, storeId, portalUserId, "NJL-ALL-DIRTY", "13800009999", IntentStatus.PENDING);
        jdbcTemplate.update("UPDATE booking_intent SET status = ? WHERE id = ?", "LANDED", dirtyId);

        MvcResult all = mockMvc.perform(get("/api/admin/intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("storeId", String.valueOf(storeId)))
                .andReturn();
        assertThat(all.getResponse().getStatus())
                .as(all.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo(200);
        JsonNode allBody = objectMapper.readTree(all.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(allBody.path("data").path("total").asInt()).isEqualTo(IntentStatus.values().length + 1);
        assertThat(allBody.path("data").path("items").toString()).contains("NJL-ALL-DIRTY");
        assertThat(allBody.path("data").path("items").toString()).contains("已入住");

        for (IntentStatus status : IntentStatus.values()) {
            mockMvc.perform(get("/api/admin/intents")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                            .param("storeId", String.valueOf(storeId))
                            .param("status", status.code()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.items[0].status").value(status.code()))
                    .andExpect(jsonPath("$.data.items[0].statusLabel").value(status.label()));
        }

        mockMvc.perform(get("/api/admin/intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("status", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/admin/intents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("status", "LANDED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数不正确"));

        mockMvc.perform(get("/api/admin/intents/" + dirtyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intentNo").value("NJL-ALL-DIRTY"))
                .andExpect(jsonPath("$.data.status").value(nullValue()))
                .andExpect(jsonPath("$.data.statusLabel").value(""));

        mockMvc.perform(post("/api/admin/intents/" + dirtyId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus":"CONFIRMED","version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("不能改为已确认"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM booking_intent WHERE id = ?", String.class, dirtyId))
                .isEqualTo("LANDED");

        String csv = mockMvc.perform(get("/api/admin/intents/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)
                        .param("storeId", String.valueOf(storeId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("NJL-ALL-DIRTY");
        assertThat(csv).contains("已入住");
        assertThat(csv).doesNotContain("已落地");
    }

    @Test
    void articlesAndSiteConfigReachThePortal() throws Exception {
        String admin = adminToken();
        mockMvc.perform(get("/api/admin/site-config").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brandColor").value("#4BA3D9"))
                .andExpect(jsonPath("$.data.siteName").value("站点名称"))
                .andExpect(jsonPath("$.data.contactPhone").value("400-000-0000"));

        JsonNode draft = postJson("/api/admin/articles", """
                {"title":"草稿攻略","summary":"还没发","body":"正文","status":"DRAFT"}
                """, admin);
        long articleId = draft.path("data").path("id").asLong();
        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == " + articleId + ")]").isEmpty());

        putJson("/api/admin/articles/" + articleId, """
                {"title":"周末去广陵","summary":"已发布","body":"正文","status":"PUBLISHED"}
                """, admin);
        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == " + articleId + ")].title").value(hasItem("周末去广陵")));
        mockMvc.perform(get("/api/articles/" + articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        putJson("/api/admin/site-config", """
                {"siteName":"苏中农家乐","brandColor":"#112233","contactPhone":"400-111-2222"}
                """, admin);
        mockMvc.perform(get("/api/config/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteName").value("苏中农家乐"))
                .andExpect(jsonPath("$.data.brandColor").value("#112233"))
                .andExpect(jsonPath("$.data.contactPhone").value("400-111-2222"));

        assertThat(auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getAction, AuditAction.SITE_CONFIG_UPDATE))).isPositive();
    }

    private String adminToken() throws Exception {
        JsonNode body = postJson("/api/admin/auth/login", """
                {"phone":"%s","password":"%s"}
                """.formatted(ADMIN_PHONE, ADMIN_PASSWORD), null);
        assertThat(body.path("code").asInt()).isZero();
        return body.path("data").path("token").asText();
    }

    private String merchantLogin(String phone, String password) throws Exception {
        JsonNode body = postJson("/api/merchant/auth/login", """
                {"phone":"%s","password":"%s"}
                """.formatted(phone, password), null);
        assertThat(body.path("code").asInt()).isZero();
        return body.path("data").path("token").asText();
    }

    private String registerTourist(String phone) throws Exception {
        JsonNode body = postJson("/api/auth/register", """
                {"phone":"%s","password":"secret12","displayName":"游客"}
                """.formatted(phone), null);
        assertThat(body.path("code").asInt()).isZero();
        return body.path("data").path("token").asText();
    }

    private long submitApply(long regionId, String phone, String orgName) throws Exception {
        JsonNode body = postJson("/api/merchant-applies", """
                {"orgName":"%s","contactName":"联系人","contactPhone":"%s","plannedStoreName":"%s店","regionId":%d}
                """.formatted(orgName, phone, orgName, regionId), null);
        assertThat(body.path("code").asInt()).isZero();
        return body.path("data").path("id").asLong();
    }

    private JsonNode postJson(String path, String json, String token) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private JsonNode putJson(String path, String json, String token) throws Exception {
        MvcResult result = mockMvc.perform(put(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.path("code").asInt()).isZero();
        return body;
    }

    private JsonNode patchJson(String path, String json, String token) throws Exception {
        MvcResult result = mockMvc.perform(patch(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body.path("code").asInt()).isZero();
        return body;
    }

    private String storeBody(long regionId, long version, String name, String shelfStatus, boolean homeRecommended, int homeSort) {
        return """
                {"name":"%s","regionId":%d,"address":"广陵路1号","packageText":"含早","shelfStatus":"%s","homeRecommended":%s,"homeSort":%d,"version":%d}
                """.formatted(name, regionId, shelfStatus, homeRecommended, homeSort, version);
    }

    private long regionId() {
        Region region = regionMapper.selectOne(Wrappers.<Region>lambdaQuery().eq(Region::getSlug, "guangling"));
        assertThat(region).isNotNull();
        return region.getId();
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

    private void insertUser(long orgId, String phone, boolean enabled, String password) {
        MerchantUser user = new MerchantUser();
        user.setMerchantOrgId(orgId);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(phone);
        user.setEnabled(enabled);
        merchantUserMapper.insert(user);
    }

    private long insertStore(long orgId, long regionId, String name, ShelfStatus shelfStatus, boolean homeRecommended, int homeSort) {
        Store store = new Store();
        store.setMerchantOrgId(orgId);
        store.setRegionId(regionId);
        store.setName(name);
        store.setAddress("原地址");
        store.setPackageText("原套餐");
        store.setShelfStatus(shelfStatus);
        store.setHomeRecommended(homeRecommended);
        store.setHomeSort(homeSort);
        store.setTestData(false);
        store.setVersion(0L);
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

    private long insertIntent(long orgId, long storeId, long userId, String intentNo, String phone, IntentStatus status) {
        BookingIntent intent = new BookingIntent();
        intent.setIntentNo(intentNo);
        intent.setUserId(userId);
        intent.setStoreId(storeId);
        intent.setMerchantOrgId(orgId);
        intent.setCheckIn(LocalDate.of(2026, 11, 20));
        intent.setCheckOut(LocalDate.of(2026, 11, 22));
        intent.setNights(2);
        intent.setGuests(2);
        intent.setContactName("游客甲");
        intent.setContactPhone(phone);
        intent.setUserRemark("想看湖");
        intent.setStatus(status);
        intent.setVersion(0L);
        bookingIntentMapper.insert(intent);
        return intent.getId();
    }
}
