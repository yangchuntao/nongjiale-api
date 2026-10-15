package com.suzhong.njl.service.support;

public final class TestDataMark {

    public static final String LABEL = "测试";

    private TestDataMark() {
    }

    public static String label(Boolean testData) {
        return Boolean.TRUE.equals(testData) ? LABEL : null;
    }
}
