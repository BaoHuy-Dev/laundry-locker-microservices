package com.huynqb.laundrylocker.notification.config;

import com.huynqb.laundrylocker.common.security.SecuritySecrets;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.security.Principal;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Value("${app.security.jwt.secret:laundry-locker-microservices-secret-key-change-me-please-32chars}")
  private String secret;

  private final Environment environment;
  private SecretKey key;

  @PostConstruct
  void init() {
    key =
        SecuritySecrets.hmacShaKeyFor(
            secret, "app.security.jwt.secret", environment.getActiveProfiles());
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic", "/queue");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        new ChannelInterceptor() {
          @Override
          public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
              accessor.setUser(authenticate(accessor));
            }
            return message;
          }
        });
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
  }

  private Principal authenticate(StompHeaderAccessor accessor) {
    String authHeader = firstHeader(accessor, "Authorization");
    if (authHeader == null) {
      authHeader = firstHeader(accessor, "authorization");
    }
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new MessagingException("Missing STOMP bearer token");
    }

    Claims claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(authHeader.substring(7)).getPayload();
    if (!"access".equals(claims.get("tokenUse", String.class))) {
      throw new MessagingException("STOMP bearer token must be an access token");
    }
    return () -> claims.getSubject();
  }

  private String firstHeader(StompHeaderAccessor accessor, String name) {
    String value = accessor.getFirstNativeHeader(name);
    return value == null || value.isBlank() ? null : value.trim();
  }
}
