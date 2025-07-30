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
import java.time.LocalDate;
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
            // Calculate date ranges - fetch last 3 months in 15-day chunks (GMT+3)
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
            LocalDateTime startDate = now.minusMonths(3);
            
            int totalSaved = 0;
            int totalSkipped = 0;
            
            // Process in 15-day chunks from 3 months ago to now
            LocalDateTime currentStart = startDate;
            
            while (currentStart.isBefore(now)) {
                LocalDateTime currentEnd = currentStart.plusDays(15);
                if (currentEnd.isAfter(now)) {
                    currentEnd = now;
                }
                
                log.info("Fetching orders for store {} from {} to {}", storeId, currentStart, currentEnd);
                
                // Convert to GMT+3 milliseconds
                long startMillis = currentStart.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
                long endMillis = currentEnd.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
                
                // Fetch all pages for this date range
                int[] results = fetchOrdersForDateRange(credentials, storeId, store, startMillis, endMillis);
                totalSaved += results[0];
                totalSkipped += results[1];
                
                currentStart = currentEnd;
                
                // Small delay to avoid rate limiting
                Thread.sleep(1000);
            }
            
            log.info("Completed order fetch for store {}: {} total saved, {} total skipped", 
                    storeId, totalSaved, totalSkipped);
            
        } catch (Exception e) {
            log.error("Error fetching orders for store {}: {}", storeId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch orders from Trendyol: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fetch all orders for a specific date range with pagination
     */
    private int[] fetchOrdersForDateRange(TrendyolCredentials credentials, UUID storeId, Store store, 
                                         long startDate, long endDate) {
        int savedCount = 0;
        int skippedCount = 0;
        int page = 0;
        boolean hasMorePages = true;
        
        // Pre-load all products for this store to avoid N+1 queries
        Map<String, TrendyolProduct> productCache = productRepository.findByStoreId(storeId)
                .stream()
                .filter(p -> p.getBarcode() != null && !p.getBarcode().isEmpty())
                .collect(Collectors.toMap(TrendyolProduct::getBarcode, p -> p));
        
        log.info("Loaded {} products into cache for store {}", productCache.size(), storeId);
        
        while (hasMorePages) {
            try {
                TrendyolOrderApiResponse apiResponse = fetchOrdersFromTrendyol(credentials, page, 200, startDate, endDate);
                
                if (apiResponse == null || apiResponse.getContent() == null || apiResponse.getContent().isEmpty()) {
                    break;
                }
                
                if (page % 10 == 0) { // Log progress every 10 pages
                    log.info("Processing page {} with {} orders", page, apiResponse.getContent().size());
                }
                
                // Batch process orders
                List<TrendyolOrder> ordersToSave = new ArrayList<>();
                Set<String> existingPackages = new HashSet<>();
                
                // Check existing orders in batch
                List<Long> packageNumbers = apiResponse.getContent().stream()
                        .filter(order -> order.getId() != null)
                        .map(TrendyolOrderApiResponse.TrendyolOrderContent::getId)
                        .collect(Collectors.toList());
                
                if (!packageNumbers.isEmpty()) {
                    Set<Long> existingPackageSet = new HashSet<>(orderRepository.findExistingPackageNumbers(storeId, packageNumbers));
                    existingPackages = existingPackageSet.stream().map(String::valueOf).collect(Collectors.toSet());
                }
                
                // Process orders in this page
                for (TrendyolOrderApiResponse.TrendyolOrderContent orderContent : apiResponse.getContent()) {
                    try {
                        // Skip orders without cargoTrackingNumber (package number)
                        if (orderContent.getCargoTrackingNumber() == null || orderContent.getId() == null) {
                            skippedCount++;
                            continue;
                        }
                        
                        // Check if order already exists (from batch check)
                        if (existingPackages.contains(orderContent.getId().toString())) {
                            skippedCount++;
                            continue;
                        }
                        
                        // Convert order using product cache
                        TrendyolOrder order = convertApiResponseToEntity(orderContent, store, productCache);
                        ordersToSave.add(order);
                        savedCount++;
                        
                    } catch (Exception e) {
                        log.error("Error processing order {}: {}", orderContent.getOrderNumber(), e.getMessage());
                        skippedCount++;
                    }
                }
                
                // Batch save orders
                if (!ordersToSave.isEmpty()) {
                    orderRepository.saveAll(ordersToSave);
                    if (page % 10 == 0) {
                        log.info("Saved batch of {} orders", ordersToSave.size());
                    }
                }
                
                // Check if we have more pages
                hasMorePages = (page + 1) < apiResponse.getTotalPages();
                page++;
                
                // Small delay between pages
                if (hasMorePages) {
                    Thread.sleep(500);
                }
                
            } catch (Exception e) {
                log.error("Error fetching page {} for date range: {}", page, e.getMessage(), e);
                break;
            }
        }
        
        return new int[]{savedCount, skippedCount};
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
    
    private TrendyolOrderApiResponse fetchOrdersFromTrendyol(TrendyolCredentials credentials, int page, int size, 
                                                           Long startDate, Long endDate) {
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // Build URL with date parameters
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(String.format("%s/integration/order/sellers/%s/orders?page=%d&size=%d", 
                TRENDYOL_BASE_URL, credentials.getSellerId(), page, size));
        
        if (startDate != null) {
            urlBuilder.append("&startDate=").append(startDate);
        }
        if (endDate != null) {
            urlBuilder.append("&endDate=").append(endDate);
        }
        
        String url = urlBuilder.toString();
        log.debug("Fetching orders from URL: {}", url);
        
        ResponseEntity<TrendyolOrderApiResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, TrendyolOrderApiResponse.class);
        
        return response.getBody();
    }
    
    private TrendyolOrder convertApiResponseToEntity(TrendyolOrderApiResponse.TrendyolOrderContent orderContent, Store store) {
        return convertApiResponseToEntity(orderContent, store, null);
    }
    
    private TrendyolOrder convertApiResponseToEntity(TrendyolOrderApiResponse.TrendyolOrderContent orderContent, Store store, Map<String, TrendyolProduct> productCache) {
        // Convert milliseconds to LocalDateTime (Trendyol sends in GMT+3, keep it as is)
        LocalDateTime orderDate = Instant.ofEpochMilli(orderContent.getOriginShipmentDate())
                .atZone(ZoneId.of("Europe/Istanbul")) // GMT+3 timezone
                .toLocalDateTime();
        
        // Convert order lines to order items with cost information
        List<OrderItem> orderItems = orderContent.getLines().stream()
                .map(line -> convertLineToOrderItem(line, store.getId(), orderDate, productCache))
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
    
    private OrderItem convertLineToOrderItem(TrendyolOrderApiResponse.TrendyolOrderLine line, UUID storeId, LocalDateTime orderDate) {
        return convertLineToOrderItem(line, storeId, orderDate, null);
    }
    
    private OrderItem convertLineToOrderItem(TrendyolOrderApiResponse.TrendyolOrderLine line, UUID storeId, LocalDateTime orderDate, Map<String, TrendyolProduct> productCache) {
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
            TrendyolProduct product = null;
            
            // Use cache if available, otherwise query database
            if (productCache != null) {
                product = productCache.get(line.getBarcode());
            } else {
                Optional<TrendyolProduct> productOpt = productRepository.findByStoreIdAndBarcode(storeId, line.getBarcode());
                product = productOpt.orElse(null);
            }
            
            if (product != null) {
                // Get the appropriate cost information based on order date
                if (!product.getCostAndStockInfo().isEmpty()) {
                    // Sort cost and stock info by date (earliest first)
                    List<CostAndStockInfo> sortedCosts = product.getCostAndStockInfo().stream()
                            .filter(cost -> cost.getStockDate() != null)
                            .sorted((c1, c2) -> c1.getStockDate().compareTo(c2.getStockDate()))
                            .collect(Collectors.toList());
                    
                    if (!sortedCosts.isEmpty()) {
                        // Find the most appropriate cost for the order date
                        CostAndStockInfo appropriateCost = findAppropriateCost(sortedCosts, orderDate.toLocalDate());
                        
                        if (appropriateCost != null) {
                            itemBuilder.cost(appropriateCost.getUnitCost() != null ? 
                                            BigDecimal.valueOf(appropriateCost.getUnitCost()) : null)
                                      .costVat(appropriateCost.getCostVatRate());
                            
                            log.debug("Found cost {} for product {} on order date {}", 
                                    appropriateCost.getUnitCost(), line.getBarcode(), orderDate);
                        } else {
                            log.debug("No appropriate cost found for product {} on order date {}", 
                                    line.getBarcode(), orderDate);
                        }
                    }
                } else {
                    log.debug("No cost information found for product with barcode: {}", line.getBarcode());
                }
            } else {
                log.debug("Product not found in trendyol_products for barcode: {}", line.getBarcode());
            }
        }
        
        return itemBuilder.build();
    }
    
    /**
     * Find the most appropriate cost for the given order date
     * Logic: Use the latest cost entry that is on or before the order date
     */
    private CostAndStockInfo findAppropriateCost(List<CostAndStockInfo> sortedCosts, java.time.LocalDate orderDate) {
        CostAndStockInfo appropriateCost = null;
        
        for (CostAndStockInfo cost : sortedCosts) {
            // If cost date is after order date, break (since list is sorted)
            if (cost.getStockDate().isAfter(orderDate)) {
                break;
            }
            // This cost is on or before the order date, so it's a candidate
            appropriateCost = cost;
        }
        
        return appropriateCost;
    }
    
    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        if (store.getCredentials() instanceof TrendyolCredentials) {
            return (TrendyolCredentials) store.getCredentials();
        }
        return null;
    }
}
