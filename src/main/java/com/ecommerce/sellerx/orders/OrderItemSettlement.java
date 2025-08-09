package com.ecommerce.sellerx.orders;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemSettlement {
    
    @JsonProperty("id")
    private String id; // Settlement transaction ID from Trendyol
    
    @JsonProperty("barcode")
    private String barcode; // Product barcode (for matching)
    
    @JsonProperty("transactionType")
    private String transactionType; // "Satış", "İade", etc.
    
    @JsonProperty("status") 
    private String status; // "SOLD", "RETURNED", "CANCELLED"
    
    @JsonProperty("receiptId")
    private Long receiptId; // Receipt ID from settlement
    
    @JsonProperty("debt")
    private BigDecimal debt; // Debt amount
    
    @JsonProperty("credit")
    private BigDecimal credit; // Credit amount (actual received amount)
    
    @JsonProperty("paymentPeriod")
    private Integer paymentPeriod; // Payment period in days
    
    @JsonProperty("commissionRate")
    private BigDecimal commissionRate; // ACTUAL commission rate from settlement
    
    @JsonProperty("commissionAmount")
    private BigDecimal commissionAmount; // ACTUAL commission amount
    
    @JsonProperty("commissionInvoiceSerialNumber")
    private String commissionInvoiceSerialNumber; // Commission invoice serial
    
    @JsonProperty("sellerRevenue")
    private BigDecimal sellerRevenue; // Net seller revenue after commission
    
    @JsonProperty("paymentOrderId")
    private Long paymentOrderId; // Payment order ID
    
    @JsonProperty("country")
    private String country; // Country from settlement
    
    @JsonProperty("shipmentPackageId")
    private Long shipmentPackageId; // Shipment package ID - used for matching
}
