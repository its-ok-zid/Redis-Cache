package com.zidtech.redis.service;

import com.zidtech.redis.entity.Product;
import com.zidtech.redis.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "products")
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisCacheService redisCacheService;

    @Cacheable(key = "'product:' + #id")
    @Transactional(readOnly = true)
    public Optional<Product> getProductByUuid(UUID id) {
        log.info("Fetching product with UUID: {} from database", id);
        return productRepository.findById(id);
    }

    @Cacheable(key = "'products:page:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> listAvailableProducts(Pageable pageable) {
        return productRepository.findByIsAvailableTrue(pageable);
    }

    @Transactional
    @CachePut(key = "'product:' + #product.productUuid")
    public Product createOrUpdateProduct(Product product) {
        Product saved = productRepository.save(product);
        // update manual caches if needed
        redisCacheService.cacheValue("products:latest", saved);
        return saved;
    }

    @Transactional
    @CacheEvict(key = "'product:' + #id")
    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
    }

    public List<Product> searchProducts(String q) {
        return productRepository.searchProducts(q);
    }

    /** Example cache priming */
    public void primeCaches() {
        List<Product> topProducts = productRepository.findNewArrivals();
        redisCacheService.cacheValue("products:top", topProducts);

        // categories
        for (long categoryId = 1; categoryId <= 8; categoryId++) {
            List<Product> categoryProducts = productRepository.findByCategoryIdAndIsAvailableTrue(categoryId);
            redisCacheService.cacheValue("products:category:" + categoryId, categoryProducts);
        }

        List<Product> newArrivals = productRepository.findNewArrivals();
        redisCacheService.cacheValue("products:new-arrivals", newArrivals);

        log.info("Cache priming completed. Cached {} products", topProducts.size());
    }
}