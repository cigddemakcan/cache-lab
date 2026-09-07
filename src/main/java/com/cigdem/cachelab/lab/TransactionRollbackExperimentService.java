package com.cigdem.cachelab.lab;

import com.cigdem.cachelab.cache.ProductCacheInvalidator;
import com.cigdem.cachelab.product.ProductNotFoundException;
import com.cigdem.cachelab.product.ProductRepository;
import com.cigdem.cachelab.product.ProductResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionRollbackExperimentService {

    private final ProductRepository productRepository;
    private final RollbackCachedReader cachedReader;
    private final RollbackMutationService mutationService;
    private final ProductCacheInvalidator cacheInvalidator;

    public TransactionRollbackExperimentService(
            ProductRepository productRepository,
            RollbackCachedReader cachedReader,
            RollbackMutationService mutationService,
            ProductCacheInvalidator cacheInvalidator
    ) {
        this.productRepository = productRepository;
        this.cachedReader = cachedReader;
        this.mutationService = mutationService;
        this.cacheInvalidator = cacheInvalidator;
    }

    public LabResponses.TransactionRollbackExperiment broken(Long productId, BigDecimal attemptedPrice) {
        ExperimentContext context = prepare(productId);

        try {
            mutationService.brokenUpdateCacheBeforeRollback(productId, attemptedPrice);
        } catch (IntentionalRollbackException ignored) {
            // Exception mutation bean'inden disari cikti; Spring bu noktadan once rollback yapti.
        }

        return result("BROKEN_CACHE_WRITE_BEFORE_COMMIT", context, attemptedPrice,
                "DB rollback ile eski fiyatina dondu; Redis transaction disinda kaldigi icin "
                        + "denenen yeni fiyati tutuyor.");
    }

    public LabResponses.TransactionRollbackExperiment fixed(Long productId, BigDecimal attemptedPrice) {
        ExperimentContext context = prepare(productId);

        try {
            mutationService.fixedEvictOnlyAfterCommit(productId, attemptedPrice);
        } catch (IntentionalRollbackException ignored) {
            // Rollback sonrasi afterCommit callback calismaz.
        }

        return result("FIXED_AFTER_COMMIT_INVALIDATION", context, attemptedPrice,
                "Transaction commit olmadigi icin cache'e dokunulmadi. DB ve Redis eski, "
                        + "birbiriyle tutarli fiyati tasiyor.");
    }

    private ExperimentContext prepare(Long productId) {
        cacheInvalidator.evictRollbackNow(productId);
        ProductResponse original = cachedReader.get(productId); // Eski degerle cache'i isit.
        return new ExperimentContext(productId, original.price());
    }

    private LabResponses.TransactionRollbackExperiment result(
            String mode,
            ExperimentContext context,
            BigDecimal attemptedPrice,
            String explanation
    ) {
        ProductResponse databaseProduct = productRepository.findById(context.productId())
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(context.productId()));
        ProductResponse cachedProduct = cachedReader.get(context.productId());
        boolean stale = databaseProduct.price().compareTo(cachedProduct.price()) != 0;

        return new LabResponses.TransactionRollbackExperiment(
                mode,
                context.productId(),
                context.originalPrice(),
                attemptedPrice,
                databaseProduct.price(),
                cachedProduct.price(),
                stale,
                explanation
        );
    }

    private record ExperimentContext(Long productId, BigDecimal originalPrice) {
    }
}
