package com.ecommerce.sellerx.dashboard;

import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardStatsService {
    
    private final TrendyolOrderRepository orderRepository;
    
    // Turkey timezone
    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");
    
    // Return cost per item (50 TL for now)
    private static final BigDecimal RETURN_COST_PER_ITEM = BigDecimal.valueOf(50);
    
    public DashboardStatsResponse getStatsForStore(UUID storeId) {
        log.info("Calculating dashboard stats for store: {}", storeId);
        
        LocalDate today = LocalDate.now(TURKEY_ZONE);
        LocalDate yesterday = today.minusDays(1);
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate firstDayOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayOfLastMonth = today.withDayOfMonth(1).minusDays(1);
        
        log.info("Current Turkey date: {}", today);
        
        DashboardStatsDto todayStats = calculateStatsForPeriod(storeId, "today", today, today);
        DashboardStatsDto yesterdayStats = calculateStatsForPeriod(storeId, "yesterday", yesterday, yesterday);
        DashboardStatsDto thisMonthStats = calculateStatsForPeriod(storeId, "thisMonth", firstDayOfMonth, today);
        DashboardStatsDto lastMonthStats = calculateStatsForPeriod(storeId, "lastMonth", firstDayOfLastMonth, lastDayOfLastMonth);
        
        return DashboardStatsResponse.builder()
                .today(todayStats)
                .yesterday(yesterdayStats)
                .thisMonth(thisMonthStats)
                .lastMonth(lastMonthStats)
                .storeId(storeId.toString())
                .calculatedAt(LocalDateTime.now(TURKEY_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
    
    private DashboardStatsDto calculateStatsForPeriod(UUID storeId, String period, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        log.debug("Calculating stats for period {} from {} to {}", period, startDateTime, endDateTime);
        
        // Get revenue orders (not cancelled, returned etc.)
        List<TrendyolOrder> revenueOrders = orderRepository.findRevenueOrdersByStoreAndDateRange(
                storeId, startDateTime, endDateTime);
        
        // Get returned orders
        List<TrendyolOrder> returnedOrders = orderRepository.findReturnedOrdersByStoreAndDateRange(
                storeId, startDateTime, endDateTime);
        
        // Calculate basic stats
        int totalOrders = revenueOrders.size();
        int totalProductsSold = calculateTotalProductsSold(revenueOrders);
        int returnCount = returnedOrders.size();
        
        // Calculate revenue (Ciro = gross_amount - total_discount)
        BigDecimal totalRevenue = calculateTotalRevenue(revenueOrders);
        
        // Calculate return cost
        BigDecimal returnCost = RETURN_COST_PER_ITEM.multiply(BigDecimal.valueOf(returnCount));
        
        // Calculate product costs and items without cost
        ProductCostResult productCostResult = calculateProductCosts(revenueOrders);
        BigDecimal totalProductCosts = productCostResult.getTotalCosts();
        int itemsWithoutCost = productCostResult.getItemsWithoutCost();
        
        // Calculate gross profit
        BigDecimal grossProfit = totalRevenue.subtract(totalProductCosts);
        
        // Calculate VAT difference
        BigDecimal vatDifference = calculateVatDifference(revenueOrders);
        
        return DashboardStatsDto.builder()
                .period(period)
                .totalOrders(totalOrders)
                .totalProductsSold(totalProductsSold)
                .totalRevenue(totalRevenue)
                .returnCount(returnCount)
                .returnCost(returnCost)
                .totalProductCosts(totalProductCosts)
                .grossProfit(grossProfit)
                .vatDifference(vatDifference)
                .itemsWithoutCost(itemsWithoutCost)
                .orders(calculateOrderDetails(revenueOrders, returnedOrders))
                .products(calculateProductDetails(revenueOrders, returnedOrders))
                .build();
    }
    
    private int calculateTotalProductsSold(List<TrendyolOrder> orders) {
        return orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
    
    private BigDecimal calculateTotalRevenue(List<TrendyolOrder> orders) {
        return orders.stream()
                .map(order -> {
                    BigDecimal grossAmount = order.getGrossAmount() != null ? order.getGrossAmount() : BigDecimal.ZERO;
                    BigDecimal totalDiscount = order.getTotalDiscount() != null ? order.getTotalDiscount() : BigDecimal.ZERO;
                    return grossAmount.subtract(totalDiscount);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private ProductCostResult calculateProductCosts(List<TrendyolOrder> orders) {
        BigDecimal totalCosts = BigDecimal.ZERO;
        int itemsWithoutCost = 0;
        
        for (TrendyolOrder order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getCost() != null && item.getCost().compareTo(BigDecimal.ZERO) > 0) {
                    // Item has cost - add to total
                    BigDecimal itemTotalCost = item.getCost().multiply(BigDecimal.valueOf(item.getQuantity()));
                    totalCosts = totalCosts.add(itemTotalCost);
                } else {
                    // Item doesn't have cost
                    itemsWithoutCost += item.getQuantity();
                }
            }
        }
        
        return new ProductCostResult(totalCosts, itemsWithoutCost);
    }
    
    private BigDecimal calculateVatDifference(List<TrendyolOrder> orders) {
        BigDecimal totalVatDifference = BigDecimal.ZERO;
        
        for (TrendyolOrder order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                // Only calculate VAT difference if both cost and price exist
                if (item.getPrice() != null && item.getCost() != null && 
                    item.getPrice().compareTo(BigDecimal.ZERO) > 0 && 
                    item.getCost().compareTo(BigDecimal.ZERO) > 0) {
                    
                    // Sales VAT (Tahsil Edilen KDV) - Calculate from price using vatBaseAmount as rate
                    BigDecimal salesVat;
                    if (item.getVatBaseAmount() != null && item.getVatBaseAmount().compareTo(BigDecimal.ZERO) > 0) {
                        // vatBaseAmount is actually the VAT rate (e.g., 20 for 20%)
                        BigDecimal salesVatRate = item.getVatBaseAmount().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        BigDecimal salesPriceIncludingVat = item.getPrice();
                        
                        // Formula: Sales VAT = (Sales Price (VAT Included) / (1 + VAT Rate)) × VAT Rate
                        BigDecimal salesPriceExcludingVat = salesPriceIncludingVat.divide(
                                BigDecimal.ONE.add(salesVatRate), 4, RoundingMode.HALF_UP);
                        salesVat = salesPriceExcludingVat.multiply(salesVatRate);
                    } else {
                        // Fallback: Calculate from price assuming 20% VAT
                        BigDecimal salesPrice = item.getPrice();
                        salesVat = salesPrice.multiply(BigDecimal.valueOf(0.20))
                                .divide(BigDecimal.valueOf(1.20), 2, RoundingMode.HALF_UP);
                    }
                    
                    // Cost VAT (Ödenen KDV) - Calculate using actual costVat rate
                    BigDecimal costVat = BigDecimal.ZERO;
                    if (item.getCostVat() != null && item.getCostVat() > 0) {
                        // costVat is the VAT rate (e.g., 20 for 20%)
                        BigDecimal costVatRate = BigDecimal.valueOf(item.getCostVat()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        BigDecimal costIncludingVat = item.getCost();
                        
                        // Formula: Cost VAT = (Cost (VAT Included) / (1 + VAT Rate)) × VAT Rate
                        BigDecimal costExcludingVat = costIncludingVat.divide(
                                BigDecimal.ONE.add(costVatRate), 4, RoundingMode.HALF_UP);
                        costVat = costExcludingVat.multiply(costVatRate);
                    }
                    
                    // VAT difference per item (Net KDV Farkı)
                    // KDV Farkı = Satış KDV − Alış KDV
                    BigDecimal vatDifferencePerItem = salesVat.subtract(costVat);
                    
                    // Total VAT difference for this item (multiply by quantity)
                    BigDecimal itemVatDifference = vatDifferencePerItem.multiply(BigDecimal.valueOf(item.getQuantity()));
                    
                    totalVatDifference = totalVatDifference.add(itemVatDifference);
                    
                    log.debug("VAT calculation for {}: Sales VAT={}, Cost VAT={}, Difference={}, Quantity={}, Total Diff={}", 
                            item.getBarcode(), salesVat, costVat, vatDifferencePerItem, item.getQuantity(), itemVatDifference);
                }
            }
        }
        
        return totalVatDifference;
    }
    
    // Helper class for product cost calculation result
    private static class ProductCostResult {
        private final BigDecimal totalCosts;
        private final int itemsWithoutCost;
        
        public ProductCostResult(BigDecimal totalCosts, int itemsWithoutCost) {
            this.totalCosts = totalCosts;
            this.itemsWithoutCost = itemsWithoutCost;
        }
        
        public BigDecimal getTotalCosts() {
            return totalCosts;
        }
        
        public int getItemsWithoutCost() {
            return itemsWithoutCost;
        }
    }
    
    private List<OrderDetailDto> calculateOrderDetails(List<TrendyolOrder> revenueOrders, List<TrendyolOrder> returnedOrders) {
        return revenueOrders.stream()
                .map(order -> {
                    // Siparişin ürünlerini listele
                    List<OrderProductDetailDto> products = order.getOrderItems().stream()
                            .map(item -> OrderProductDetailDto.builder()
                                    .productName(item.getProductName())
                                    .quantity(item.getQuantity())
                                    .build())
                            .toList();
                    
                    // Sipariş için ciro hesaplama (gross_amount - total_discount)
                    BigDecimal grossAmount = order.getGrossAmount() != null ? order.getGrossAmount() : BigDecimal.ZERO;
                    BigDecimal totalDiscount = order.getTotalDiscount() != null ? order.getTotalDiscount() : BigDecimal.ZERO;
                    BigDecimal orderRevenue = grossAmount.subtract(totalDiscount);
                    
                    // Sipariş için toplam maliyet hesaplama
                    BigDecimal orderTotalCost = order.getOrderItems().stream()
                            .filter(item -> item.getCost() != null && item.getCost().compareTo(BigDecimal.ZERO) > 0)
                            .map(item -> item.getCost().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    // Brüt kar hesaplama
                    BigDecimal orderGrossProfit = orderRevenue.subtract(orderTotalCost);
                    
                    // İade fiyatı (şimdilik 0, gerekirse daha sonra hesaplanabilir)
                    BigDecimal returnPrice = BigDecimal.ZERO;
                    
                    return OrderDetailDto.builder()
                            .orderNumber(order.getTyOrderNumber())
                            .orderDate(order.getOrderDate())
                            .products(products)
                            .totalPrice(grossAmount)
                            .returnPrice(returnPrice)
                            .revenue(orderRevenue)
                            .grossProfit(orderGrossProfit)
                            .build();
                })
                .toList();
    }
    
    private List<ProductDetailDto> calculateProductDetails(List<TrendyolOrder> revenueOrders, List<TrendyolOrder> returnedOrders) {
        Map<String, ProductDetailDto.ProductDetailDtoBuilder> productMap = new HashMap<>();
        
        // Revenue siparişlerinden ürün bilgilerini topla
        for (TrendyolOrder order : revenueOrders) {
            for (OrderItem item : order.getOrderItems()) {
                String barcode = item.getBarcode();
                String productName = item.getProductName();
                ProductDetailDto.ProductDetailDtoBuilder builder = productMap.computeIfAbsent(barcode, 
                        k -> ProductDetailDto.builder()
                                .productName(productName)
                                .barcode(barcode)
                                .totalSoldQuantity(0)
                                .returnQuantity(0)
                                .revenue(BigDecimal.ZERO)
                                .grossProfit(BigDecimal.ZERO));
                
                // Mevcut bilgileri al
                ProductDetailDto current = builder.build();
                
                // Satış miktarını ekle
                int newTotalSold = current.getTotalSoldQuantity() + item.getQuantity();
                
                // Ürün için ciro hesaplama (price * quantity)
                BigDecimal itemRevenue = item.getPrice() != null ? 
                        item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())) : BigDecimal.ZERO;
                BigDecimal newRevenue = current.getRevenue().add(itemRevenue);
                
                // Ürün için maliyet ve brüt kar hesaplama
                BigDecimal itemCost = BigDecimal.ZERO;
                if (item.getCost() != null && item.getCost().compareTo(BigDecimal.ZERO) > 0) {
                    itemCost = item.getCost().multiply(BigDecimal.valueOf(item.getQuantity()));
                }
                BigDecimal itemGrossProfit = itemRevenue.subtract(itemCost);
                BigDecimal newGrossProfit = current.getGrossProfit().add(itemGrossProfit);
                
                // Builder'ı güncelle
                productMap.put(barcode, ProductDetailDto.builder()
                        .productName(productName)
                        .barcode(barcode)
                        .totalSoldQuantity(newTotalSold)
                        .returnQuantity(current.getReturnQuantity()) // İade miktarı aynı kalır bu döngüde
                        .revenue(newRevenue)
                        .grossProfit(newGrossProfit));
            }
        }
        
        // İade siparişlerinden iade miktarlarını ekle
        for (TrendyolOrder order : returnedOrders) {
            for (OrderItem item : order.getOrderItems()) {
                String barcode = item.getBarcode();
                String productName = item.getProductName();
                ProductDetailDto.ProductDetailDtoBuilder builder = productMap.get(barcode);
                
                if (builder != null) {
                    ProductDetailDto current = builder.build();
                    int newReturnQuantity = current.getReturnQuantity() + item.getQuantity();
                    
                    // Builder'ı güncelle
                    productMap.put(barcode, ProductDetailDto.builder()
                            .productName(productName)
                            .barcode(barcode)
                            .totalSoldQuantity(current.getTotalSoldQuantity())
                            .returnQuantity(newReturnQuantity)
                            .revenue(current.getRevenue())
                            .grossProfit(current.getGrossProfit()));
                }
            }
        }
        
        return productMap.values().stream()
                .map(ProductDetailDto.ProductDetailDtoBuilder::build)
                .toList();
    }
}
