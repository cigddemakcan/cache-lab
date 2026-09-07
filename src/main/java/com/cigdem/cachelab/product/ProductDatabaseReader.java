package com.cigdem.cachelab.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

@Service
public class ProductDatabaseReader {

    private static final Logger log = LoggerFactory.getLogger(ProductDatabaseReader.class);

    private final ProductRepository productRepository;
    private final Duration simulatedDelay;
    private final LongAdder databaseReads = new LongAdder();

    public ProductDatabaseReader(
            ProductRepository productRepository,
            @Value("${cachelab.simulated-db-delay}") Duration simulatedDelay
    ) {
        this.productRepository = productRepository;
        this.simulatedDelay = simulatedDelay;
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long productId) {
        databaseReads.increment();
        simulateSlowQuery();
        log.info("DATABASE QUERY -> productId={}", productId);

        return productRepository.findById(productId)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        databaseReads.increment();
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public long readCount() {
        return databaseReads.sum();
    }

    public void resetReadCount() {
        databaseReads.reset();
    }

    private void simulateSlowQuery() {
        if (simulatedDelay.isZero() || simulatedDelay.isNegative()) {
            return;
        }

        try {
            Thread.sleep(simulatedDelay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulated database wait was interrupted", exception);
        }
    }
}
