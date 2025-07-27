package com.ecommerce.sellerx.products;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
