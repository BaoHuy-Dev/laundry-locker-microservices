package com.huynqb.laundrylocker.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class JwtGatewayFilterTest {

  private static final String SECRET = "test-jwt-secret-at-least-32-bytes-long";

  private JwtGatewayFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtGatewayFilter(new MockEnvironment());
    ReflectionTestUtils.setField(filter, "secret", SECRET);
    filter.init();
  }

  @Test
  void blocksInternalEndpointsBeforeAuthentication() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/internal/orders/123").build());
    AtomicBoolean chainCalled = new AtomicBoolean(false);

    filter.filter(exchange, chainThatMarks(chainCalled)).block();

    assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    assertFalse(chainCalled.get());
  }

  @Test
  void rejectsRefreshTokenForBusinessApi() {
    MockServerWebExchange exchange =
        exchangeWithBearer(MockServerHttpRequest.get("/api/orders/my-orders"), token("refresh", "CUSTOMER"));
    AtomicBoolean chainCalled = new AtomicBoolean(false);

    filter.filter(exchange, chainThatMarks(chainCalled)).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    assertFalse(chainCalled.get());
  }

  @Test
  void requiresAdminForAdminApi() {
    MockServerWebExchange exchange =
        exchangeWithBearer(MockServerHttpRequest.get("/api/admin/orders"), token("access", "MANAGER"));
    AtomicBoolean chainCalled = new AtomicBoolean(false);

    filter.filter(exchange, chainThatMarks(chainCalled)).block();

    assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    assertFalse(chainCalled.get());
  }

  @Test
  void allowsManagerForManageApiAndForwardsIdentityHeaders() {
    MockServerWebExchange exchange =
        exchangeWithBearer(MockServerHttpRequest.get("/api/manage/orders"), token("access", "MANAGER"));
    AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

    filter.filter(exchange, captureExchange(forwarded)).block();

    assertEquals("42", forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"));
    assertEquals("7", forwarded.get().getRequest().getHeaders().getFirst("X-Account-Id"));
    assertEquals("MANAGER", forwarded.get().getRequest().getHeaders().getFirst("X-User-Roles"));
  }

  @Test
  void allowsMaintenanceForMaintenanceApi() {
    MockServerWebExchange exchange =
        exchangeWithBearer(
            MockServerHttpRequest.get("/api/maintenance/reports"), token("access", "MAINTENANCE"));
    AtomicBoolean chainCalled = new AtomicBoolean(false);

    filter.filter(exchange, chainThatMarks(chainCalled)).block();

    assertTrue(chainCalled.get());
  }

  @Test
  void keepsOpenApiAndSwaggerUiPublic() {
    assertPublicGet("/v3/api-docs");
    assertPublicGet("/v3/api-docs/order-service");
    assertPublicGet("/swagger-ui/index.html");
  }

  @Test
  void keepsCatalogueGetPublicButRequiresJwtForCatalogueMutation() {
    assertPublicGet("/api/lockers/2/layout");

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.post("/api/lockers").build());
    AtomicBoolean chainCalled = new AtomicBoolean(false);

    filter.filter(exchange, chainThatMarks(chainCalled)).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    assertFalse(chainCalled.get());
  }

  private void assertPublicGet(String path) {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    AtomicBoolean chainCalled = new AtomicBoolean(false);

    filter.filter(exchange, chainThatMarks(chainCalled)).block();

    assertTrue(chainCalled.get());
  }

  private MockServerWebExchange exchangeWithBearer(MockServerHttpRequest.BaseBuilder<?> request, String token) {
    return MockServerWebExchange.from(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());
  }

  private GatewayFilterChain chainThatMarks(AtomicBoolean called) {
    return exchange -> {
      called.set(true);
      return Mono.empty();
    };
  }

  private GatewayFilterChain captureExchange(AtomicReference<ServerWebExchange> forwarded) {
    return exchange -> {
      forwarded.set(exchange);
      return Mono.empty();
    };
  }

  private String token(String tokenUse, String... roles) {
    Instant now = Instant.now();
    SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .subject("42")
        .claim("accountId", 7L)
        .claim("roles", List.of(roles))
        .claim("tokenUse", tokenUse)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(3600)))
        .signWith(key)
        .compact();
  }
}
