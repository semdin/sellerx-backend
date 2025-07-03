package com.ecommerce.sellerx.stores;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class StoreService {
    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;

    public List<StoreDto> getAllStores(String sortBy) {
        if (!List.of("storeName", "marketplace").contains(sortBy))
            sortBy = "storeName";
        return storeRepository.findAll(Sort.by(sortBy))
                .stream()
                .map(storeMapper::toDto)
                .toList();
    }

    public StoreDto getStore(UUID storeId) {
        var store = storeRepository.findById(storeId).orElseThrow(StoreNotFoundException::new);
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
        var store = storeRepository.findById(storeId).orElseThrow(StoreNotFoundException::new);
        storeMapper.update(request, store);
        store.setUpdatedAt(java.time.LocalDateTime.now());
        storeRepository.save(store);
        return storeMapper.toDto(store);
    }

    public void deleteStore(UUID storeId) {
        var store = storeRepository.findById(storeId).orElseThrow(StoreNotFoundException::new);
        storeRepository.delete(store);
    }
}
