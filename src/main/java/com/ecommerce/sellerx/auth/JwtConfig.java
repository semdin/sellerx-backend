package com.ecommerce.sellerx.auth;

import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
@ConfigurationProperties(prefix = "spring.jwt")
@Data
public class JwtConfig {
    private String secret;
    private int accessTokenExpiration;
    private int refreshTokenExpiration;

    public SecretKey getSecretKey() {
        System.out.println("Key byte length: " + secret.getBytes().length);
        System.out.println("Secret key: " + new String(secret));
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}

