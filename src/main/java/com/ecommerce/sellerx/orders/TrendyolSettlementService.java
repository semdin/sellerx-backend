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

        // Fetch both Sale and Return settlements
        String[] transactionTypes = {"Sale", "Return"};
        
        for (String transactionType : transactionTypes) {
            log.info("Fetching {} settlements for store: {} from {} to {}", 
                transactionType, store.getId(), startDate, endDate);
                
            fetchSettlementsByType(store, credentials, entity, startTimestamp, endTimestamp, transactionType);
            
            // Add delay between different transaction types
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void fetchSettlementsByType(Store store, TrendyolCredentials credentials, HttpEntity<String> entity,
                                      long startTimestamp, long endTimestamp, String transactionType) {
        // Start with page 0 and fetch all pages
        int currentPage = 0;
        int totalPages = 1; // Start with 1, will be updated from first response
        int totalProcessed = 0;

        while (currentPage < totalPages) {
            String url = TRENDYOL_BASE_URL + SETTLEMENT_ENDPOINT + 
                        "?transactionType=" + transactionType +
                        "&startDate=" + startTimestamp +
                        "&endDate=" + endTimestamp +
                        "&page=" + currentPage +
                        "&size=1000";

            try {
                log.info("Fetching {} settlements page {} of {} for store: {}", 
                    transactionType, currentPage + 1, totalPages, store.getId());
                
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
                        log.info("Total {} pages for {} settlements, total elements: {} for store: {}", 
                            totalPages, transactionType, settlementResponse.getTotalElements(), store.getId());
                    }
                    
                    // Process this page's settlements
                    if (settlementResponse.getContent() != null && !settlementResponse.getContent().isEmpty()) {
                        processSettlementResponse(store, settlementResponse);
                        totalProcessed += settlementResponse.getContent().size();
                    }
                } else {
                    log.warn("Failed to fetch {} settlements page {} for store: {} - Status: {}", 
                        transactionType, currentPage, store.getId(), response.getStatusCode());
                    break; // Stop processing if we get an error
                }

                currentPage++;
                
                // Add a small delay between pages to avoid rate limiting
                if (currentPage < totalPages) {
                    Thread.sleep(200);
                }

            } catch (Exception e) {
                log.error("Error fetching {} settlements page {} for store: {}", 
                    transactionType, currentPage, store.getId(), e);
                break; // Stop processing if we get an error
            }
        }
        
        log.info("Completed fetching {} settlements for store: {} - Processed {} settlements across {} pages", 
            transactionType, store.getId(), totalProcessed, currentPage);
    }

    private void processSettlementResponse(Store store, TrendyolSettlementResponse response) {
        if (response.getContent() == null || response.getContent().isEmpty()) {
            log.info("No settlements found in this page for store: {}", store.getId());
            return;
        }

        log.info("Processing {} settlement items for store: {}", 
            response.getContent().size(), store.getId());

        // Separate sale and return settlements for different processing logic
        Map<String, List<TrendyolSettlementItem>> saleSettlements = new HashMap<>();
        Map<String, List<TrendyolSettlementItem>> returnSettlements = new HashMap<>();
        
        for (TrendyolSettlementItem item : response.getContent()) {
            String key = item.getOrderNumber() + "_" + item.getShipmentPackageId();
            
            if ("Satış".equals(item.getTransactionType()) || "Sale".equals(item.getTransactionType())) {
                saleSettlements.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            } else if ("İade".equals(item.getTransactionType()) || "Return".equals(item.getTransactionType())) {
                // For returns, we might need to search by order number only since package might be different
                String returnKey = item.getOrderNumber();
                returnSettlements.computeIfAbsent(returnKey, k -> new ArrayList<>()).add(item);
            }
        }

        log.info("Grouped settlements for store: {} - Sales: {}, Returns: {}", 
            store.getId(), saleSettlements.size(), returnSettlements.size());

        int processedOrders = 0;
        int foundOrders = 0;
        
        // Process sale settlements (existing logic)
        for (Map.Entry<String, List<TrendyolSettlementItem>> entry : saleSettlements.entrySet()) {
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
                log.error("Failed to update order {} package {} with sale settlements", 
                    orderNumber, packageId, e);
            }
        }
        
        // Process return settlements (special logic)
        for (Map.Entry<String, List<TrendyolSettlementItem>> entry : returnSettlements.entrySet()) {
            String orderNumber = entry.getKey();
            
            try {
                boolean orderFound = updateOrderWithReturnSettlements(store, orderNumber, entry.getValue());
                processedOrders++;
                if (orderFound) {
                    foundOrders++;
                }
            } catch (Exception e) {
                log.error("Failed to update order {} with return settlements", orderNumber, e);
            }
        }
        
        log.info("Settlement processing completed for store: {} - Processed: {} orders, Found in system: {}", 
            store.getId(), processedOrders, foundOrders);
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

        // Group settlements by barcode, but ONLY for this specific packageId
        Map<String, List<TrendyolSettlementItem>> settlementsByBarcode = settlementItems.stream()
            .filter(item -> packageId.equals(item.getShipmentPackageId())) // Filter by package ID
            .collect(Collectors.groupingBy(TrendyolSettlementItem::getBarcode));

        if (settlementsByBarcode.isEmpty()) {
            log.debug("No settlements found for package {} in order {}", packageId, orderNumber);
            return false;
        }

        boolean orderUpdated = false;
        int itemsWithSettlements = 0;
        
        // Update each order item with its corresponding settlements
        for (OrderItem orderItem : orderItems) {
            String barcode = orderItem.getBarcode();
            List<TrendyolSettlementItem> itemSettlements = settlementsByBarcode.get(barcode);
            
            if (itemSettlements != null && !itemSettlements.isEmpty()) {
                // Process settlements in a smart way: prefer returns over sales for status updates
                boolean itemUpdated = processItemSettlements(orderItem, itemSettlements, orderNumber, packageId);
                
                if (itemUpdated) {
                    itemsWithSettlements++;
                    orderUpdated = true;
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
     * Process settlements for a single order item with smart status management
     */
    private boolean processItemSettlements(OrderItem orderItem, List<TrendyolSettlementItem> itemSettlements, 
                                         String orderNumber, Long packageId) {
        
        // Initialize transactions list if null
        if (orderItem.getTransactions() == null) {
            orderItem.setTransactions(new ArrayList<>());
        }

        boolean itemUpdated = false;
        
        // Separate settlements by type to handle them intelligently
        List<TrendyolSettlementItem> sales = new ArrayList<>();
        List<TrendyolSettlementItem> returns = new ArrayList<>();
        List<TrendyolSettlementItem> others = new ArrayList<>();
        
        for (TrendyolSettlementItem settlement : itemSettlements) {
            String transactionType = settlement.getTransactionType();
            if ("Satış".equals(transactionType) || "Sale".equals(transactionType)) {
                sales.add(settlement);
            } else if ("İade".equals(transactionType) || "Return".equals(transactionType)) {
                returns.add(settlement);
            } else {
                others.add(settlement);
            }
        }

        // First, add all sales as SOLD
        for (TrendyolSettlementItem saleSettlement : sales) {
            OrderItemSettlement settlement = settlementMapper.mapToOrderItemSettlement(saleSettlement);
            
            // Check if this settlement already exists
            boolean exists = orderItem.getTransactions().stream()
                .anyMatch(existing -> existing.getId().equals(settlement.getId()));
                
            if (!exists) {
                settlement.setStatus("SOLD");
                orderItem.getTransactions().add(settlement);
                itemUpdated = true;
                log.debug("Added SOLD settlement {} for product {} in order {} package {}", 
                    settlement.getId(), orderItem.getBarcode(), orderNumber, packageId);
            }
        }
        
        // Then, handle returns - update existing SOLD transactions to RETURNED
        for (TrendyolSettlementItem returnSettlement : returns) {
            OrderItemSettlement returnSettlementObj = settlementMapper.mapToOrderItemSettlement(returnSettlement);
            
            // Check if this return settlement already exists
            boolean returnExists = orderItem.getTransactions().stream()
                .anyMatch(existing -> existing.getId().equals(returnSettlementObj.getId()));
                
            if (!returnExists) {
                // Find a SOLD transaction to convert to RETURNED
                Optional<OrderItemSettlement> soldTransaction = orderItem.getTransactions().stream()
                    .filter(t -> "SOLD".equals(t.getStatus()) && t.getBarcode().equals(returnSettlement.getBarcode()))
                    .findFirst();
                
                if (soldTransaction.isPresent()) {
                    // Update existing SOLD transaction to RETURNED
                    OrderItemSettlement existingTransaction = soldTransaction.get();
                    existingTransaction.setStatus("RETURNED");
                    existingTransaction.setTransactionType("İade");
                    // Keep original sale data but mark as returned
                    log.info("Updated transaction {} from SOLD to RETURNED for product {} in order {} package {}", 
                        existingTransaction.getId(), orderItem.getBarcode(), orderNumber, packageId);
                } else {
                    // No SOLD transaction found, add as new RETURNED transaction
                    returnSettlementObj.setStatus("RETURNED");
                    orderItem.getTransactions().add(returnSettlementObj);
                    log.info("Added new RETURNED settlement {} for product {} in order {} package {}", 
                        returnSettlementObj.getId(), orderItem.getBarcode(), orderNumber, packageId);
                }
                itemUpdated = true;
            }
        }
        
        // Handle other transaction types
        for (TrendyolSettlementItem otherSettlement : others) {
            OrderItemSettlement settlement = settlementMapper.mapToOrderItemSettlement(otherSettlement);
            
            boolean exists = orderItem.getTransactions().stream()
                .anyMatch(existing -> existing.getId().equals(settlement.getId()));
                
            if (!exists) {
                orderItem.getTransactions().add(settlement);
                itemUpdated = true;
                log.debug("Added {} settlement {} for product {} in order {} package {}", 
                    settlement.getStatus(), settlement.getId(), orderItem.getBarcode(), orderNumber, packageId);
            }
        }
        
        return itemUpdated;
    }

    /**
     * Update orders with return settlements.
     * For returns, we find SOLD transactions and update their status to RETURNED.
     */
    private boolean updateOrderWithReturnSettlements(Store store, String orderNumber, 
                                                   List<TrendyolSettlementItem> returnSettlements) {
        
        // Find all orders with this order number for the store
        List<TrendyolOrder> orders = orderRepository.findByStoreIdAndTyOrderNumber(store.getId(), orderNumber);

        if (orders.isEmpty()) {
            log.debug("No orders found for return settlement: orderNumber={}, store={}", 
                orderNumber, store.getId());
            return false;
        }

        log.debug("Found {} order packages for return settlement: orderNumber={}", orders.size(), orderNumber);

        // Group return settlements by barcode and count them
        Map<String, Integer> returnCountByBarcode = returnSettlements.stream()
            .collect(Collectors.groupingBy(
                TrendyolSettlementItem::getBarcode,
                Collectors.summingInt(item -> 1)
            ));

        log.info("Return settlement counts by barcode for order {}: {}", orderNumber, returnCountByBarcode);

        boolean anyOrderUpdated = false;

        // Process each barcode across all order packages
        for (Map.Entry<String, Integer> entry : returnCountByBarcode.entrySet()) {
            String barcode = entry.getKey();
            int totalReturnsForBarcode = entry.getValue();
            
            log.info("Processing {} returns for barcode {} in order {}", totalReturnsForBarcode, barcode, orderNumber);
            
            // Find all order items with this barcode across all packages
            List<OrderItem> matchingOrderItems = orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .filter(item -> barcode.equals(item.getBarcode()))
                .collect(Collectors.toList());
            
            if (matchingOrderItems.isEmpty()) {
                log.warn("No order items found for barcode {} in order {}", barcode, orderNumber);
                continue;
            }
            
            // Calculate total quantity and SOLD transactions for this barcode
            int totalQuantityForBarcode = matchingOrderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
                
            log.info("Total quantity for barcode {} in order {}: {}", barcode, orderNumber, totalQuantityForBarcode);
            
            // Count existing SOLD transactions
            int soldTransactionsUpdated = 0;
            
            for (OrderItem orderItem : matchingOrderItems) {
                if (orderItem.getTransactions() == null) {
                    continue;
                }
                
                // Find SOLD transactions for this barcode and update them to RETURNED
                List<OrderItemSettlement> soldTransactions = orderItem.getTransactions().stream()
                    .filter(t -> "SOLD".equals(t.getStatus()) && barcode.equals(t.getBarcode()))
                    .collect(Collectors.toList());
                
                for (OrderItemSettlement soldTransaction : soldTransactions) {
                    if (soldTransactionsUpdated < totalReturnsForBarcode) {
                        soldTransaction.setStatus("RETURNED");
                        soldTransaction.setTransactionType("İade");
                        soldTransactionsUpdated++;
                        anyOrderUpdated = true;
                        
                        log.info("Updated transaction {} from SOLD to RETURNED for barcode {} in order {}", 
                            soldTransaction.getId(), barcode, orderNumber);
                    }
                }
            }
            
            log.info("Updated {}/{} SOLD transactions to RETURNED for barcode {} in order {}", 
                soldTransactionsUpdated, totalReturnsForBarcode, barcode, orderNumber);
        }

        // Save all updated orders
        if (anyOrderUpdated) {
            for (TrendyolOrder order : orders) {
                if (order.getTransactionStatus() == null || "NOT_SETTLED".equals(order.getTransactionStatus())) {
                    order.setTransactionStatus("SETTLED");
                }
                orderRepository.save(order);
            }
            
            log.info("Successfully processed return settlements for order {}", orderNumber);
        }

        return anyOrderUpdated;
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
     * Get settlement statistics for a store including both sales and returns
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
        
        // Get detailed transaction statistics
        Map<String, Object> transactionStats = getTransactionStatistics(store);
        stats.put("transactionStats", transactionStats);
        
        return stats;
    }

    /**
     * Get detailed transaction statistics (sales vs returns)
     */
    private Map<String, Object> getTransactionStatistics(Store store) {
        List<TrendyolOrder> settledOrders = orderRepository.findByStoreAndTransactionStatus(store, "SETTLED");
        
        int totalSaleTransactions = 0;
        int totalReturnTransactions = 0;
        double totalSaleRevenue = 0.0;
        double totalReturnAmount = 0.0;
        
        for (TrendyolOrder order : settledOrders) {
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    if (item.getTransactions() != null) {
                        for (OrderItemSettlement transaction : item.getTransactions()) {
                            if ("Satış".equals(transaction.getTransactionType()) || "Sale".equals(transaction.getTransactionType())) {
                                totalSaleTransactions++;
                                if (transaction.getSellerRevenue() != null) {
                                    totalSaleRevenue += transaction.getSellerRevenue().doubleValue();
                                }
                            } else if ("İade".equals(transaction.getTransactionType()) || "Return".equals(transaction.getTransactionType())) {
                                totalReturnTransactions++;
                                if (transaction.getSellerRevenue() != null) {
                                    totalReturnAmount += transaction.getSellerRevenue().doubleValue();
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Map<String, Object> transactionStats = new HashMap<>();
        transactionStats.put("totalSaleTransactions", totalSaleTransactions);
        transactionStats.put("totalReturnTransactions", totalReturnTransactions);
        transactionStats.put("totalSaleRevenue", totalSaleRevenue);
        transactionStats.put("totalReturnAmount", totalReturnAmount);
        transactionStats.put("netRevenue", totalSaleRevenue - totalReturnAmount);
        
        return transactionStats;
    }
}
