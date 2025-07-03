
package com.ecommerce.sellerx.stores;

@lombok.Data
@lombok.EqualsAndHashCode(callSuper = false)
public class TrendyolCredentials extends MarketplaceCredentials {
    private String apiKey;
    private String apiSecret;
    private Long sellerId;
    private String integrationCode;
    private String Token; // keep the capital T to match existing data
}
