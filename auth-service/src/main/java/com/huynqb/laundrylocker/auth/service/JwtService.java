package com.huynqb.laundrylocker.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${app.security.jwt.secret:laundry-locker-microservices-secret-key-change-me-please-32chars}")
  private String secret;

  @Value("${app.security.jwt.expiration-ms:86400000}")
  private long accessExpirationMs;

  @Value("${app.security.jwt.refresh-expiration-ms:2592000000}")
  private long refreshExpirationMs;

  private SecretKey key;

  @PostConstruct
  void init() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      keyBytes = (secret + "00000000000000000000000000000000").substring(0, 32).getBytes(StandardCharsets.UTF_8);
    }
    key = Keys.hmacShaKeyFor(keyBytes);
  }

  public TokenPair issue(Long accountId, Long userId, Set<String> roles) {
    Instant now = Instant.now();
    Instant accessExpiresAt = now.plusMillis(accessExpirationMs);
    Instant refreshExpiresAt = now.plusMillis(refreshExpirationMs);
    String accessToken = buildToken(accountId, userId, roles, "access", accessExpiresAt);
    String refreshToken = buildToken(accountId, userId, roles, "refresh", refreshExpiresAt);
    return new TokenPair(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  private String buildToken(
      Long accountId, Long userId, Set<String> roles, String tokenUse, Instant expiresAt) {
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("accountId", accountId)
        .claim("roles", roles)
        .claim("tokenUse", tokenUse)
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(expiresAt))
        .signWith(key)
        .compact();
  }

  public record TokenPair(
      String accessToken, String refreshToken, Instant accessExpiresAt, Instant refreshExpiresAt) {}
}
