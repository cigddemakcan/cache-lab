package com.cigdem.cachelab.cache;

import com.cigdem.cachelab.product.ProductResponse;

public record CacheLookupResult(ProductResponse product, String source) {
}
