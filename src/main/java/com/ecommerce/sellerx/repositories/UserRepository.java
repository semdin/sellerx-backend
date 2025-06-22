package com.ecommerce.sellerx.repositories;

import com.ecommerce.sellerx.entities.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
