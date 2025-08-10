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
        
        // Determine status based on transaction type
        String status = determineStatus(apiItem.getTransactionType());
        
        // For discount and coupon, use simplified mapping
        if ("İndirim".equals(apiItem.getTransactionType()) || "Discount".equals(apiItem.getTransactionType()) ||
            "Kupon".equals(apiItem.getTransactionType()) || "Coupon".equals(apiItem.getTransactionType())) {
            
            return OrderItemSettlement.builder()
                    .id(apiItem.getId())
                    .transactionType(apiItem.getTransactionType())
                    .status(status)
                    .debt(apiItem.getDebt())
                    .credit(apiItem.getCredit())
                    .commissionRate(apiItem.getCommissionRate())
                    .commissionAmount(apiItem.getCommissionAmount())
                    .sellerRevenue(apiItem.getSellerRevenue())
                    .build();
        }
        
        // For sales and returns, use full mapping
        return OrderItemSettlement.builder()
                .id(apiItem.getId())
                .barcode(apiItem.getBarcode())
                .transactionType(apiItem.getTransactionType())
                .status(status)
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
    
    /**
     * Determine status based on transaction type
     */
    private String determineStatus(String transactionType) {
        if ("Satış".equals(transactionType) || "Sale".equals(transactionType)) {
            return "SOLD";
        } else if ("İade".equals(transactionType) || "Return".equals(transactionType)) {
            return "RETURNED";
        } else if ("İptal".equals(transactionType) || "Cancel".equals(transactionType)) {
            return "CANCELLED";
        } else if ("İndirim".equals(transactionType) || "Discount".equals(transactionType)) {
            return "DISCOUNT";
        } else if ("Kupon".equals(transactionType) || "Coupon".equals(transactionType)) {
            return "COUPON";
        } else {
            return "UNKNOWN";
        }
    }
}
