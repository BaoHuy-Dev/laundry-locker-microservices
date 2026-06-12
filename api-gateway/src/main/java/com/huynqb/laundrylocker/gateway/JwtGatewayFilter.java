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
    // Service-to-service endpoints are reachable only inside the cluster
    // (Feign via Eureka); never through the public gateway.
    if (path.startsWith("/internal")) {
      exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
      return exchange.getResponse().setComplete();
    }
    if (isPublic(path, exchange.getRequest().getMethod())) {
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
      boolean mutatingLockerStructure =
          !org.springframework.http.HttpMethod.GET.equals(exchange.getRequest().getMethod())
              && (path.startsWith("/api/lockers") || path.startsWith("/api/boxes"))
              && !isCustomerLockerAction(path);
      if (!hasRequiredRole(path, roles)
          || (mutatingLockerStructure && !hasAny(roles, "ADMIN", "MANAGER"))) {
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

  // Path-prefix RBAC. ADMIN is a superset of every operational role.
  private boolean hasRequiredRole(String path, List<String> roles) {
    if (path.startsWith("/api/admin")) {
      return hasAny(roles, "ADMIN");
    }
    if (path.startsWith("/api/manage")) {
      return hasAny(roles, "MANAGER", "ADMIN");
    }
    if (path.startsWith("/api/maintenance")) {
      return hasAny(roles, "MAINTENANCE", "ADMIN");
    }
    return true;
  }

  // Cabinet/box structure changes are operator work; customers only report
  // faults, file reports, or trigger an open on their own cell.
  private boolean isCustomerLockerAction(String path) {
    return path.endsWith("/fault") || path.endsWith("/report") || path.endsWith("/open");
  }

  private boolean hasAny(List<String> roles, String... required) {
    if (roles == null) {
      return false;
    }
    for (String role : required) {
      if (roles.contains(role)) {
        return true;
      }
    }
    return false;
  }

  private boolean isPublic(String path, org.springframework.http.HttpMethod method) {
    if (path.startsWith("/api/auth")
        || path.startsWith("/api/admin/auth")
        || path.equals("/")
        || path.startsWith("/ws")
        || path.startsWith("/actuator")
        || path.startsWith("/api/payments/vnpay")
        || path.startsWith("/api/payments/momo")) {
      return true;
    }
    // Catalogue browsing is anonymous; any mutation requires a JWT.
    boolean readOnly = org.springframework.http.HttpMethod.GET.equals(method);
    return readOnly
        && (path.startsWith("/api/stores")
            || path.startsWith("/api/lockers")
            || path.startsWith("/api/services")
            || path.startsWith("/api/laundry-services")
            || path.startsWith("/api/promotions"));
  }

  @Override
  public int getOrder() {
    return -100;
  }
}
