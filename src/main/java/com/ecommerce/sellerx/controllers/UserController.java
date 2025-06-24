package com.ecommerce.sellerx.controllers;

import com.ecommerce.sellerx.dtos.RegisterUserRequest;
import com.ecommerce.sellerx.dtos.UserDto;
import com.ecommerce.sellerx.entities.User;
import com.ecommerce.sellerx.mappers.UserMapper;
import com.ecommerce.sellerx.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;
import java.util.stream.Collectors;


@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @GetMapping
    // method: GET
    public Iterable<UserDto> getAllUsers(
            @RequestHeader(name = "x-auth-token") String authToken,
            @RequestParam(required = false, defaultValue = "") String sort
    ) {

        System.out.println(authToken);

        if(!Set.of("name", "email").contains(sort)) {
            sort =  "name";
        }

        return userRepository.findAll(Sort.by(sort))
                .stream()
                .map(userMapper::userToUserDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        var user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(userMapper.userToUserDto(user));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @RequestBody RegisterUserRequest request,
            UriComponentsBuilder uriBuilder
    ) {

        var user = userMapper.userDtoToUser(request);
        userRepository.save(user);
        var userDto =  userMapper.userToUserDto(user);
        var uri  = uriBuilder.path("/users/{id}").buildAndExpand(userDto.getId()).toUri();
        return ResponseEntity.created(uri).body(userDto);
    }
}
