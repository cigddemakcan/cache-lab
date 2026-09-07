package com.cigdem.cachelab.config;

import com.cigdem.cachelab.product.Product;
import com.cigdem.cachelab.product.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DemoDataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        productRepository.saveAll(List.of(
                new Product("Mechanical Keyboard", "ACCESSORY", new BigDecimal("2499.90"), 25),
                new Product("27-inch Monitor", "DISPLAY", new BigDecimal("8999.00"), 12),
                new Product("USB-C Dock", "ACCESSORY", new BigDecimal("3199.50"), 40)
        ));
    }
}
