package com.huynqb.laundrylocker.auth.controller;

import com.huynqb.laundrylocker.auth.dto.AuthResponse;
import com.huynqb.laundrylocker.auth.dto.LoginRequest;
import com.huynqb.laundrylocker.auth.dto.LogoutRequest;
import com.huynqb.laundrylocker.auth.dto.RefreshTokenRequest;
import com.huynqb.laundrylocker.auth.dto.RegisterRequest;
import com.huynqb.laundrylocker.auth.service.AuthService;
import com.huynqb.laundrylocker.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/api/auth/register")
  public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.ok("AUTH_REGISTERED", "Account registered", authService.register(request));
  }

  @PostMapping("/api/auth/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok("AUTH_LOGIN_OK", "Login successful", authService.login(request));
  }

  @PostMapping("/api/auth/refresh-token")
  public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.ok("AUTH_REFRESH_OK", "Token refreshed", authService.refresh(request));
  }

  @PostMapping("/api/auth/logout")
  public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request);
    return ApiResponse.ok("AUTH_LOGOUT_OK", "Logged out");
  }

  @GetMapping("/internal/auth/accounts/{id}")
  public ApiResponse<AuthResponse> getAccount(@PathVariable Long id) {
    return ApiResponse.ok(authService.getAccount(id));
  }
}
