package com.ecommerce.sellerx.repositories;

import com.ecommerce.sellerx.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
