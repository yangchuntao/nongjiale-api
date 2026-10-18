package com.suzhong.njl.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.suzhong.njl.domain.enums.BannerMessage;
import com.suzhong.njl.domain.enums.BannerScope;
import com.suzhong.njl.domain.enums.IntentStatus;

class BannerMessageTest {

    @Test
    void messagesKeepTheContractCopy() {
        assertThat(BannerMessage.IMAGE_REQUIRED.text()).isEqualTo("请填写图片地址");
        assertThat(BannerMessage.IMAGE_TOO_LONG.text()).isEqualTo("图片地址过长");
        assertThat(BannerMessage.IMAGE_SCHEME.text()).isEqualTo("图片地址须为 http 或 https");
        assertThat(BannerMessage.LINK_REQUIRED.text()).isEqualTo("请填写跳转链接");
        assertThat(BannerMessage.LINK_TOO_LONG.text()).isEqualTo("跳转链接过长");
        assertThat(BannerMessage.LINK_INVALID.text()).isEqualTo("跳转链接须为站内路径或 http(s) 外链");
        assertThat(BannerMessage.SCOPE_REQUIRED.text()).isEqualTo("请选择投放范围");
        assertThat(BannerMessage.ALL_WITH_CITY.text()).isEqualTo("全站投放不能指定市");
        assertThat(BannerMessage.CITY_REQUIRED.text()).isEqualTo("请选择已开通的市");
        assertThat(BannerMessage.CITY_NOT_OPEN.text()).isEqualTo("投放市未开通，请改选全站或其他已开通市");
        assertThat(BannerMessage.REGION_DISABLED.text()).isEqualTo("地区不可用");
        assertThat(BannerMessage.SORT_INVALID.text()).isEqualTo("请输入整数排序值");
        assertThat(BannerMessage.TITLE_TOO_LONG.text()).isEqualTo("标题过长");
        assertThat(BannerMessage.ENABLED_REQUIRED.text()).isEqualTo("请指定是否启用");
        assertThat(BannerMessage.NOT_FOUND.text()).isEqualTo("Banner 不存在");
        assertThat(BannerMessage.CITY_NOT_FOUND.text()).isEqualTo("城市不存在");
        assertThat(BannerMessage.ENABLE_CITY_CLOSED.text()).isEqualTo("投放市未开通，请改选全站或其他已开通市");
        assertThat(BannerMessage.CITY_NOT_OPEN.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(BannerMessage.ENABLE_CITY_CLOSED.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(BannerMessage.NOT_FOUND.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(BannerMessage.NOT_FOUND.toException()).hasMessage("Banner 不存在");
        assertThat(BannerScope.ALL.code()).isEqualTo("ALL");
        assertThat(BannerScope.CITY.code()).isEqualTo("CITY");
        assertThat(IntentStatus.CHECKED_IN.label()).isEqualTo("已入住");
    }
}
