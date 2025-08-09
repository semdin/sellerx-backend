package com.ecommerce.sellerx.orders;

import org.springframework.stereotype.Component;

@Component
public class TrendyolSettlementMapper {
    
    /**
     * Maps Trendyol API settlement item to our internal OrderItemSettlement DTO
     */
    public OrderItemSettlement mapToOrderItemSettlement(TrendyolSettlementItem apiItem) {
        if (apiItem == null) {
            return null;
        }
        
        return OrderItemSettlement.builder()
                .id(apiItem.getId())
                .barcode(apiItem.getBarcode())
                .transactionType(apiItem.getTransactionType())
                .receiptId(apiItem.getReceiptId())
                .debt(apiItem.getDebt())
                .credit(apiItem.getCredit())
                .paymentPeriod(apiItem.getPaymentPeriod())
                .commissionRate(apiItem.getCommissionRate())
                .commissionAmount(apiItem.getCommissionAmount())
                .commissionInvoiceSerialNumber(apiItem.getCommissionInvoiceSerialNumber())
                .sellerRevenue(apiItem.getSellerRevenue())
                .paymentOrderId(apiItem.getPaymentOrderId())
                .country(apiItem.getCountry())
                .shipmentPackageId(apiItem.getShipmentPackageId())
                .build();
    }
}
