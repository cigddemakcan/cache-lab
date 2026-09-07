package com.cigdem.cachelab.config;

import com.cigdem.cachelab.cache.LoggingCacheErrorHandler;
import com.cigdem.cachelab.cache.SpringCacheFailureTracker;
import com.cigdem.cachelab.product.ProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private final SpringCacheFailureTracker failureTracker;

    public CacheConfig(SpringCacheFailureTracker failureTracker) {
        this.failureTracker = failureTracker;
    }

    @Bean
    public RedisSerializer<ProductResponse> productRedisSerializer(ObjectMapper objectMapper) {
        // JSON secmemizin nedeni redis-cli ile degerleri insan tarafindan okunabilir tutmak.
        return new Jackson2JsonRedisSerializer<>(objectMapper.copy(), ProductResponse.class);
    }

    @Bean
    public RedisTemplate<String, ProductResponse> productRedisTemplate(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<ProductResponse> productRedisSerializer
    ) {
        RedisTemplate<String, ProductResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(productRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(productRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<ProductResponse> productRedisSerializer,
            @Value("${cachelab.cache.product-ttl}") Duration productTtl
    ) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(productTtl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(productRedisSerializer));

        // transactionAware() bilerek kullanilmiyor. Rollback laboratuvarinda erken
        // cache yaziminin nasil stale veri uretebildigini gorecegiz.
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        // Redis gecici olarak kapanirsa @Cacheable metodu yine calisir ve DB'ye duser.
        return new LoggingCacheErrorHandler(failureTracker);
    }
}
