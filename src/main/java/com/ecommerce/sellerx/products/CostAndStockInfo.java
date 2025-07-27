package com.ecommerce.sellerx.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CostAndStockInfo {
    private Integer quantity;
    private Double unitCost;
    private Integer costVatRate;
    private LocalDateTime stockDate;
}
