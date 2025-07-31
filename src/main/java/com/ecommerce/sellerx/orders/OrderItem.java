package com.ecommerce.sellerx.orders;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    
    @JsonProperty("barcode")
    private String barcode;
    
    @JsonProperty("productName")
    private String productName;
    
    @JsonProperty("quantity")
    private Integer quantity;
    
    @JsonProperty("unitPriceOrder")
    private BigDecimal unitPriceOrder; // This is "amount" from Trendyol API
    
    @JsonProperty("unitPriceDiscount")
    private BigDecimal unitPriceDiscount; // This is "discount" from Trendyol API
    
    @JsonProperty("unitPriceTyDiscount")
    private BigDecimal unitPriceTyDiscount; // This is "tyDiscount" from Trendyol API
    
    @JsonProperty("vatBaseAmount")
    private BigDecimal vatBaseAmount;
    
    @JsonProperty("price")
    private BigDecimal price; // This is the actual price after discounts
    
    // Additional fields that we'll get from our trendyol_products table
    @JsonProperty("cost")
    private BigDecimal cost; // Product cost from our system
    
    @JsonProperty("costVat")
    private Integer costVat; // VAT rate for cost calculation
    
    @JsonProperty("stockDate")
    private LocalDate stockDate; // Which stock date this order item was sourced from
}
