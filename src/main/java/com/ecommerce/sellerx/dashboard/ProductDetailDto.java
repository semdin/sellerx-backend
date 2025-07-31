package com.ecommerce.sellerx.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailDto {
    
    private String productName;
    private String barcode; // ürün barkodu
    private Integer totalSoldQuantity; // brüt satış adedi
    private Integer returnQuantity; // iade adedi
    private BigDecimal revenue; // ciro
    private BigDecimal grossProfit; // brüt kar
}
