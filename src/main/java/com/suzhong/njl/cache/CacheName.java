package com.suzhong.njl.cache;

public enum CacheName {
    HOME_FEED("home-feed"),
    STORE_DETAIL("store-detail"),
    REGION("region"),
    SITE_CONFIG("site-config");

    private final String key;

    CacheName(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
