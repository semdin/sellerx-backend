package com.ecommerce.sellerx.dashboard;

import com.ecommerce.sellerx.auth.JwtService;
import com.ecommerce.sellerx.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController {
    private final JwtService jwtService;
    private final UserService userService;

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            UUID selectedStoreId = userService.getSelectedStoreId(userId);
            
            if (selectedStoreId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No store selected"));
            }
            
            // TODO: Dashboard verilerini sadece seçili store için getir
            // DashboardStats stats = dashboardService.getStatsByStore(selectedStoreId);
            
            // Detaylı mock veri döndürüyoruz (Trendyol benzeri dashboard)
            
            // Today details
            Map<String, Object> todayDetails = new HashMap<>();
            todayDetails.put("grossRevenue", 694.06);
            todayDetails.put("grossSalesCount", 54);
            todayDetails.put("netRevenue", 694.06);
            todayDetails.put("netSalesCount", 51);
            todayDetails.put("adExpenses", -4.31);
            
            // Kupon detayları
            Map<String, Object> couponDetails = new HashMap<>();
            couponDetails.put("total", 0.00);
            couponDetails.put("adSpending", -0.81);
            couponDetails.put("discount", 0.00);
            couponDetails.put("coupon", 0.00);
            todayDetails.put("couponDetails", couponDetails);
            
            todayDetails.put("productCost", -103.68);
            todayDetails.put("shippingCost", -25.50);
            todayDetails.put("returnCost", -12.00);
            todayDetails.put("otherExpenses", -8.00);
            
            // Yurt dışı operasyon detayları
            Map<String, Object> internationalDetails = new HashMap<>();
            internationalDetails.put("total", 0.00);
            internationalDetails.put("internationalServiceFee", 0.00);
            internationalDetails.put("internationalOperationFee", 0.00);
            internationalDetails.put("termDelayFee", 0.00);
            internationalDetails.put("platformServiceFee", 0.00);
            internationalDetails.put("invoiceCounterSalesFee", 0.00);
            internationalDetails.put("supplyFailureFee", 0.00);
            internationalDetails.put("azInternationalOperationFee", 0.00);
            internationalDetails.put("azPlatformServiceFee", 0.00);
            internationalDetails.put("eCommerceWithholdingExpense", 0.00);
            todayDetails.put("internationalDetails", internationalDetails);
            
            todayDetails.put("extraExpenses", -3.50);
            
            // Ambalaj detayları
            Map<String, Object> packagingDetails = new HashMap<>();
            packagingDetails.put("total", 1.00);
            packagingDetails.put("officeExpense", 0.00);
            packagingDetails.put("packaging", 1.00);
            packagingDetails.put("accountingEtc", 2.00);
            todayDetails.put("packagingDetails", packagingDetails);
            
            todayDetails.put("trendyolCommissionAmount", -104.11);
            todayDetails.put("vatDifference", -15.20);
            todayDetails.put("trendyolCommissionRate", 15.0);
            todayDetails.put("returnRate", 11.11);
            todayDetails.put("netProfit", 33.66);
            todayDetails.put("roi", 32.46);
            todayDetails.put("profitMargin", 4.85);
            
            // Yesterday details
            Map<String, Object> yesterdayDetails = new HashMap<>();
            yesterdayDetails.put("grossRevenue", 719.83);
            yesterdayDetails.put("grossSalesCount", 67);
            yesterdayDetails.put("netRevenue", 719.83);
            yesterdayDetails.put("netSalesCount", 64);
            yesterdayDetails.put("adExpenses", -9.54);
            
            // Kupon detayları
            Map<String, Object> yesterdayCouponDetails = new HashMap<>();
            yesterdayCouponDetails.put("total", 0.00);
            yesterdayCouponDetails.put("adSpending", -1.24);
            yesterdayCouponDetails.put("discount", 0.00);
            yesterdayCouponDetails.put("coupon", 0.00);
            yesterdayDetails.put("couponDetails", yesterdayCouponDetails);
            
            yesterdayDetails.put("productCost", -128.64);
            yesterdayDetails.put("shippingCost", -32.00);
            yesterdayDetails.put("returnCost", -14.00);
            yesterdayDetails.put("otherExpenses", -10.00);
            
            // Yurt dışı operasyon detayları
            Map<String, Object> yesterdayInternationalDetails = new HashMap<>();
            yesterdayInternationalDetails.put("total", 0.00);
            yesterdayInternationalDetails.put("internationalServiceFee", 0.00);
            yesterdayInternationalDetails.put("internationalOperationFee", 0.00);
            yesterdayInternationalDetails.put("termDelayFee", 0.00);
            yesterdayInternationalDetails.put("platformServiceFee", 0.00);
            yesterdayInternationalDetails.put("invoiceCounterSalesFee", 0.00);
            yesterdayInternationalDetails.put("supplyFailureFee", 0.00);
            yesterdayInternationalDetails.put("azInternationalOperationFee", 0.00);
            yesterdayInternationalDetails.put("azPlatformServiceFee", 0.00);
            yesterdayInternationalDetails.put("eCommerceWithholdingExpense", 0.00);
            yesterdayDetails.put("internationalDetails", yesterdayInternationalDetails);
            
            yesterdayDetails.put("extraExpenses", -4.50);
            
            // Ambalaj detayları
            Map<String, Object> yesterdayPackagingDetails = new HashMap<>();
            yesterdayPackagingDetails.put("total", 1.34);
            yesterdayPackagingDetails.put("officeExpense", 0.00);
            yesterdayPackagingDetails.put("packaging", 1.34);
            yesterdayPackagingDetails.put("accountingEtc", 2.67);
            yesterdayDetails.put("packagingDetails", yesterdayPackagingDetails);
            
            yesterdayDetails.put("trendyolCommissionAmount", -107.97);
            yesterdayDetails.put("vatDifference", -16.50);
            yesterdayDetails.put("trendyolCommissionRate", 15.0);
            yesterdayDetails.put("returnRate", 10.45);
            yesterdayDetails.put("netProfit", 49.41);
            yesterdayDetails.put("roi", 38.43);
            yesterdayDetails.put("profitMargin", 6.87);
            
            // Month to date details
            Map<String, Object> monthToDateDetails = new HashMap<>();
            monthToDateDetails.put("grossRevenue", 12441.03);
            monthToDateDetails.put("grossSalesCount", 1057);
            monthToDateDetails.put("netRevenue", 12441.03);
            monthToDateDetails.put("netSalesCount", 904);
            monthToDateDetails.put("adExpenses", -205.88);
            
            // Kupon detayları
            Map<String, Object> monthToDateCouponDetails = new HashMap<>();
            monthToDateCouponDetails.put("total", 0.00);
            monthToDateCouponDetails.put("adSpending", -18.65);
            monthToDateCouponDetails.put("discount", 0.00);
            monthToDateCouponDetails.put("coupon", 0.00);
            monthToDateDetails.put("couponDetails", monthToDateCouponDetails);
            
            monthToDateDetails.put("productCost", -2288.24);
            monthToDateDetails.put("shippingCost", -528.50);
            monthToDateDetails.put("returnCost", -260.00);
            monthToDateDetails.put("otherExpenses", -156.00);
            
            // Yurt dışı operasyon detayları
            Map<String, Object> monthToDateInternationalDetails = new HashMap<>();
            monthToDateInternationalDetails.put("total", 0.00);
            monthToDateInternationalDetails.put("internationalServiceFee", 0.00);
            monthToDateInternationalDetails.put("internationalOperationFee", 0.00);
            monthToDateInternationalDetails.put("termDelayFee", 0.00);
            monthToDateInternationalDetails.put("platformServiceFee", 0.00);
            monthToDateInternationalDetails.put("invoiceCounterSalesFee", 0.00);
            monthToDateInternationalDetails.put("supplyFailureFee", 0.00);
            monthToDateInternationalDetails.put("azInternationalOperationFee", 0.00);
            monthToDateInternationalDetails.put("azPlatformServiceFee", 0.00);
            monthToDateInternationalDetails.put("eCommerceWithholdingExpense", 0.00);
            monthToDateDetails.put("internationalDetails", monthToDateInternationalDetails);
            
            monthToDateDetails.put("extraExpenses", -73.50);
            
            // Ambalaj detayları
            Map<String, Object> monthToDatePackagingDetails = new HashMap<>();
            monthToDatePackagingDetails.put("total", 21.15);
            monthToDatePackagingDetails.put("officeExpense", 0.00);
            monthToDatePackagingDetails.put("packaging", 21.15);
            monthToDatePackagingDetails.put("accountingEtc", 42.30);
            monthToDateDetails.put("packagingDetails", monthToDatePackagingDetails);
            
            monthToDateDetails.put("trendyolCommissionAmount", -1866.15);
            monthToDateDetails.put("vatDifference", -288.15);
            monthToDateDetails.put("trendyolCommissionRate", 15.0);
            monthToDateDetails.put("returnRate", 5.75);
            monthToDateDetails.put("netProfit", 3677.32);
            monthToDateDetails.put("roi", 178.55);
            monthToDateDetails.put("profitMargin", 29.55);
            
            // Last month details
            Map<String, Object> lastMonthDetails = new HashMap<>();
            lastMonthDetails.put("grossRevenue", 25878.53);
            lastMonthDetails.put("grossSalesCount", 2278);
            lastMonthDetails.put("netRevenue", 25878.53);
            lastMonthDetails.put("netSalesCount", 1913);
            lastMonthDetails.put("adExpenses", -548.97);
            
            // Kupon detayları
            Map<String, Object> lastMonthCouponDetails = new HashMap<>();
            lastMonthCouponDetails.put("total", 0.00);
            lastMonthCouponDetails.put("adSpending", -49.72);
            lastMonthCouponDetails.put("discount", 0.00);
            lastMonthCouponDetails.put("coupon", 0.00);
            lastMonthDetails.put("couponDetails", lastMonthCouponDetails);
            
            lastMonthDetails.put("productCost", -4555.70);
            lastMonthDetails.put("shippingCost", -1139.00);
            lastMonthDetails.put("returnCost", -532.00);
            lastMonthDetails.put("otherExpenses", -342.00);
            
            // Yurt dışı operasyon detayları
            Map<String, Object> lastMonthInternationalDetails = new HashMap<>();
            lastMonthInternationalDetails.put("total", 0.00);
            lastMonthInternationalDetails.put("internationalServiceFee", 0.00);
            lastMonthInternationalDetails.put("internationalOperationFee", 0.00);
            lastMonthInternationalDetails.put("termDelayFee", 0.00);
            lastMonthInternationalDetails.put("platformServiceFee", 0.00);
            lastMonthInternationalDetails.put("invoiceCounterSalesFee", 0.00);
            lastMonthInternationalDetails.put("supplyFailureFee", 0.00);
            lastMonthInternationalDetails.put("azInternationalOperationFee", 0.00);
            lastMonthInternationalDetails.put("azPlatformServiceFee", 0.00);
            lastMonthInternationalDetails.put("eCommerceWithholdingExpense", 0.00);
            lastMonthDetails.put("internationalDetails", lastMonthInternationalDetails);
            
            lastMonthDetails.put("extraExpenses", -159.50);
            
            // Ambalaj detayları
            Map<String, Object> lastMonthPackagingDetails = new HashMap<>();
            lastMonthPackagingDetails.put("total", 45.56);
            lastMonthPackagingDetails.put("officeExpense", 0.00);
            lastMonthPackagingDetails.put("packaging", 45.56);
            lastMonthPackagingDetails.put("accountingEtc", 91.12);
            lastMonthDetails.put("packagingDetails", lastMonthPackagingDetails);
            
            lastMonthDetails.put("trendyolCommissionAmount", -3881.78);
            lastMonthDetails.put("vatDifference", -594.68);
            lastMonthDetails.put("trendyolCommissionRate", 15.0);
            lastMonthDetails.put("returnRate", 3.97);
            lastMonthDetails.put("netProfit", 8494.39);
            lastMonthDetails.put("roi", 328.23);
            lastMonthDetails.put("profitMargin", 32.82);
            
            
            // Ana stats map'i oluştur
            Map<String, Object> stats = new HashMap<>();
            stats.put("storeId", selectedStoreId.toString());
            
            // Today data
            Map<String, Object> todayData = new HashMap<>();
            todayData.put("date", "15 Aralık 2024");
            todayData.put("revenue", 694.06);
            todayData.put("currency", "TL");
            todayData.put("orders", 51);
            todayData.put("units", 54);
            todayData.put("returns", 6);
            todayData.put("adCost", 4.31);
            todayData.put("estimatedPayment", 430.43);
            todayData.put("grossProfit", 184.66);
            todayData.put("netProfit", 33.66);
            todayData.put("details", todayDetails);
            stats.put("today", todayData);
            
            // Yesterday data
            Map<String, Object> yesterdayData = new HashMap<>();
            yesterdayData.put("date", "14 Aralık 2024");
            yesterdayData.put("revenue", 719.83);
            yesterdayData.put("currency", "TL");
            yesterdayData.put("orders", 64);
            yesterdayData.put("units", 67);
            yesterdayData.put("returns", 7);
            yesterdayData.put("adCost", 9.54);
            yesterdayData.put("estimatedPayment", 403.90);
            yesterdayData.put("grossProfit", 74.41);
            yesterdayData.put("netProfit", 49.41);
            yesterdayData.put("details", yesterdayDetails);
            stats.put("yesterday", yesterdayData);
            
            // Month to date data
            Map<String, Object> monthToDateData = new HashMap<>();
            monthToDateData.put("date", "1-15 Aralık 2024");
            monthToDateData.put("revenue", 12441.03);
            monthToDateData.put("currency", "TL");
            monthToDateData.put("orders", 904);
            monthToDateData.put("units", 1057);
            monthToDateData.put("returns", 52);
            monthToDateData.put("adCost", 205.88);
            monthToDateData.put("estimatedPayment", 7810.74);
            monthToDateData.put("grossProfit", 4151.32);
            monthToDateData.put("netProfit", 3677.32);
            monthToDateData.put("details", monthToDateDetails);
            stats.put("monthToDate", monthToDateData);
            
            // Last month data
            Map<String, Object> lastMonthData = new HashMap<>();
            lastMonthData.put("date", "1-30 Kasım 2024");
            lastMonthData.put("revenue", 25878.53);
            lastMonthData.put("currency", "TL");
            lastMonthData.put("orders", 1913);
            lastMonthData.put("units", 2278);
            lastMonthData.put("returns", 76);
            lastMonthData.put("adCost", 548.97);
            lastMonthData.put("estimatedPayment", 15390.42);
            lastMonthData.put("grossProfit", 8803.39);
            lastMonthData.put("netProfit", 8494.39);
            lastMonthData.put("details", lastMonthDetails);
            stats.put("lastMonth", lastMonthData);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }
}
