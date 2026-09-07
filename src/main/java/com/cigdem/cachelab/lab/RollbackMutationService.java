package com.cigdem.cachelab.lab;

import com.cigdem.cachelab.cache.CacheNames;
import com.cigdem.cachelab.cache.ProductCacheInvalidator;
import com.cigdem.cachelab.product.Product;
import com.cigdem.cachelab.product.ProductNotFoundException;
import com.cigdem.cachelab.product.ProductRepository;
import com.cigdem.cachelab.product.ProductResponse;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class RollbackMutationService {

    private final ProductRepository productRepository;
    private final CacheManager cacheManager;
    private final ProductCacheInvalidator cacheInvalidator;

    public RollbackMutationService(
            ProductRepository productRepository,
            CacheManager cacheManager,
            ProductCacheInvalidator cacheInvalidator
    ) {
        this.productRepository = productRepository;
        this.cacheManager = cacheManager;
        this.cacheInvalidator = cacheInvalidator;
    }

    @Transactional
    public void brokenUpdateCacheBeforeRollback(Long productId, BigDecimal attemptedPrice) {
        Product product = getEntity(productId);
        product.changePrice(attemptedPrice);
        productRepository.flush();

        // HATA: Transaction henuz commit olmadan cache'e yeni degeri yaziyoruz.
        // Bir sonraki exception DB'yi geri alacak ama bu Redis yazisini geri alamaz.
        rollbackCache().put(productId, ProductResponse.from(product));
        throw new IntentionalRollbackException();
    }

    @Transactional
    public void fixedEvictOnlyAfterCommit(Long productId, BigDecimal attemptedPrice) {
        Product product = getEntity(productId);
        product.changePrice(attemptedPrice);
        productRepository.flush();

        // Dogru fikir: cache islemi ancak DB transaction'i basariyla commit olursa calissin.
        cacheInvalidator.evictRollbackAfterCommit(productId);
        throw new IntentionalRollbackException();
    }

    private Product getEntity(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private Cache rollbackCache() {
        Cache cache = cacheManager.getCache(CacheNames.ROLLBACK_PRODUCT);
        if (cache == null) {
            throw new IllegalStateException("Rollback experiment cache is missing");
        }
        return cache;
    }
}
