package com.suzhong.njl.service.support;

/**
 * 抓取/导入房源的「测试」标记。
 *
 * <p>规则来自开城设计与 README：{@code store.test_data=true} 的门店仍可出现在已开通市的
 * 门户列表和详情，但不计入开城供给门槛，并在对客字段上带 {@code testTag}。
 */
public final class TestDataMark {

    /** 对客展示的测试标记文案，非测试数据时返回 null 而不是空串。 */
    public static final String LABEL = "测试";

    private TestDataMark() {
    }

    /**
     * 把 {@code test_data} 布尔值转成对客标记。
     *
     * @param testData 是否测试数据，null 按 false 处理
     * @return 测试数据返回「测试」，否则返回 null
     */
    public static String label(Boolean testData) {
        return Boolean.TRUE.equals(testData) ? LABEL : null;
    }
}
