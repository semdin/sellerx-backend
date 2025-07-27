package com.ecommerce.sellerx.products;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.StoreNotFoundException;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolProductService {
    
    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    
    private final TrendyolProductRepository trendyolProductRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductMapper productMapper;
    private final RestTemplate restTemplate;
    
    public SyncProductsResponse syncProductsFromTrendyol(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException("Store not found"));
        
        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null) {
            return new SyncProductsResponse(false, "Trendyol credentials not found", 0, 0, 0);
        }
        
        try {
            int totalFetched = 0;
            int totalSaved = 0;
            int totalUpdated = 0;
            int page = 0;
            int size = 200;
            boolean hasMorePages = true;
            
            while (hasMorePages) {
                String url = String.format("%s/integration/product/sellers/%d/products?size=%d&page=%d",
                        TRENDYOL_BASE_URL, credentials.getSellerId(), size, page);
                
                HttpHeaders headers = createAuthHeaders(credentials);
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<TrendyolApiProductResponse> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, TrendyolApiProductResponse.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolApiProductResponse apiResponse = response.getBody();
                    
                    if (apiResponse.getContent() != null) {
                        for (TrendyolApiProductResponse.TrendyolApiProduct apiProduct : apiResponse.getContent()) {
                            try {
                                boolean isNew = saveOrUpdateProduct(store, apiProduct);
                                totalFetched++;
                                if (isNew) {
                                    totalSaved++;
                                } else {
                                    totalUpdated++;
                                }
                            } catch (Exception e) {
                                log.error("Error processing product {}: {}", apiProduct.getId(), e.getMessage());
                            }
                        }
                    }
                    
                    // Check if there are more pages
                    hasMorePages = page < (apiResponse.getTotalPages() - 1);
                    page++;
                } else {
                    hasMorePages = false;
                    log.error("Failed to fetch products from Trendyol. Status: {}", response.getStatusCode());
                    return new SyncProductsResponse(false, "Failed to fetch products from Trendyol", 0, 0, 0);
                }
            }
            
            return new SyncProductsResponse(true, "Products synced successfully", 
                    totalFetched, totalSaved, totalUpdated);
            
        } catch (Exception e) {
            log.error("Error syncing products from Trendyol: ", e);
            return new SyncProductsResponse(false, "Error syncing products: " + e.getMessage(), 0, 0, 0);
        }
    }
    
    private boolean saveOrUpdateProduct(Store store, TrendyolApiProductResponse.TrendyolApiProduct apiProduct) {
        // Check if product already exists
        boolean isNew = !trendyolProductRepository.existsByStoreIdAndProductId(store.getId(), apiProduct.getId());
        
        TrendyolProduct product = trendyolProductRepository
                .findByStoreIdAndProductId(store.getId(), apiProduct.getId())
                .orElse(TrendyolProduct.builder()
                        .store(store)
                        .productId(apiProduct.getId())
                        .costAndStockInfo(new ArrayList<>())
                        .build());
        
        // Update product fields
        product.setBarcode(apiProduct.getBarcode());
        product.setTitle(apiProduct.getTitle());
        product.setCategoryName(apiProduct.getCategoryName());
        product.setCreateDateTime(apiProduct.getCreateDateTime());
        product.setHasActiveCampaign(apiProduct.getHasActiveCampaign() != null ? apiProduct.getHasActiveCampaign() : false);
        product.setBrand(apiProduct.getBrand());
        product.setBrandId(apiProduct.getBrandId());
        product.setProductMainId(apiProduct.getProductMainId());
        product.setProductUrl(apiProduct.getProductUrl());
        product.setDimensionalWeight(apiProduct.getDimensionalWeight());
        product.setSalePrice(apiProduct.getSalePrice());
        product.setVatRate(apiProduct.getVatRate());
        product.setQuantity(apiProduct.getQuantity() != null ? apiProduct.getQuantity() : 0);
        
        // Set first image if available
        if (apiProduct.getImages() != null && !apiProduct.getImages().isEmpty()) {
            product.setImage(apiProduct.getImages().get(0).getUrl());
        }
        
        trendyolProductRepository.save(product);
        return isNew;
    }
    
    public List<TrendyolProductDto> getProductsByStore(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException("Store not found");
        }
        
        List<TrendyolProduct> products = trendyolProductRepository.findByStoreId(storeId);
        return productMapper.toDtoList(products);
    }
    
    public TrendyolProductDto updateCostAndStock(UUID productId, UpdateCostAndStockRequest request) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Create new cost and stock info
        CostAndStockInfo newInfo = CostAndStockInfo.builder()
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .costVatRate(request.getCostVatRate())
                .stockDate(request.getStockDate() != null ? request.getStockDate() : LocalDate.now())
                .build();
        
        // Add to existing list or merge with same date
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            costAndStockList = new ArrayList<>();
        }
        
        addOrMergeCostAndStockInfo(costAndStockList, newInfo);
        product.setCostAndStockInfo(costAndStockList);
        
        // Update quantity with the latest value
        product.setQuantity(request.getQuantity());
        
        TrendyolProduct savedProduct = trendyolProductRepository.save(product);
        return productMapper.toDto(savedProduct);
    }
    
    public TrendyolProductDto addStockInfo(UUID productId, AddStockInfoRequest request) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        CostAndStockInfo newInfo = CostAndStockInfo.builder()
                .quantity(request.getQuantity())
                .unitCost(request.getUnitCost())
                .costVatRate(request.getCostVatRate())
                .stockDate(request.getStockDate() != null ? request.getStockDate() : LocalDate.now())
                .build();
        
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            costAndStockList = new ArrayList<>();
        }
        
        addOrMergeCostAndStockInfo(costAndStockList, newInfo);
        product.setCostAndStockInfo(costAndStockList);
        
        // Calculate total quantity
        int totalQuantity = costAndStockList.stream()
                .mapToInt(CostAndStockInfo::getQuantity)
                .sum();
        product.setQuantity(totalQuantity);
        
        TrendyolProduct savedProduct = trendyolProductRepository.save(product);
        return productMapper.toDto(savedProduct);
    }
    
    public TrendyolProductDto updateStockInfoByDate(UUID productId, LocalDate stockDate, UpdateStockInfoRequest request) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            throw new RuntimeException("No stock info found for this product");
        }
        
        // Find and update the specific date entry
        boolean found = false;
        for (CostAndStockInfo info : costAndStockList) {
            if (info.getStockDate().equals(stockDate)) {
                info.setQuantity(request.getQuantity());
                info.setUnitCost(request.getUnitCost());
                info.setCostVatRate(request.getCostVatRate());
                found = true;
                break;
            }
        }
        
        if (!found) {
            throw new RuntimeException("No stock info found for date: " + stockDate);
        }
        
        product.setCostAndStockInfo(costAndStockList);
        
        // Recalculate total quantity
        int totalQuantity = costAndStockList.stream()
                .mapToInt(CostAndStockInfo::getQuantity)
                .sum();
        product.setQuantity(totalQuantity);
        
        TrendyolProduct savedProduct = trendyolProductRepository.save(product);
        return productMapper.toDto(savedProduct);
    }
    
    public TrendyolProductDto deleteStockInfoByDate(UUID productId, LocalDate stockDate) {
        TrendyolProduct product = trendyolProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        List<CostAndStockInfo> costAndStockList = product.getCostAndStockInfo();
        if (costAndStockList == null) {
            throw new RuntimeException("No stock info found for this product");
        }
        
        // Remove the specific date entry
        boolean removed = costAndStockList.removeIf(info -> info.getStockDate().equals(stockDate));
        
        if (!removed) {
            throw new RuntimeException("No stock info found for date: " + stockDate);
        }
        
        product.setCostAndStockInfo(costAndStockList);
        
        // Recalculate total quantity
        int totalQuantity = costAndStockList.stream()
                .mapToInt(CostAndStockInfo::getQuantity)
                .sum();
        product.setQuantity(totalQuantity);
        
        TrendyolProduct savedProduct = trendyolProductRepository.save(product);
        return productMapper.toDto(savedProduct);
    }
    
    private void addOrMergeCostAndStockInfo(List<CostAndStockInfo> costAndStockList, CostAndStockInfo newInfo) {
        // Check if there's already an entry for this date
        for (CostAndStockInfo existingInfo : costAndStockList) {
            if (existingInfo.getStockDate().equals(newInfo.getStockDate())) {
                // Merge with existing entry (weighted average for both cost and VAT rate)
                int existingQuantity = existingInfo.getQuantity();
                int newQuantity = newInfo.getQuantity();
                int totalQuantity = existingQuantity + newQuantity;
                
                // Weighted average cost calculation
                double totalCost = (existingQuantity * existingInfo.getUnitCost()) + 
                                  (newQuantity * newInfo.getUnitCost());
                double weightedAverageCost = totalCost / totalQuantity;
                
                // Weighted average VAT rate calculation
                double totalVatWeighted = (existingQuantity * existingInfo.getCostVatRate()) + 
                                         (newQuantity * newInfo.getCostVatRate());
                double weightedAverageVatRate = totalVatWeighted / totalQuantity;
                
                existingInfo.setQuantity(totalQuantity);
                existingInfo.setUnitCost(weightedAverageCost);
                existingInfo.setCostVatRate((int) Math.round(weightedAverageVatRate));
                return;
            }
        }
        
        // No existing entry for this date, add new one
        costAndStockList.add(newInfo);
    }
    
    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        MarketplaceCredentials credentials = store.getCredentials();
        if (credentials instanceof TrendyolCredentials) {
            return (TrendyolCredentials) credentials;
        }
        return null;
    }
    
    private HttpHeaders createAuthHeaders(TrendyolCredentials credentials) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", "SellerX/1.0");
        
        return headers;
    }
}
