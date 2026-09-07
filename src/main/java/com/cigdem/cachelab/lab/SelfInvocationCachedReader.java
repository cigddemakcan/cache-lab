package com.cigdem.cachelab.lab;

import com.cigdem.cachelab.cache.CacheNames;
import com.cigdem.cachelab.product.ProductDatabaseReader;
import com.cigdem.cachelab.product.ProductResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SelfInvocationCachedReader {

    private final ProductDatabaseReader databaseReader;

    public SelfInvocationCachedReader(ProductDatabaseReader databaseReader) {
        this.databaseReader = databaseReader;
    }

    @Cacheable(cacheNames = CacheNames.SELF_INVOCATION_PRODUCT, key = "#productId")
    public ProductResponse get(Long productId) {
        return databaseReader.findById(productId);
    }
}
