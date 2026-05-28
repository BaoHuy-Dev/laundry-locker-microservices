package com.huynqb.laundrylocker.user.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.common.dto.UserSummary;
import com.huynqb.laundrylocker.user.dto.UserProfileRequest;
import com.huynqb.laundrylocker.user.service.UserProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

  private final UserProfileService userProfileService;

  @PostMapping("/api/users")
  public ApiResponse<UserSummary> create(@RequestBody UserProfileRequest request) {
    return ApiResponse.ok("USER_CREATED", "User created", userProfileService.create(request));
  }

  @PostMapping("/api/user")
  public ApiResponse<UserSummary> createLegacy(@RequestBody UserProfileRequest request) {
    return create(request);
  }

  @PutMapping("/api/users/{id}")
  public ApiResponse<UserSummary> update(@PathVariable Long id, @RequestBody UserProfileRequest request) {
    return ApiResponse.ok("USER_UPDATED", "User updated", userProfileService.update(id, request));
  }

  @GetMapping("/api/users/{id}")
  public ApiResponse<UserSummary> get(@PathVariable Long id) {
    return ApiResponse.ok(userProfileService.get(id));
  }

  @GetMapping("/api/user/{id}")
  public ApiResponse<UserSummary> getLegacy(@PathVariable Long id) {
    return get(id);
  }

  @GetMapping("/api/users")
  public ApiResponse<List<UserSummary>> list() {
    return ApiResponse.ok(userProfileService.list());
  }

  @GetMapping("/api/user/read")
  public ApiResponse<List<UserSummary>> listLegacy() {
    return list();
  }

  @PostMapping("/internal/users")
  public ApiResponse<UserSummary> provision(@RequestBody UserProfileRequest request) {
    return ApiResponse.ok("USER_PROVISIONED", "User provisioned", userProfileService.create(request));
  }

  @GetMapping("/internal/users/{id}")
  public ApiResponse<UserSummary> getInternal(@PathVariable Long id) {
    return ApiResponse.ok(userProfileService.get(id));
  }
}
