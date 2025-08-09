package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolSettlementService {

    private static final String TRENDYOL_BASE_URL = "https://apigw.trendyol.com";
    private static final String SETTLEMENT_ENDPOINT = "/integration/finance/che/sellers/{sellerId}/settlements";
    
    private final TrendyolOrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final TrendyolSettlementMapper settlementMapper;
    private final RestTemplate restTemplate;

    /**
     * Fetch settlements for a store and update corresponding orders
     */
    public void fetchAndUpdateSettlementsForStore(UUID storeId) {
        log.info("Starting to fetch settlements for store: {}", storeId);
        
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
        
        if (!"trendyol".equalsIgnoreCase(store.getMarketplace())) {
            throw new RuntimeException("Store is not a Trendyol store");
        }

        TrendyolCredentials credentials = extractTrendyolCredentials(store);
        if (credentials == null || credentials.getSellerId() == null) {
            throw new RuntimeException("Trendyol credentials not configured for store: " + storeId);
        }

        // Fetch settlements for the last 3 months in 15-day intervals
        fetchSettlementsForLast3Months(store, credentials);
        
        log.info("Completed fetching settlements for store: {}", storeId);
    }

    /**
     * Fetch settlements for all Trendyol stores
     */
    public void fetchAndUpdateSettlementsForAllStores() {
        log.info("Starting to fetch settlements for all Trendyol stores");
        
        List<Store> trendyolStores = storeRepository.findByMarketplaceIgnoreCase("trendyol");
        
        for (Store store : trendyolStores) {
            try {
                fetchAndUpdateSettlementsForStore(store.getId());
            } catch (Exception e) {
                log.error("Failed to fetch settlements for store: {}", store.getId(), e);
            }
        }
        
        log.info("Completed fetching settlements for all Trendyol stores");
    }

    /**
     * Extract TrendyolCredentials from Store
     */
    private TrendyolCredentials extractTrendyolCredentials(Store store) {
        MarketplaceCredentials credentials = store.getCredentials();
        if (credentials instanceof TrendyolCredentials) {
            return (TrendyolCredentials) credentials;
        }
        return null;
    }

    private void fetchSettlementsForLast3Months(Store store, TrendyolCredentials credentials) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeMonthsAgo = now.minusMonths(3);

        // Split the 3-month period into 15-day intervals
        LocalDateTime currentStart = threeMonthsAgo;
        
        while (currentStart.isBefore(now)) {
            LocalDateTime currentEnd = currentStart.plusDays(15);
            if (currentEnd.isAfter(now)) {
                currentEnd = now;
            }
            
            try {
                fetchSettlementsForPeriod(store, credentials, currentStart, currentEnd);
                
                // Add a small delay between API calls to avoid rate limiting
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("Failed to fetch settlements for store: {} in period {} - {}", 
                    store.getId(), currentStart, currentEnd, e);
            }
            
            currentStart = currentEnd;
        }
    }

    private void fetchSettlementsForPeriod(Store store, TrendyolCredentials credentials, 
                                         LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching settlements for store: {} from {} to {}", 
            store.getId(), startDate, endDate);

        long startTimestamp = startDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();
        long endTimestamp = endDate.atZone(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli();

        // Create Basic Auth header like other Trendyol API calls
        String auth = credentials.getApiKey() + ":" + credentials.getApiSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("User-Agent", credentials.getSellerId() + " - SelfIntegration");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Start with page 0 and fetch all pages
        int currentPage = 0;
        int totalPages = 1; // Start with 1, will be updated from first response
        int totalProcessed = 0;

        while (currentPage < totalPages) {
            String url = TRENDYOL_BASE_URL + SETTLEMENT_ENDPOINT + 
                        "?transactionType=Sale" +
                        "&startDate=" + startTimestamp +
                        "&endDate=" + endTimestamp +
                        "&page=" + currentPage +
                        "&size=1000";

            try {
                log.info("Fetching settlements page {} of {} for store: {}", 
                    currentPage + 1, totalPages, store.getId());
                
                ResponseEntity<TrendyolSettlementResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    TrendyolSettlementResponse.class,
                    credentials.getSellerId()
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    TrendyolSettlementResponse settlementResponse = response.getBody();
                    
                    // Update totalPages from the first response
                    if (currentPage == 0) {
                        totalPages = settlementResponse.getTotalPages() != null ? 
                                   settlementResponse.getTotalPages() : 1;
                        log.info("Total {} settlement pages to process for store: {}, total elements: {}", 
                            totalPages, store.getId(), settlementResponse.getTotalElements());
                    }
                    
                    // Process this page's settlements
                    if (settlementResponse.getContent() != null && !settlementResponse.getContent().isEmpty()) {
                        processSettlementResponse(store, settlementResponse);
                        totalProcessed += settlementResponse.getContent().size();
                    }
                } else {
                    log.warn("Failed to fetch settlements page {} for store: {} - Status: {}", 
                        currentPage, store.getId(), response.getStatusCode());
                    break; // Stop processing if we get an error
                }

                currentPage++;
                
                // Add a small delay between pages to avoid rate limiting
                if (currentPage < totalPages) {
                    Thread.sleep(200);
                }

            } catch (Exception e) {
                log.error("Error fetching settlements page {} for store: {}", currentPage, store.getId(), e);
                break; // Stop processing if we get an error
            }
        }
        
        log.info("Completed fetching settlements for store: {} - Processed {} settlements across {} pages", 
            store.getId(), totalProcessed, currentPage);
    }

    private void processSettlementResponse(Store store, TrendyolSettlementResponse response) {
        if (response.getContent() == null || response.getContent().isEmpty()) {
            log.info("No settlements found in this page for store: {}", store.getId());
            return;
        }

        log.info("Processing {} settlement items for store: {}", 
            response.getContent().size(), store.getId());

        // Group settlements by order number and shipment package ID
        Map<String, List<TrendyolSettlementItem>> settlementsByOrder = response.getContent().stream()
            .collect(Collectors.groupingBy(item -> 
                item.getOrderNumber() + "_" + item.getShipmentPackageId()));

        log.info("Grouped settlements into {} unique orders for store: {}", 
            settlementsByOrder.size(), store.getId());

        int processedOrders = 0;
        int foundOrders = 0;
        
        for (Map.Entry<String, List<TrendyolSettlementItem>> entry : settlementsByOrder.entrySet()) {
            String[] parts = entry.getKey().split("_");
            String orderNumber = parts[0];
            Long packageId = Long.valueOf(parts[1]);
            
            try {
                boolean orderFound = updateOrderWithSettlements(store, orderNumber, packageId, entry.getValue());
                processedOrders++;
                if (orderFound) {
                    foundOrders++;
                }
            } catch (Exception e) {
                log.error("Failed to update order {} package {} with settlements", 
                    orderNumber, packageId, e);
            }
        }
        
        log.info("Settlement processing completed for store: {} - Processed: {}/{} orders, Found in system: {}", 
            store.getId(), processedOrders, settlementsByOrder.size(), foundOrders);
    }

    private boolean updateOrderWithSettlements(Store store, String orderNumber, Long packageId, 
                                          List<TrendyolSettlementItem> settlementItems) {
        
        // Find the order by order number, package ID and store
        Optional<TrendyolOrder> orderOptional = orderRepository
            .findByTyOrderNumberAndPackageNoAndStore(orderNumber, packageId, store);

        if (orderOptional.isEmpty()) {
            log.debug("Order not found for settlement update: orderNumber={}, packageId={}, store={}", 
                orderNumber, packageId, store.getId());
            return false;
        }

        TrendyolOrder order = orderOptional.get();
        List<OrderItem> orderItems = order.getOrderItems();
        
        if (orderItems == null || orderItems.isEmpty()) {
            log.warn("No order items found for order: orderNumber={}, packageId={}", 
                orderNumber, packageId);
            return false;
        }

        // Group settlements by barcode to match with order items
        Map<String, List<TrendyolSettlementItem>> settlementsByBarcode = settlementItems.stream()
            .collect(Collectors.groupingBy(TrendyolSettlementItem::getBarcode));

        boolean orderUpdated = false;
        int itemsWithSettlements = 0;
        
        // Update each order item with its corresponding settlements
        for (OrderItem orderItem : orderItems) {
            String barcode = orderItem.getBarcode();
            List<TrendyolSettlementItem> itemSettlements = settlementsByBarcode.get(barcode);
            
            if (itemSettlements != null && !itemSettlements.isEmpty()) {
                // Convert settlement items to our internal format
                List<OrderItemSettlement> orderItemSettlements = itemSettlements.stream()
                    .map(settlementMapper::mapToOrderItemSettlement)
                    .collect(Collectors.toList());
                
                // Add to existing transactions or create new list
                if (orderItem.getTransactions() == null) {
                    orderItem.setTransactions(new ArrayList<>());
                }
                
                // Avoid duplicates - check if settlement ID already exists
                int newSettlementsAdded = 0;
                for (OrderItemSettlement newSettlement : orderItemSettlements) {
                    boolean exists = orderItem.getTransactions().stream()
                        .anyMatch(existing -> existing.getId().equals(newSettlement.getId()));
                    
                    if (!exists) {
                        orderItem.getTransactions().add(newSettlement);
                        newSettlementsAdded++;
                        orderUpdated = true;
                    }
                }
                
                if (newSettlementsAdded > 0) {
                    itemsWithSettlements++;
                    log.debug("Added {} new settlements to product {} in order {}", 
                        newSettlementsAdded, barcode, orderNumber);
                }
            }
        }

        if (orderUpdated) {
            // Set transaction status and date based on settlements
            if (!settlementItems.isEmpty()) {
                TrendyolSettlementItem firstSettlement = settlementItems.get(0);
                order.setTransactionDate(convertTimestampToLocalDateTime(firstSettlement.getTransactionDate()));
                order.setTransactionStatus("SETTLED");
            }

            orderRepository.save(order);
            
            log.info("Updated order {} package {} with settlements for {}/{} products", 
                orderNumber, packageId, itemsWithSettlements, orderItems.size());
        } else {
            log.debug("No new settlements to add for order {} package {}", orderNumber, packageId);
        }
        
        return true; // Order was found
    }
    
    /**
     * Converts milliseconds timestamp to LocalDateTime
     */
    private LocalDateTime convertTimestampToLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("Europe/Istanbul"))
                .toLocalDateTime();
    }

    /**
     * Get settlement statistics for a store
     */
    public Map<String, Object> getSettlementStats(UUID storeId) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        Map<String, Object> stats = new HashMap<>();
        
        long totalOrders = orderRepository.countByStore(store);
        long settledOrders = orderRepository.countByStoreAndTransactionStatus(store, "SETTLED");
        long notSettledOrders = orderRepository.countByStoreAndTransactionStatus(store, "NOT_SETTLED");
        
        stats.put("totalOrders", totalOrders);
        stats.put("settledOrders", settledOrders);
        stats.put("notSettledOrders", notSettledOrders);
        stats.put("settlementRate", totalOrders > 0 ? (double) settledOrders / totalOrders * 100 : 0);
        
        return stats;
    }
}
