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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final RedisCacheService redisCacheService;

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable UUID id) {
        return productService.getProductByUuid(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<Product>> listProducts(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> p = productService.listAvailableProducts(pageable);
        return ResponseEntity.ok(p);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(@RequestParam String q) {
        List<Product> result = productService.searchProducts(q);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProductKeys", redisCacheService.getCacheSize("products:*"));
        stats.put("totalSearchKeys", redisCacheService.getCacheSize("products:search:*"));
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/cache/refresh")
    public ResponseEntity<String> refreshProductCache(@PathVariable UUID id) {
        log.info("POST /api/products/{}/cache/refresh", id);
        // simple evict and let next GET repopulate via @Cacheable
        redisCacheService.evictKey("product:" + id);
        return ResponseEntity.ok("Product cache refreshed");
    }
}