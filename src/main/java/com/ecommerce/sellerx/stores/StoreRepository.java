package com.ecommerce.sellerx.stores;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import java.util.List;
import com.ecommerce.sellerx.users.User;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    List<Store> findAllByUser(User user);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Store s WHERE s.id = :id AND s.user = :user")
    void deleteByIdAndUser(@Param("id") UUID id, @Param("user") User user);
}
