package com.ecommerce.sellerx.products;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrendyolProductRepository extends JpaRepository<TrendyolProduct, UUID> {
    
    List<TrendyolProduct> findByStoreId(UUID storeId);
    
    Optional<TrendyolProduct> findByStoreIdAndProductId(UUID storeId, String productId);
    
    @Query("SELECT tp FROM TrendyolProduct tp WHERE tp.store.id = :storeId AND tp.barcode = :barcode")
    Optional<TrendyolProduct> findByStoreIdAndBarcode(@Param("storeId") UUID storeId, @Param("barcode") String barcode);
    
    boolean existsByStoreIdAndProductId(UUID storeId, String productId);
    
    long countByStoreId(UUID storeId);
}
