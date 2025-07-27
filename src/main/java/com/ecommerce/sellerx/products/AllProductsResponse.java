package com.ecommerce.sellerx.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllProductsResponse {
    private List<TrendyolProductDto> products;
    private Integer totalCount;
    private String message;
}
