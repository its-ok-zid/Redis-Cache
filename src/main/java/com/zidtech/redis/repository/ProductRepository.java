package com.zidtech.redis.repository;

import com.zidtech.redis.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByIsAvailableTrue(Pageable pageable);

    List<Product> findByPriceBetweenAndIsAvailableTrue(BigDecimal minPrice, BigDecimal maxPrice);

    @Query("SELECT p FROM Product p WHERE p.isAvailable = true AND p.quantity > 0 ORDER BY p.createdAt DESC")
    List<Product> findNewArrivals();

    @Query("SELECT p FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :query, '%')) AND p.isAvailable = true")
    List<Product> searchProducts(@Param("query") String query);

    List<Product> findByCategoryIdAndIsAvailableTrue(Long categoryId);

    boolean existsBySku(String sku);
}