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

    @Cacheable(key = "'product': + #id")
    @Transactional(readOnly = true)
    public Optional<Product> getProductByUuid(UUID id) {
        log.info("Fetching product with UUID: {} from database", id);
        return productRepository.findById(id);
    }

    @Cacheable(key = "'product_sku': + #sku")
    @Transactional(readOnly = true)
    public Optional<Product> getProductBySku(String sku) {
        log.info("Fetching product with SKU: {} from database", sku);
        return productRepository.findBySku(sku);
    }

    @Cacheable(key = "'products:category:' + #categoryId")
    @Transactional(readOnly = true)
    public Optional<Product> getProductsByCategoryId(Long categoryId) {
        log.info("Fetching products for category ID: {} from database", categoryId);
        return productRepository.findByCategoryUuidAndIsAvailableTrue(categoryId);
    }

    @CachePut(key = "'product': + #result.id")
    @CacheEvict(key = "'products:category:' + #result.categoryId", beforeInvocation = false)
    @Transactional
    public Product createProduct(Product product) {
        log.info("Creating new product: {}", product.getProductName());
        Product savedProduct = productRepository.save(product);

        // Manual cache update for related caches
        redisCacheService.evictPattern("products:page:*");
        redisCacheService.evictPattern("products:search:*");

        return savedProduct;
    }


    // CacheEvict - Remove from cache when updating
    @Caching(evict = {
            @CacheEvict(key = "'product:' + #product.id"),
            @CacheEvict(key = "'product:sku:' + #product.sku"),
            @CacheEvict(key = "'products:category:' + #product.categoryId")
    })
    @Transactional
    public Product updateProduct(Product product) {
        log.info("Updating product with ID: {}", product.getProductUuid());
        Product updatedProduct = productRepository.save(product);

        // Evict pattern-based caches
        redisCacheService.evictPattern("products:page:*");
        redisCacheService.evictPattern("products:search:*");

        return updatedProduct;
    }

    // Complex eviction pattern for multiple caches
    @Caching(evict = {
            @CacheEvict(key = "'product:' + #id"),
            @CacheEvict(key = "'products:category:' + #categoryId")
    })
    @Transactional
    public void deleteProduct(UUID id, Long categoryId) {
        log.info("Deleting product with ID: {}", id);
        productRepository.deleteById(id);

        // Evict additional caches
        redisCacheService.evictPattern("products:page:*");
        redisCacheService.evictPattern("products:search:*");
        redisCacheService.evictKey("product:sku:*"); // Pattern eviction for SKU
    }

    // Paginated results with caching
    @Cacheable(key = "'products:page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> getActiveProducts(Pageable pageable) {
        log.info("Fetching paginated products from database - Page: {}, Size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findByIsAvailableTrue(pageable);
    }

    // Price range search with caching
    @Cacheable(key = "'products:price:min:' + #minPrice + ':max:' + #maxPrice")
    @Transactional(readOnly = true)
    public Optional<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.info("Fetching products by price range: {} - {}", minPrice, maxPrice);
        return productRepository.findByPriceBetweenAndIsAvailableTrue(minPrice, maxPrice);
    }

    // Search functionality with caching
    @Cacheable(key = "'products:search:' + #query.hashCode()")
    @Transactional(readOnly = true)
    public Optional<Product> searchProducts(String query) {
        log.info("Searching products with query: {}", query);
        return productRepository.searchProducts(query);
    }

    // New arrivals with caching
    @Cacheable(key = "'products:new-arrivals'")
    @Transactional(readOnly = true)
    public Optional<Product> getNewArrivals() {
        log.info("Fetching new arrivals from database");
        return productRepository.findNewArrivals();
    }

    // Manual cache management example
    public void refreshProductCache(UUID productId) {
        log.info("Refreshing cache for product ID: {}", productId);
        productRepository.findById(productId).ifPresent(product -> {
            redisCacheService.cacheValue("product:" + productId, product);
            redisCacheService.cacheValue("product:sku:" + product.getSku(), product);
        });
    }

    // Bulk cache loading - Priming the cache
    @Transactional(readOnly = true)
    public void primeProductCache() {
        log.info("Starting cache priming for products");

        // Cache top 100 active products
        List<Product> topProducts = productRepository.findByIsAvailableTrue(
                org.springframework.data.domain.PageRequest.of(0, 100)
        ).getContent();

        for (Product product : topProducts) {
            redisCacheService.cacheValue("product:" + product.getProductUuid(), product);
            redisCacheService.cacheValue("product:sku:" + product.getSku(), product);
        }

        // Cache products by categories
        for (long categoryId = 1; categoryId <= 8; categoryId++) {
            Optional<Product> categoryProducts = productRepository.findByCategoryUuidAndIsAvailableTrue(categoryId);
            redisCacheService.cacheValue("products:category:" + categoryId, categoryProducts);
        }

        // Cache new arrivals
        Optional<Product> newArrivals = productRepository.findNewArrivals();
        redisCacheService.cacheValue("products:new-arrivals", newArrivals);

        log.info("Cache priming completed. Cached {} products", topProducts.size());
    }
}


