package com.huynqb.laundrylocker.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

  @Value("${app.security.jwt.secret:laundry-locker-microservices-secret-key-change-me-please-32chars}")
  private String secret;

  private SecretKey key;

  @PostConstruct
  void init() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      keyBytes = (secret + "00000000000000000000000000000000").substring(0, 32).getBytes(StandardCharsets.UTF_8);
    }
    key = Keys.hmacShaKeyFor(keyBytes);
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    if (isPublic(path)) {
      return enrichIfPresent(exchange, chain);
    }

    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }

    try {
      Claims claims = parse(authHeader.substring(7));
      List<String> roles = claims.get("roles", List.class);
      if (path.startsWith("/api/admin") && (roles == null || !roles.contains("ADMIN"))) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
      }
      return chain.filter(withUserHeaders(exchange, claims, roles));
    } catch (Exception ex) {
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }
  }

  private Mono<Void> enrichIfPresent(ServerWebExchange exchange, GatewayFilterChain chain) {
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return chain.filter(exchange);
    }
    try {
      Claims claims = parse(authHeader.substring(7));
      return chain.filter(withUserHeaders(exchange, claims, claims.get("roles", List.class)));
    } catch (Exception ex) {
      return chain.filter(exchange);
    }
  }

  private ServerWebExchange withUserHeaders(
      ServerWebExchange exchange, Claims claims, List<String> roles) {
    ServerHttpRequest request =
        exchange
            .getRequest()
            .mutate()
            .header("X-User-Id", claims.getSubject())
            .header("X-Account-Id", String.valueOf(claims.get("accountId")))
            .header("X-User-Roles", roles == null ? "" : String.join(",", roles))
            .build();
    return exchange.mutate().request(request).build();
  }

  private Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  private boolean isPublic(String path) {
    return path.startsWith("/api/auth")
        || path.startsWith("/api/admin/auth")
        || path.equals("/")
        || path.startsWith("/ws")
        || path.startsWith("/actuator")
        || path.startsWith("/api/stores")
        || path.startsWith("/api/lockers")
        || path.startsWith("/api/services")
        || path.startsWith("/api/laundry-services")
        || path.startsWith("/api/payments/vnpay")
        || path.startsWith("/api/payments/momo")
        || path.startsWith("/api/promotions")
        || path.startsWith("/internal");
  }

  @Override
  public int getOrder() {
    return -100;
  }
}
