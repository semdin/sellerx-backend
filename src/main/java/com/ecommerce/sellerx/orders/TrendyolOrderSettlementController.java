package com.ecommerce.sellerx.orders;

import com.ecommerce.sellerx.auth.JwtService;
import com.ecommerce.sellerx.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
public class TrendyolOrderSettlementController {

    private final TrendyolSettlementService settlementService;
    private final JwtService jwtService;
    private final UserService userService;

    /**
     * Sync settlements for the current user's selected store
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncSettlements(HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            UUID selectedStoreId = userService.getSelectedStoreId(userId);
            
            if (selectedStoreId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No store selected"));
            }
            
            log.info("Starting settlement sync for store: {} by user: {}", selectedStoreId, userId);
            
            settlementService.fetchAndUpdateSettlementsForStore(selectedStoreId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Settlement sync completed successfully",
                "storeId", selectedStoreId.toString()
            ));
            
        } catch (Exception e) {
            log.error("Failed to sync settlements", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to sync settlements: " + e.getMessage()));
        }
    }

    /**
     * Sync settlements for a specific store (admin only)
     */
    @PostMapping("/sync/{storeId}")
    public ResponseEntity<?> syncSettlementsForStore(@PathVariable UUID storeId, HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            log.info("Starting settlement sync for store: {} by user: {}", storeId, userId);
            
            settlementService.fetchAndUpdateSettlementsForStore(storeId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Settlement sync completed successfully",
                "storeId", storeId.toString()
            ));
            
        } catch (Exception e) {
            log.error("Failed to sync settlements for store: {}", storeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to sync settlements: " + e.getMessage()));
        }
    }

    /**
     * Sync settlements for all Trendyol stores (admin only)
     */
    @PostMapping("/sync-all")
    public ResponseEntity<?> syncAllSettlements(HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            log.info("Starting settlement sync for all stores by user: {}", userId);
            
            settlementService.fetchAndUpdateSettlementsForAllStores();
            
            return ResponseEntity.ok(Map.of(
                "message", "Settlement sync completed successfully for all stores"
            ));
            
        } catch (Exception e) {
            log.error("Failed to sync settlements for all stores", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to sync settlements: " + e.getMessage()));
        }
    }

    /**
     * Get settlement statistics for current user's selected store
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSettlementStats(HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            UUID selectedStoreId = userService.getSelectedStoreId(userId);
            
            if (selectedStoreId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No store selected"));
            }
            
            Map<String, Object> stats = settlementService.getSettlementStats(selectedStoreId);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Failed to get settlement stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get settlement stats: " + e.getMessage()));
        }
    }

    /**
     * Get settlement statistics for a specific store
     */
    @GetMapping("/stats/{storeId}")
    public ResponseEntity<?> getSettlementStatsForStore(@PathVariable UUID storeId, HttpServletRequest request) {
        try {
            Long userId = jwtService.getUserIdFromToken(request);
            log.info("Getting settlement stats for store: {} by user: {}", storeId, userId);
            
            Map<String, Object> stats = settlementService.getSettlementStats(storeId);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Failed to get settlement stats for store: {}", storeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get settlement stats: " + e.getMessage()));
        }
    }
}
