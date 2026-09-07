package com.cigdem.cachelab.product;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int available, int requested) {
        super("Insufficient stock for product %d. Available: %d, requested: %d"
                .formatted(productId, available, requested));
    }
}
