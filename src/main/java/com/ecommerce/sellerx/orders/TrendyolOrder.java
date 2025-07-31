package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.stores.Store;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trendyol_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @Column(name = "ty_order_number", nullable = false)
    private String tyOrderNumber;
    
    @Column(name = "package_no", nullable = false)
    private Long packageNo; // This is the "id" field from Trendyol API
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate; // Converted from originShipmentDate milliseconds
    
    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;
    
    @Column(name = "total_discount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    
    @Column(name = "total_ty_discount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalTyDiscount = BigDecimal.ZERO;
    
    @Type(JsonBinaryType.class)
    @Column(name = "order_items", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
    
    @Column(name = "shipment_package_status")
    private String shipmentPackageStatus;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "total_price", precision = 19, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(name = "cargo_deci")
    @Builder.Default
    private Integer cargoDeci = 0;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
