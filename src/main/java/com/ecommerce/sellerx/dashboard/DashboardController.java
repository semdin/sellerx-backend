package com.ecommerce.sellerx.dashboard;

import com.ecommerce.sellerx.auth.JwtService;
import com.ecommerce.sellerx.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            
            // Şimdilik örnek veri döndürüyoruz
            var stats = Map.of(
                "storeId", selectedStoreId.toString(),
                "totalOrders", 150,
                "totalRevenue", 45000.0,
                "pendingOrders", 12,
                "lowStockProducts", 5
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }
}
