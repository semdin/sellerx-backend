package com.ecommerce.sellerx.stores;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    // Ek sorgular eklenebilir
}
