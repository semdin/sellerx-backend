package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolOrderService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    
    private final TrendyolOrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductRepository productRepository;
    private final TrendyolOrderMapper orderMapper;
    private final RestTemplate restTemplate;

    /**
     * Fetch and save orders for a specific store from Trendyol API
     */
    public void fetchAndSaveOrdersForStore(UUID storeId) {
        log.info("Starting to fetch orders for store: {}", storeId);
        
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
        
        if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
            throw new RuntimeException("Store is not a Trendyol store");
        }
        
        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null) {
            throw new RuntimeException("Trendyol credentials not found");
        }
        
        try {
            // Fetch orders from Trendyol API
            TrendyolOrderApiResponse apiResponse = fetchOrdersFromTrendyol(credentials, 0, 200);
            
            if (apiResponse == null || apiResponse.getContent() == null) {
                log.warn("No orders received from Trendyol API for store: {}", storeId);
                return;
            }
            
            log.info("Received {} orders from Trendyol API for store: {}", 
                    apiResponse.getContent().size(), storeId);
            
            // Process and save orders
            int savedCount = 0;
            int skippedCount = 0;
            
            for (TrendyolOrderApiResponse.TrendyolOrderContent orderContent : apiResponse.getContent()) {
                try {
                    // Skip orders without cargoTrackingNumber (package number)
                    if (orderContent.getCargoTrackingNumber() == null || orderContent.getId() == null) {
                        log.debug("Skipping order {} - no package number", orderContent.getOrderNumber());
                        skippedCount++;
                        continue;
                    }
                    
                    // Check if order already exists
                    if (orderRepository.existsByStoreIdAndPackageNo(storeId, orderContent.getId())) {
                        log.debug("Order already exists for store {} and package {}", storeId, orderContent.getId());
                        skippedCount++;
                        continue;
                    }
                    
                    // Convert and save order
                    TrendyolOrder order = convertApiResponseToEntity(orderContent, store);
                    orderRepository.save(order);
                    savedCount++;
                    
                    log.debug("Saved order: {} with package: {}", order.getTyOrderNumber(), order.getPackageNo());
                    
                } catch (Exception e) {
                    log.error("Error processing order {}: {}", orderContent.getOrderNumber(), e.getMessage(), e);
                }
            }
            
            log.info("Completed order fetch for store {}: {} saved, {} skipped", storeId, savedCount, skippedCount);
            
            // If there are more pages, we could implement pagination here
            if (apiResponse.getTotalPages() > 1) {
                log.info("Total pages available: {}, currently processed page 1", apiResponse.getTotalPages());
                // For now, we're only processing the first page
                // You can extend this to fetch all pages if needed
            }
            
        } catch (Exception e) {
            log.error("Error fetching orders for store {}: {}", storeId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch orders from Trendyol: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get orders for a store with pagination
     */
    public Page<TrendyolOrderDto> getOrdersForStore(UUID storeId, Pageable pageable) {
        Page<TrendyolOrder> orders = orderRepository.findByStoreIdOrderByOrderDateDesc(storeId, pageable);
        return orders.map(orderMapper::toDto);
    }
    
    /**
     * Get orders for store by date range
     */
    public Page<TrendyolOrderDto> getOrdersForStoreByDateRange(UUID storeId, 
                                                              LocalDateTime startDate, 
                                                              LocalDateTime endDate, 
                                                              Pageable pageable) {
        Page<TrendyolOrder> orders = orderRepository.findByStoreAndDateRange(storeId, startDate, endDate, pageable);
        return orders.map(orderMapper::toDto);
    }
    
    /**
     * Get orders by status
     */
    public Page<TrendyolOrderDto> getOrdersByStatus(UUID storeId, String status, Pageable pageable) {
        Page<TrendyolOrder> orders = orderRepository.findByStoreAndStatus(storeId, status, pageable);
        return orders.map(orderMapper::toDto);
    }
    
    /**
     * Get order statistics for a store
     */
    public OrderStatistics getOrderStatistics(UUID storeId) {
        long totalOrders = orderRepository.countByStoreId(storeId);
        long deliveredOrders = orderRepository.countByStoreIdAndStatus(storeId, "Delivered");
        long returnedOrders = orderRepository.countByStoreIdAndStatus(storeId, "Returned");
        
        return OrderStatistics.builder()
                .totalOrders(totalOrders)
                .deliveredOrders(deliveredOrders)
                .returnedOrders(returnedOrders)
                .build();
    }
    
    private TrendyolOrderApiResponse fetchOrdersFromTrendyol(TrendyolCredentials credentials, int page, int size) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        String url = String.format("%s/integration/order/sellers/%s/orders?page=%d&size=%d", 
                TRENDYOL_BASE_URL, credentials.getSellerId(), page, size);
        
        ResponseEntity<TrendyolOrderApiResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, TrendyolOrderApiResponse.class);
        
        return response.getBody();
    }
    
    private TrendyolOrder convertApiResponseToEntity(TrendyolOrderApiResponse.TrendyolOrderContent orderContent, Store store) {
        // Convert milliseconds to LocalDateTime
        LocalDateTime orderDate = Instant.ofEpochMilli(orderContent.getOriginShipmentDate())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        
        // Convert order lines to order items with cost information
        List<OrderItem> orderItems = orderContent.getLines().stream()
                .map(line -> convertLineToOrderItem(line, store.getId()))
                .collect(Collectors.toList());
        
        return TrendyolOrder.builder()
                .store(store)
                .tyOrderNumber(orderContent.getOrderNumber())
                .packageNo(orderContent.getId())
                .orderDate(orderDate)
                .grossAmount(orderContent.getGrossAmount())
                .totalDiscount(orderContent.getTotalDiscount())
                .totalTyDiscount(orderContent.getTotalTyDiscount())
                .orderItems(orderItems)
                .shipmentPackageStatus(orderContent.getShipmentPackageStatus())
                .status(orderContent.getStatus())
                .cargoDeci(orderContent.getCargoDeci())
                .build();
    }
    
    private OrderItem convertLineToOrderItem(TrendyolOrderApiResponse.TrendyolOrderLine line, UUID storeId) {
        OrderItem.OrderItemBuilder itemBuilder = OrderItem.builder()
                .barcode(line.getBarcode())
                .productName(line.getProductName())
                .quantity(line.getQuantity())
                .unitPriceOrder(line.getAmount())
                .unitPriceDiscount(line.getDiscount())
                .unitPriceTyDiscount(line.getTyDiscount())
                .vatBaseAmount(line.getVatBaseAmount())
                .price(line.getPrice());
        
        // Try to get cost information from our trendyol_products table
        if (line.getBarcode() != null && !line.getBarcode().isEmpty()) {
            Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(storeId, line.getBarcode());
            
            if (productOpt.isPresent()) {
                TrendyolProduct product = productOpt.get();
                
                // Get the latest cost information
                if (!product.getCostAndStockInfo().isEmpty()) {
                    CostAndStockInfo latestCost = product.getCostAndStockInfo().get(0);
                    itemBuilder.cost(latestCost.getUnitCost() != null ? 
                                    BigDecimal.valueOf(latestCost.getUnitCost()) : null)
                              .costVat(latestCost.getCostVatRate());
                } else {
                    log.debug("No cost information found for product with barcode: {}", line.getBarcode());
                }
            } else {
                log.debug("Product not found in trendyol_products for barcode: {}", line.getBarcode());
            }
        }
        
        return itemBuilder.build();
    }
    
    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        if (store.getCredentials() instanceof TrendyolCredentials) {
            return (TrendyolCredentials) store.getCredentials();
        }
        return null;
    }
}
