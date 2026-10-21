package com.suzhong.njl.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.RegionLevel;
import com.suzhong.njl.domain.enums.RegionOpenStatus;
import com.suzhong.njl.domain.enums.RegionSource;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.service.admin.RegionImportService;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.admin.dto.RegionImportResult;
import com.suzhong.njl.web.admin.dto.RegionImportRow;

/**
 * 全国区划导入的单元与接口测试。
 *
 * <p>覆盖：内置数据完整性（省市区数量、层级连续）、幂等（重复导入不重复插入）、
 * 不覆盖开城状态与启停、非法数据拒绝、简称推导、以及同步接口的鉴权与返回。
 */
@SpringBootTest(properties = "app.cache.enabled=false")
@AutoConfigureMockMvc
@Transactional
class RegionImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private RegionImportService regionImportService;

    @Test
    void 内置数据覆盖全国且层级连续() {
        List<RegionImportRow> rows = regionImportService.read();

        // 34 个省级（含港澳台）、300 个以上市级、2800 个以上区县，低于这些数说明数据文件被截断
        assertThat(rows).hasSizeGreaterThan(3000);
        long provinces = rows.stream().filter(row -> row.level() == RegionLevel.PROVINCE.code()).count();
        long cities = rows.stream().filter(row -> row.level() == RegionLevel.CITY.code()).count();
        long districts = rows.stream().filter(row -> row.level() == RegionLevel.DISTRICT.code()).count();
        assertThat(provinces).isEqualTo(34);
        assertThat(cities).isGreaterThan(300);
        assertThat(districts).isGreaterThan(2800);

        // 直辖市与港澳必须有市级节点，否则这些地方无法开城
        for (String adcode : List.of("110100", "120100", "310100", "500100", "810100", "820100")) {
            assertThat(rows)
                    .as("直辖市/港澳的合成市级节点 %s 应存在", adcode)
                    .anyMatch(row -> row.adcode().equals(adcode) && row.level() == RegionLevel.CITY.code());
        }
    }

    @Test
    void 重复导入幂等且不新增重复行() {
        // 首次同步：库里原本只有种子区划，应插入大量新行
        RegionImportResult first = regionImportService.syncFromResource();
        assertThat(first.inserted()).isGreaterThan(3000);
        long afterFirst = regionMapper.selectCount(Wrappers.<Region>lambdaQuery());

        // 再同步一次：全部命中已有 adcode，不应再插入
        RegionImportResult second = regionImportService.syncFromResource();
        assertThat(second.inserted()).isZero();
        assertThat(regionMapper.selectCount(Wrappers.<Region>lambdaQuery())).isEqualTo(afterFirst);

        // 落库的 adcode 全局唯一
        List<Region> all = regionMapper.selectList(Wrappers.<Region>lambdaQuery());
        List<String> adcodes = all.stream()
                .map(Region::getAdcode)
                .filter(adcode -> adcode != null && !adcode.isBlank())
                .toList();
        assertThat(adcodes).doesNotHaveDuplicates();
    }

    @Test
    void 同步不覆盖已开通状态与启停() {
        // 先同步，让苏州按全国数据落库
        regionImportService.syncFromResource();
        Region suzhou = byAdcode("320500");
        assertThat(suzhou).isNotNull();

        // 模拟运营已开通苏州并停用该节点
        suzhou.setOpenStatus(RegionOpenStatus.OPEN);
        suzhou.setEnabled(Boolean.FALSE);
        regionMapper.updateById(suzhou);

        regionImportService.syncFromResource();

        Region after = byAdcode("320500");
        assertThat(after.getOpenStatus()).isEqualTo(RegionOpenStatus.OPEN);
        assertThat(after.getEnabled()).isFalse();
    }

    @Test
    void 同步补齐名称与排序但保留原slug() {
        // 种子里的扬州 slug 是 yangzhou，门户路由 /yangzhou 已在用，同步后不能改名
        regionImportService.syncFromResource();
        Region yangzhou = byAdcode("321000");
        assertThat(yangzhou).isNotNull();
        assertThat(yangzhou.getSlug()).isEqualTo("yangzhou");
        assertThat(yangzhou.getName()).isEqualTo("扬州市");
        assertThat(yangzhou.getSource()).isEqualTo(RegionSource.SEED.code());
    }

    @Test
    void 新插入行使用同步来源与代码slug() {
        regionImportService.syncFromResource();
        Region nanjing = byAdcode("320100");
        assertThat(nanjing).isNotNull();
        assertThat(nanjing.getSource()).isEqualTo(RegionSource.SYNC.code());
        assertThat(nanjing.getSlug()).isEqualTo("ad320100");
        assertThat(nanjing.getLevel()).isEqualTo(RegionLevel.CITY);
        assertThat(nanjing.getEnabled()).isTrue();
        assertThat(nanjing.getParentId()).isEqualTo(byAdcode("320000").getId());
        assertThat(nanjing.getShortName()).isEqualTo("南京");
    }

    @Test
    void 缺父级的行被拒绝() {
        List<RegionImportRow> rows = List.of(
                new RegionImportRow("320500", "苏州市", RegionLevel.CITY.code(), "999999", 100, null, null));

        assertThatThrownBy(() -> regionImportService.apply(rows))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("父级");
    }

    @Test
    void 层级不连续的行被拒绝() {
        // 区县直接挂省，缺少市这层：抓取脚本已为直辖市合成市级节点，所以这种结构不允许入库
        List<RegionImportRow> rows = List.of(
                new RegionImportRow("110000", "北京市", RegionLevel.PROVINCE.code(), null, 100, null, null),
                new RegionImportRow("110101", "东城区", RegionLevel.DISTRICT.code(), "110000", 100, null, null));

        assertThatThrownBy(() -> regionImportService.apply(rows))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("层级不连续");
    }

    @Test
    void 编码重复的行被拒绝() {
        List<RegionImportRow> rows = List.of(
                new RegionImportRow("320500", "苏州市", RegionLevel.CITY.code(), null, 100, null, null),
                new RegionImportRow("320500", "重复苏州", RegionLevel.CITY.code(), null, 200, null, null));

        assertThatThrownBy(() -> regionImportService.apply(rows))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("重复");
    }

    @Test
    void 空数据集被拒绝() {
        assertThatThrownBy(() -> regionImportService.apply(List.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("为空");
    }

    @Test
    void 简称按后缀推导() {
        assertThat(RegionImportService.shortName("江苏省")).isEqualTo("江苏");
        assertThat(RegionImportService.shortName("广西壮族自治区")).isEqualTo("广西");
        assertThat(RegionImportService.shortName("伊犁哈萨克自治州")).isEqualTo("伊犁哈萨克");
        assertThat(RegionImportService.shortName("苏州市")).isEqualTo("苏州");
        assertThat(RegionImportService.shortName("大兴安岭地区")).isEqualTo("大兴安岭");
        // 单个字但不带后缀的保持原样
        assertThat(RegionImportService.shortName("省")).isEqualTo("省");
    }

    @Test
    void 同步接口需要运营令牌并可重复调用() throws Exception {
        unauthenticated("/api/admin/regions/sync");

        String token = adminToken();
        JsonNode first = ok(call("POST", "/api/admin/regions/sync", null, token, 200));
        assertThat(first.path("inserted").asInt()).isGreaterThan(3000);
        assertThat(first.path("total").asInt()).isGreaterThan(3000);

        JsonNode second = ok(call("POST", "/api/admin/regions/sync", null, token, 200));
        assertThat(second.path("inserted").asInt()).isZero();
    }

    @Test
    void 区划树按父级分组且含开通态() throws Exception {
        regionImportService.syncFromResource();
        String token = adminToken();

        JsonNode tree = ok(call("GET", "/api/admin/regions", null, token, 200));
        JsonNode jiangsuCities = tree.path("320000");
        assertThat(jiangsuCities.isArray()).isTrue();
        assertThat(jiangsuCities.size()).isGreaterThanOrEqualTo(13);

        // 苏州市在江苏下，且未开通（open_status 为空）
        JsonNode suzhou = null;
        for (JsonNode item : jiangsuCities) {
            if ("320500".equals(item.path("adcode").asText())) {
                suzhou = item;
            }
        }
        assertThat(suzhou).isNotNull();
        assertThat(suzhou.path("name").asText()).isEqualTo("苏州市");
        assertThat(suzhou.path("level").asInt()).isEqualTo(2);
        assertThat(suzhou.path("enabled").asBoolean()).isTrue();

        // 扬州市种子迁移里已置 OPEN，开通态必须能在树里读到
        boolean yangzhouOpen = false;
        for (JsonNode item : jiangsuCities) {
            if ("321000".equals(item.path("adcode").asText()) && "OPEN".equals(item.path("openStatus").asText())) {
                yangzhouOpen = true;
            }
        }
        assertThat(yangzhouOpen).isTrue();
    }

    @Test
    void 同步后苏州可出现在开城列表() throws Exception {
        regionImportService.syncFromResource();
        String token = adminToken();

        JsonNode cities = ok(call("GET", "/api/admin/cities", null, token, 200));
        boolean hasSuzhou = false;
        for (JsonNode city : cities) {
            if ("苏州市".equals(city.path("name").asText())) {
                hasSuzhou = true;
            }
        }
        assertThat(hasSuzhou).as("同步后苏州市应出现在开城列表，运营才点得进详情页").isTrue();
    }

    private Region byAdcode(String adcode) {
        List<Region> rows = regionMapper.selectList(Wrappers.<Region>lambdaQuery().eq(Region::getAdcode, adcode));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void unauthenticated(String path) throws Exception {
        mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
    }

    private String adminToken() throws Exception {
        JsonNode body = ok(call("POST", "/api/admin/auth/login", """
                {"phone":"17606838920","password":"123456"}
                """, null, 200));
        return body.path("token").asText();
    }

    private JsonNode ok(JsonNode envelope) {
        assertThat(envelope.path("code").asInt()).isZero();
        return envelope.path("data");
    }

    private JsonNode call(String method, String path, String json, String token, int expectedStatus) throws Exception {
        var builder = switch (method) {
            case "POST" -> post(path);
            default -> get(path);
        };
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        MvcResult result = mockMvc.perform(builder).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }
}
