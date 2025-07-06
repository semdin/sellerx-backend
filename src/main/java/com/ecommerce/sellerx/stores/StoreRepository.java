package com.ecommerce.sellerx.stores;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.util.List;
import com.ecommerce.sellerx.users.User;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    List<Store> findAllByUser(User user);
    void deleteByIdAndUser(UUID id, User user);
}
