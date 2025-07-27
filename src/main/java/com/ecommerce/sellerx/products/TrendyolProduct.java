package com.ecommerce.sellerx.products;

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
@Table(name = "trendyol_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @Column(name = "barcode")
    private String barcode;
    
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    
    @Column(name = "category_name")
    private String categoryName;
    
    @Column(name = "create_date_time")
    private Long createDateTime;
    
    @Column(name = "has_active_campaign")
    @Builder.Default
    private Boolean hasActiveCampaign = false;
    
    @Column(name = "brand")
    private String brand;
    
    @Column(name = "brand_id")
    private Long brandId;
    
    @Column(name = "product_main_id")
    private String productMainId;
    
    @Column(name = "image", columnDefinition = "TEXT")
    private String image;
    
    @Column(name = "product_url", columnDefinition = "TEXT")
    private String productUrl;
    
    @Column(name = "dimensional_weight", precision = 10, scale = 2)
    private BigDecimal dimensionalWeight;
    
    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;
    
    @Column(name = "vat_rate")
    private Integer vatRate;
    
    @Column(name = "quantity")
    @Builder.Default
    private Integer quantity = 0;
    
    @Type(JsonBinaryType.class)
    @Column(name = "cost_and_stock_info", columnDefinition = "jsonb")
    @Builder.Default
    private List<CostAndStockInfo> costAndStockInfo = new ArrayList<>();
    
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
