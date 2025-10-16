package com.zidtech.redis.repository;

import com.zidtech.redis.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    /*** This method will be cached by Spring Data Redis ***/
    Optional<Product> findBySku(String sku);

    /*** Cached methods for frequently accessed queries ***/
    Optional<Product> findByCategoryUuidAndIsAvailableTrue(Long categoryUuid);

    Page<Product> findByIsAvailableTrue(Pageable pageable);

    Optional<Product> findByPriceBetweenAndIsAvailableTrue(BigDecimal minPrice, BigDecimal maxPrice);

    @Query("SELECT p FROM Product p WHERE p.isAvailable = true AND p.quantity > 0 ORDER BY p.createdAt DESC")
    Optional<Product> findNewArrivals();

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) AND p.isAvailable = true")
    Optional<Product> searchProducts(@Param("query") String query);

    /*** Method to check if product exists by SKU (useful for cache invalidation) ***/
    boolean existsBySku(String sku);
}
