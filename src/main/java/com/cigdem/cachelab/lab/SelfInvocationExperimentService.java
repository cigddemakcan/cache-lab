package com.cigdem.cachelab.lab;

import com.cigdem.cachelab.cache.CacheNames;
import com.cigdem.cachelab.cache.ProductCacheInvalidator;
import com.cigdem.cachelab.product.ProductDatabaseReader;
import com.cigdem.cachelab.product.ProductResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class SelfInvocationExperimentService {

    private final ProductDatabaseReader databaseReader;
    private final ProductCacheInvalidator cacheInvalidator;
    private final SelfInvocationCachedReader externalCachedReader;

    public SelfInvocationExperimentService(
            ProductDatabaseReader databaseReader,
            ProductCacheInvalidator cacheInvalidator,
            SelfInvocationCachedReader externalCachedReader
    ) {
        this.databaseReader = databaseReader;
        this.cacheInvalidator = cacheInvalidator;
        this.externalCachedReader = externalCachedReader;
    }

    public LabResponses.SelfInvocationExperiment broken(Long productId) {
        cacheInvalidator.evictSelfInvocationNow(productId);
        long readsBefore = databaseReader.readCount();

        Measurement first = measure(() -> cachedMethodOnThisClass(productId));
        Measurement second = measure(() -> cachedMethodOnThisClass(productId));

        return new LabResponses.SelfInvocationExperiment(
                "BROKEN_SELF_INVOCATION",
                second.product(),
                first.elapsedMicroseconds(),
                second.elapsedMicroseconds(),
                databaseReader.readCount() - readsBefore,
                "Ayni bean icindeki this.cachedMethodOnThisClass(...) cagrisi Spring proxy'sini "
                        + "atladigi icin @Cacheable iki cagrida da calismadi."
        );
    }

    public LabResponses.SelfInvocationExperiment fixed(Long productId) {
        cacheInvalidator.evictSelfInvocationNow(productId);
        long readsBefore = databaseReader.readCount();

        Measurement first = measure(() -> externalCachedReader.get(productId));
        Measurement second = measure(() -> externalCachedReader.get(productId));

        return new LabResponses.SelfInvocationExperiment(
                "FIXED_SEPARATE_BEAN",
                second.product(),
                first.elapsedMicroseconds(),
                second.elapsedMicroseconds(),
                databaseReader.readCount() - readsBefore,
                "Cache'li metot ayri bir bean uzerinden cagrildigi icin proxy devrede. "
                        + "Ilk cagri DB, ikinci cagri Redis'tir."
        );
    }

    @Cacheable(cacheNames = CacheNames.SELF_INVOCATION_PRODUCT, key = "#productId")
    public ProductResponse cachedMethodOnThisClass(Long productId) {
        return databaseReader.findById(productId);
    }

    private Measurement measure(Supplier<ProductResponse> action) {
        long startedAt = System.nanoTime();
        ProductResponse product = action.get();
        return new Measurement(product, (System.nanoTime() - startedAt) / 1_000);
    }

    private record Measurement(ProductResponse product, long elapsedMicroseconds) {
    }
}
