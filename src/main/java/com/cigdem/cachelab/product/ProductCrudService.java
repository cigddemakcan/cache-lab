package com.cigdem.cachelab.product;

import com.cigdem.cachelab.cache.ProductCacheInvalidator;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductCrudService {

    private final ProductRepository productRepository;
    private final ProductCacheInvalidator cacheInvalidator;

    public ProductCrudService(
            ProductRepository productRepository,
            ProductCacheInvalidator cacheInvalidator
    ) {
        this.productRepository = productRepository;
        this.cacheInvalidator = cacheInvalidator;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(
                request.name().trim(),
                request.category().trim(),
                request.price(),
                request.stock()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        // Listeyi bilerek cache'lemiyoruz. Ilk hedef tekil key invalidation'ini anlamak.
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest request) {
        Product product = getEntity(productId);
        product.update(
                request.name().trim(),
                request.category().trim(),
                request.price(),
                request.stock()
        );

        // DB commit olmadan cache silmek baska bir istegin eski veriyi yeniden
        // cache'e koymasina yol acabilir. Bu nedenle invalidation after-commit.
        cacheInvalidator.evictEverywhereAfterCommit(productId);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse purchase(Long productId, int quantity) {
        Product product = getEntity(productId);
        if (product.getStock() < quantity) {
            throw new InsufficientStockException(productId, product.getStock(), quantity);
        }

        product.decreaseStock(quantity);
        cacheInvalidator.evictEverywhereAfterCommit(productId);
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long productId) {
        Product product = getEntity(productId);
        productRepository.delete(product);
        cacheInvalidator.evictEverywhereAfterCommit(productId);
    }

    private Product getEntity(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
