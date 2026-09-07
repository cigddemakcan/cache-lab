package com.cigdem.cachelab.lab;

import com.cigdem.cachelab.cache.CacheNames;
import com.cigdem.cachelab.product.ProductResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CacheTtlInspector {

    private final RedisTemplate<String, ProductResponse> redisTemplate;

    public CacheTtlInspector(RedisTemplate<String, ProductResponse> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public LabResponses.TtlInspection inspect(Long productId) {
        String manualKey = CacheNames.manualProductKey(productId);
        String declarativeKey = CacheNames.springCacheKey(CacheNames.DECLARATIVE_PRODUCT, productId);

        return new LabResponses.TtlInspection(
                productId,
                inspectKey(manualKey),
                inspectKey(declarativeKey)
        );
    }

    private LabResponses.TtlEntry inspectKey(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return new LabResponses.TtlEntry(key, ttl, ttlState(ttl));
        } catch (RuntimeException exception) {
            return new LabResponses.TtlEntry(key, null,
                    "REDIS_UNAVAILABLE: " + exception.getClass().getSimpleName());
        }
    }

    private String ttlState(Long ttl) {
        if (ttl == null) {
            return "UNKNOWN";
        }
        if (ttl == -2) {
            return "KEY_DOES_NOT_EXIST";
        }
        if (ttl == -1) {
            return "NO_EXPIRATION";
        }
        return "EXPIRES_IN_SECONDS";
    }
}
