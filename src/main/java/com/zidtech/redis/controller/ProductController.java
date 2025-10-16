package com.zidtech.redis.controller;

import com.zidtech.redis.entity.Product;
import com.zidtech.redis.service.ProductService;
import com.zidtech.redis.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final RedisCacheService redisCacheService;

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        log.info("GET /api/products/{}", id);
        Optional<Product> product = productService.getProductByUuid(id);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        log.info("GET /api/products/sku/{}", sku);
        Optional<Product> product = productService.getProductBySku(sku);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Optional<Product>> getProductsByCategory(@PathVariable Long categoryId) {
        log.info("GET /api/products/category/{}", categoryId);
        Optional<Product> products = productService.getProductsByCategoryId(categoryId);
        return ResponseEntity.ok(products);
    }

    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/products?page={}&size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.getActiveProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<Optional<Product>> searchProducts(@RequestParam String q) {
        log.info("GET /api/products/search?q={}", q);
        Optional<Product> products = productService.searchProducts(q);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/price-range")
    public ResponseEntity<Optional<Product>> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        log.info("GET /api/products/price-range?minPrice={}&maxPrice={}", minPrice, maxPrice);
        Optional<Product> products = productService.getProductsByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/new-arrivals")
    public ResponseEntity<Optional<Product>> getNewArrivals() {
        log.info("GET /api/products/new-arrivals");
        Optional<Product> newArrivals = productService.getNewArrivals();
        return ResponseEntity.ok(newArrivals);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        log.info("POST /api/products - Creating product: {}", product.getProductName());
        Product createdProduct = productService.createProduct(product);
        return ResponseEntity.ok(createdProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @RequestBody Product product) {
        log.info("PUT /api/products/{} - Updating product", id);
        product.setProductUuid(id);
        Product updatedProduct = productService.updateProduct(product);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id, @RequestParam Long categoryId) {
        log.info("DELETE /api/products/{}", id);
        productService.deleteProduct(id, categoryId);
        return ResponseEntity.noContent().build();
    }

    // Cache management endpoints
    @PostMapping("/cache/prime")
    public ResponseEntity<String> primeCache() {
        log.info("POST /api/products/cache/prime - Priming cache");
        productService.primeProductCache();
        return ResponseEntity.ok("Cache priming completed");
    }

    @DeleteMapping("/cache/clear")
    public ResponseEntity<String> clearCache() {
        log.info("DELETE /api/products/cache/clear - Clearing cache");
        redisCacheService.clearAllCaches();
        return ResponseEntity.ok("All caches cleared");
    }

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        log.info("GET /api/products/cache/stats");

        Map<String, Object> stats = Map.of(
                "totalProductKeys", redisCacheService.getCacheSize("product:*"),
                "totalCategoryKeys", redisCacheService.getCacheSize("products:category:*"),
                "totalPageKeys", redisCacheService.getCacheSize("products:page:*"),
                "totalSearchKeys", redisCacheService.getCacheSize("products:search:*")
        );

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/cache/refresh")
    public ResponseEntity<String> refreshProductCache(@PathVariable UUID id) {
        log.info("POST /api/products/{}/cache/refresh", id);
        productService.refreshProductCache(id);
        return ResponseEntity.ok("Product cache refreshed");
    }
}

