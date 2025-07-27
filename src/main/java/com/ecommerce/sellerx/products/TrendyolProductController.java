package com.ecommerce.sellerx.products;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class TrendyolProductController {
    
    private final TrendyolProductService trendyolProductService;
    
    @PostMapping("/sync/{storeId}")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<SyncProductsResponse> syncProductsFromTrendyol(@PathVariable UUID storeId) {
        SyncProductsResponse response = trendyolProductService.syncProductsFromTrendyol(storeId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/store/{storeId}")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<List<TrendyolProductDto>> getProductsByStore(@PathVariable UUID storeId) {
        List<TrendyolProductDto> products = trendyolProductService.getProductsByStore(storeId);
        return ResponseEntity.ok(products);
    }
    
    @PutMapping("/{productId}/cost-and-stock")
    @PreAuthorize("@userSecurityRules.canAccessProduct(authentication, #productId)")
    public ResponseEntity<TrendyolProductDto> updateCostAndStock(
            @PathVariable UUID productId,
            @RequestBody UpdateCostAndStockRequest request) {
        TrendyolProductDto updatedProduct = trendyolProductService.updateCostAndStock(productId, request);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @PostMapping("/{productId}/stock-info")
    @PreAuthorize("@userSecurityRules.canAccessProduct(authentication, #productId)")
    public ResponseEntity<TrendyolProductDto> addStockInfo(
            @PathVariable UUID productId,
            @RequestBody AddStockInfoRequest request) {
        TrendyolProductDto updatedProduct = trendyolProductService.addStockInfo(productId, request);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @PutMapping("/{productId}/stock-info/{stockDate}")
    @PreAuthorize("@userSecurityRules.canAccessProduct(authentication, #productId)")
    public ResponseEntity<TrendyolProductDto> updateStockInfoByDate(
            @PathVariable UUID productId,
            @PathVariable LocalDate stockDate,
            @RequestBody UpdateStockInfoRequest request) {
        TrendyolProductDto updatedProduct = trendyolProductService.updateStockInfoByDate(productId, stockDate, request);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @DeleteMapping("/{productId}/stock-info/{stockDate}")
    @PreAuthorize("@userSecurityRules.canAccessProduct(authentication, #productId)")
    public ResponseEntity<TrendyolProductDto> deleteStockInfoByDate(
            @PathVariable UUID productId,
            @PathVariable LocalDate stockDate) {
        TrendyolProductDto updatedProduct = trendyolProductService.deleteStockInfoByDate(productId, stockDate);
        return ResponseEntity.ok(updatedProduct);
    }
}
