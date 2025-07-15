package com.ecommerce.sellerx.stores;

import com.ecommerce.sellerx.users.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserRepository;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/stores")
public class StoreController {
    private final StoreService storeService;
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/my")
    public Iterable<StoreDto> getMyStores() {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);
        return storeService.getStoresByUser(user);
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Iterable<StoreDto> getAllStores(@RequestParam(required = false, defaultValue = "", name = "sort") String sortBy) {
        return storeService.getAllStores(sortBy);
    }

    @GetMapping("/{id}")
    public StoreDto getStore(@PathVariable UUID id) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);
        StoreDto storeDto = storeService.getStore(id);
        if (!storeDto.getUserId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bu store'a erişim yetkiniz yok.");
        }
        return storeDto;
    }

    @PostMapping
    public ResponseEntity<?> registerStore(@Valid @RequestBody RegisterStoreRequest request, UriComponentsBuilder uriBuilder) {
        // UserId yerine token'dan userId al
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);
        var storeDto = storeService.registerStore(request, user);
        
        // Eğer kullanıcının hiç seçili store'u yoksa, yeni oluşturulan store'u seçili yap
        if (userService.getSelectedStoreId(userId) == null) {
            userService.setSelectedStoreId(userId, storeDto.getId());
        }
        
        var uri = uriBuilder.path("/stores/{id}").buildAndExpand(storeDto.getId()).toUri();
        return ResponseEntity.created(uri).body(storeDto);
    }

    @PutMapping("/{id}")
    public StoreDto updateStore(@PathVariable UUID id, @RequestBody UpdateStoreRequest request) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);
        return storeService.updateStoreByUser(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable UUID id) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(com.ecommerce.sellerx.users.UserNotFoundException::new);
        storeService.deleteStoreByUser(id, user);
        return ResponseEntity.noContent().build();
    }
}
