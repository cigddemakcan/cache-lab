package com.cigdem.cachelab.product;

import jakarta.validation.constraints.Min;

public record PurchaseRequest(@Min(1) int quantity) {
}
