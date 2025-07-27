package com.ecommerce.sellerx.stores;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class StoreService {
    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;
    private final com.ecommerce.sellerx.users.UserService userService;

    public List<StoreDto> getStoresByUser(com.ecommerce.sellerx.users.User user) {
        return storeRepository.findAllByUser(user)
                .stream()
                .map(storeMapper::toDto)
                .toList();
    }

    public List<StoreDto> getAllStores(String sortBy) {
        if (!List.of("storeName", "marketplace").contains(sortBy))
            sortBy = "storeName";
        return storeRepository.findAll(Sort.by(sortBy))
                .stream()
                .map(storeMapper::toDto)
                .toList();
    }

    public StoreDto getStore(UUID storeId) {
        var store = storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException("Store not found"));
        return storeMapper.toDto(store);
    }

    public StoreDto registerStore(RegisterStoreRequest request, com.ecommerce.sellerx.users.User user) {
        var store = storeMapper.toEntity(request);
        store.setUser(user);
        store.setCreatedAt(java.time.LocalDateTime.now());
        store.setUpdatedAt(java.time.LocalDateTime.now());
        storeRepository.save(store);
        return storeMapper.toDto(store);
    }

    public StoreDto updateStore(UUID storeId, UpdateStoreRequest request) {
        var store = storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException("Store not found"));
        storeMapper.update(request, store);
        store.setUpdatedAt(java.time.LocalDateTime.now());
        storeRepository.save(store);
        return storeMapper.toDto(store);
    }

    public StoreDto updateStoreByUser(UUID storeId, UpdateStoreRequest request, com.ecommerce.sellerx.users.User user) {
        var store = storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException("Store not found"));
        if (!store.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Store does not belong to user");
        }
        storeMapper.update(request, store);
        store.setUpdatedAt(java.time.LocalDateTime.now());
        storeRepository.save(store);
        return storeMapper.toDto(store);
    }

    @Transactional
    public void deleteStoreByUser(UUID storeId, com.ecommerce.sellerx.users.User user) {
        var store = storeRepository.findById(storeId).orElseThrow(() -> new StoreNotFoundException("Store not found"));
        if (!store.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Store does not belong to user");
        }
        
        // Eğer silinecek store kullanıcının seçili store'u ise, seçili store'u null yap
        if (user.getSelectedStoreId() != null && user.getSelectedStoreId().equals(storeId)) {
            userService.setSelectedStoreId(user.getId(), null);
        }
        
        storeRepository.deleteByIdAndUser(storeId, user);
    }

    public boolean isStoreOwnedByUser(UUID storeId, Long userId) {
        var store = storeRepository.findById(storeId).orElse(null);
        return store != null && store.getUser().getId().equals(userId);
    }

    public boolean isStoreOwnedByUser(String storeIdString, Long userId) {
        try {
            UUID storeId = UUID.fromString(storeIdString);
            return isStoreOwnedByUser(storeId, userId);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
