package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCostCalculator {
    
    private final TrendyolProductRepository productRepository;
    
    /**
     * Calculate and set cost information for an OrderItem builder
     */
    public void setCostInfo(OrderItem.OrderItemBuilder itemBuilder, String barcode, UUID storeId, 
                           LocalDateTime orderDate, Map<String, TrendyolProduct> productCache) {
        if (barcode == null || barcode.isEmpty()) {
            return;
        }
        
        TrendyolProduct product = null;
        
        // Use cache if available, otherwise query database
        if (productCache != null) {
            product = productCache.get(barcode);
        } else {
            Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(storeId, barcode);
            product = productOpt.orElse(null);
        }
        
        if (product != null) {
            CostAndStockInfo appropriateCost = findAppropriateCostForProduct(product, orderDate.toLocalDate());
            
            if (appropriateCost != null) {
                itemBuilder.cost(appropriateCost.getUnitCost() != null ? 
                                BigDecimal.valueOf(appropriateCost.getUnitCost()) : null)
                          .costVat(appropriateCost.getCostVatRate());
                
                log.debug("Found cost {} for product {} on order date {}", 
                        appropriateCost.getUnitCost(), barcode, orderDate);
            } else {
                log.debug("No appropriate cost found for product {} on order date {}", 
                        barcode, orderDate);
            }
        } else {
            log.debug("Product not found in trendyol_products for barcode: {}", barcode);
        }
    }
    
    /**
     * Calculate and set cost information for an OrderItem builder (without cache)
     */
    public void setCostInfo(OrderItem.OrderItemBuilder itemBuilder, String barcode, UUID storeId, 
                           LocalDateTime orderDate) {
        setCostInfo(itemBuilder, barcode, storeId, orderDate, null);
    }
    
    /**
     * Find the most appropriate cost for a product on a given order date
     */
    public CostAndStockInfo findAppropriateCostForProduct(TrendyolProduct product, LocalDate orderDate) {
        if (product.getCostAndStockInfo().isEmpty()) {
            log.debug("No cost information found for product with barcode: {}", product.getBarcode());
            return null;
        }
        
        // Sort cost and stock info by date (earliest first)
        List<CostAndStockInfo> sortedCosts = product.getCostAndStockInfo().stream()
                .filter(cost -> cost.getStockDate() != null)
                .sorted((c1, c2) -> c1.getStockDate().compareTo(c2.getStockDate()))
                .collect(Collectors.toList());
        
        if (sortedCosts.isEmpty()) {
            return null;
        }
        
        return findAppropriateCost(sortedCosts, orderDate);
    }
    
    /**
     * Find the most appropriate cost for the given order date
     * Logic: Use the latest cost entry that is on or before the order date
     */
    private CostAndStockInfo findAppropriateCost(List<CostAndStockInfo> sortedCosts, LocalDate orderDate) {
        CostAndStockInfo appropriateCost = null;
        
        for (CostAndStockInfo cost : sortedCosts) {
            // If cost date is after order date, break (since list is sorted)
            if (cost.getStockDate().isAfter(orderDate)) {
                break;
            }
            // This cost is on or before the order date, so it's a candidate
            appropriateCost = cost;
        }
        
        // If no cost found before or on order date, use the first available cost
        if (appropriateCost == null && !sortedCosts.isEmpty()) {
            appropriateCost = sortedCosts.get(0);
            log.debug("No cost found before order date, using earliest available cost from: {}", 
                     appropriateCost.getStockDate());
        }
        
        return appropriateCost;
    }
}
