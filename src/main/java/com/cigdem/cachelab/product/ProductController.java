package com.cigdem.cachelab.product;

import com.cigdem.cachelab.cache.DeclarativeProductCacheService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCrudService productCrudService;
    private final DeclarativeProductCacheService declarativeCacheService;

    public ProductController(
            ProductCrudService productCrudService,
            DeclarativeProductCacheService declarativeCacheService
    ) {
        this.productCrudService = productCrudService;
        this.declarativeCacheService = declarativeCacheService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productCrudService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<ProductResponse> findAll() {
        return productCrudService.findAll();
    }

    @GetMapping("/{productId}")
    public ProductResponse findById(@PathVariable Long productId) {
        return declarativeCacheService.get(productId);
    }

    @PutMapping("/{productId}")
    public ProductResponse update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request
    ) {
        return productCrudService.update(productId, request);
    }

    @PostMapping("/{productId}/purchase")
    public ProductResponse purchase(
            @PathVariable Long productId,
            @Valid @RequestBody PurchaseRequest request
    ) {
        return productCrudService.purchase(productId, request.quantity());
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable Long productId) {
        productCrudService.delete(productId);
        return ResponseEntity.noContent().build();
    }
}
