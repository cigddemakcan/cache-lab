package com.cigdem.cachelab.cache;

public final class CacheNames {

    public static final String MANUAL_PRODUCT_PREFIX = "lab:manual:product:v1:";
    public static final String DECLARATIVE_PRODUCT = "lab:declarative:product:v1";
    public static final String SELF_INVOCATION_PRODUCT = "lab:self-invocation:product:v1";
    public static final String ROLLBACK_PRODUCT = "lab:rollback:product:v1";

    private CacheNames() {
    }

    public static String manualProductKey(Long productId) {
        return MANUAL_PRODUCT_PREFIX + productId;
    }

    public static String springCacheKey(String cacheName, Long productId) {
        return cacheName + "::" + productId;
    }
}
