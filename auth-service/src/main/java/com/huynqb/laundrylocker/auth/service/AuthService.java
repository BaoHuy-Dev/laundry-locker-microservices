package com.huynqb.laundrylocker.auth.service;

import com.huynqb.laundrylocker.auth.dto.AuthResponse;
import com.huynqb.laundrylocker.auth.dto.LoginRequest;
import com.huynqb.laundrylocker.auth.dto.LogoutRequest;
import com.huynqb.laundrylocker.auth.dto.RefreshTokenRequest;
import com.huynqb.laundrylocker.auth.dto.RegisterRequest;
import com.huynqb.laundrylocker.auth.dto.UserProvisionRequest;
import com.huynqb.laundrylocker.auth.client.UserClient;
import com.huynqb.laundrylocker.auth.model.AuthAccount;
import com.huynqb.laundrylocker.auth.model.RefreshToken;
import com.huynqb.laundrylocker.auth.repository.AuthAccountRepository;
import com.huynqb.laundrylocker.auth.repository.RefreshTokenRepository;
import com.huynqb.laundrylocker.common.dto.UserSummary;
import com.huynqb.laundrylocker.common.exception.BusinessException;
import com.huynqb.laundrylocker.common.exception.NotFoundException;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthAccountRepository authAccountRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final UserClient userClient;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    Long userId = request.userId();
    UserSummary user =
        userId == null
            ? userClient
                .provisionUser(
                    new UserProvisionRequest(
                        request.email(),
                        request.phoneNumber(),
                        request.firstName(),
                        request.lastName(),
                        "ACTIVE",
                        defaultRoles(request.roles())))
                .data()
            : userClient.getUser(userId).data();

    AuthAccount account = new AuthAccount();
    account.setUserId(user.id());
    account.setEmail(StringUtils.hasText(request.email()) ? request.email() : user.email());
    account.setPhoneNumber(
        StringUtils.hasText(request.phoneNumber()) ? request.phoneNumber() : user.phoneNumber());
    account.setPasswordHash(passwordEncoder.encode(request.password()));
    return issue(authAccountRepository.save(account), user.roles());
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    AuthAccount account =
        authAccountRepository
            .findByEmail(request.identifier())
            .or(() -> authAccountRepository.findByPhoneNumber(request.identifier()))
            .orElseThrow(() -> new BusinessException("AUTH_INVALID", "Invalid credentials"));
    if (!"ACTIVE".equalsIgnoreCase(account.getStatus())
        || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
      throw new BusinessException("AUTH_INVALID", "Invalid credentials");
    }
    account.setLastLoginAt(LocalDateTime.now());
    UserSummary user = userClient.getUser(account.getUserId()).data();
    return issue(account, user.roles());
  }

  @Transactional
  public AuthResponse refresh(RefreshTokenRequest request) {
    Claims claims = jwtService.parse(request.refreshToken());
    if (!"refresh".equals(claims.get("tokenUse", String.class))) {
      throw new BusinessException("AUTH_INVALID_REFRESH", "Refresh token is invalid");
    }
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHashAndRevokedFalse(hash(request.refreshToken()))
            .orElseThrow(() -> new BusinessException("AUTH_INVALID_REFRESH", "Refresh token is invalid"));
    if (stored.getExpiresAt().isBefore(Instant.now())) {
      stored.setRevoked(true);
      throw new BusinessException("AUTH_REFRESH_EXPIRED", "Refresh token expired");
    }
    AuthAccount account = findAccount(stored.getAccountId());
    UserSummary user = userClient.getUser(account.getUserId()).data();
    stored.setRevoked(true);
    return issue(account, user.roles());
  }

  @Transactional
  public void logout(LogoutRequest request) {
    refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(request.refreshToken())).ifPresent(token -> token.setRevoked(true));
  }

  @Transactional(readOnly = true)
  public AuthResponse getAccount(Long id) {
    AuthAccount account = findAccount(id);
    UserSummary user = userClient.getUser(account.getUserId()).data();
    return new AuthResponse(account.getId(), account.getUserId(), null, null, "Bearer", null, user.roles());
  }

  private AuthAccount findAccount(Long id) {
    return authAccountRepository.findById(id).orElseThrow(() -> new NotFoundException("AuthAccount", id));
  }

  private AuthResponse issue(AuthAccount account, Set<String> roles) {
    Set<String> effectiveRoles = defaultRoles(roles);
    JwtService.TokenPair tokenPair = jwtService.issue(account.getId(), account.getUserId(), effectiveRoles);
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setAccountId(account.getId());
    refreshToken.setTokenHash(hash(tokenPair.refreshToken()));
    refreshToken.setExpiresAt(tokenPair.refreshExpiresAt());
    refreshTokenRepository.save(refreshToken);
    return new AuthResponse(
        account.getId(),
        account.getUserId(),
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        "Bearer",
        tokenPair.accessExpiresAt(),
        effectiveRoles);
  }

  private Set<String> defaultRoles(Set<String> roles) {
    return roles == null || roles.isEmpty() ? Set.of("USER") : roles;
  }

  private String hash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Could not hash token", ex);
    }
  }
}
