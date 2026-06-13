package com.huynqb.laundrylocker.gateway;

import com.huynqb.laundrylocker.common.security.SecuritySecrets;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
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

  private final Environment environment;
  private SecretKey key;

  public JwtGatewayFilter(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  void init() {
    key =
        SecuritySecrets.hmacShaKeyFor(
            secret, "app.security.jwt.secret", environment.getActiveProfiles());
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
      if (!isAccessToken(claims)) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
      }
      List<String> roles = extractRoles(claims);
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
      if (!isAccessToken(claims)) {
        return chain.filter(exchange);
      }
      return chain.filter(withUserHeaders(exchange, claims, extractRoles(claims)));
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

  private boolean isAccessToken(Claims claims) {
    return "access".equals(claims.get("tokenUse", String.class));
  }

  private List<String> extractRoles(Claims claims) {
    Object value = claims.get("roles");
    if (value instanceof List<?> list) {
      return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
    if (value instanceof String roles && !roles.isBlank()) {
      return Arrays.stream(roles.split(",")).map(String::trim).filter(role -> !role.isBlank()).toList();
    }
    return Collections.emptyList();
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
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/swagger-ui")
        || path.equals("/swagger-ui.html")
        || path.startsWith("/webjars")
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
