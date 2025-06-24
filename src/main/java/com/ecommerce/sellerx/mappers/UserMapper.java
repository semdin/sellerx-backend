package com.ecommerce.sellerx.mappers;

import com.ecommerce.sellerx.dtos.RegisterUserRequest;
import com.ecommerce.sellerx.dtos.UserDto;
import com.ecommerce.sellerx.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    //@Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    UserDto userToUserDto(User user);
    User userDtoToUser(RegisterUserRequest request);
}
