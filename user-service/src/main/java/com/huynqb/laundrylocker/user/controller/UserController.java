package com.huynqb.laundrylocker.user.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.common.dto.UserSummary;
import com.huynqb.laundrylocker.user.client.AuthClient;
import com.huynqb.laundrylocker.user.client.NotificationClient;
import com.huynqb.laundrylocker.user.dto.UserProfileRequest;
import com.huynqb.laundrylocker.user.model.AuditLog;
import com.huynqb.laundrylocker.user.repository.AuditLogRepository;
import com.huynqb.laundrylocker.user.service.UserProfileService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

  private final UserProfileService userProfileService;
  private final AuthClient authClient;
  private final NotificationClient notificationClient;
  private final AuditLogRepository auditLogRepository;

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

  @GetMapping("/api/user/profile")
  public ApiResponse<UserSummary> profile(@RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok("USER_PROFILE_OK", "User profile", userProfileService.get(userId));
  }

  @PutMapping("/api/user/profile")
  public ApiResponse<UserSummary> updateProfile(
      @RequestHeader("X-User-Id") Long userId, @RequestBody UserProfileRequest request) {
    return ApiResponse.ok("PROFILE_UPDATED", "Profile updated", userProfileService.update(userId, request));
  }

  @PutMapping("/api/user/avatar")
  public ApiResponse<UserSummary> updateAvatar(
      @RequestHeader("X-User-Id") Long userId, @RequestBody Map<String, Object> request) {
    String imageUrl =
        request.get("imageUrl") == null ? String.valueOf(request.get("avatar")) : String.valueOf(request.get("imageUrl"));
    return ApiResponse.ok("AVATAR_UPDATED", "Avatar updated", userProfileService.updateAvatar(userId, imageUrl));
  }

  @PutMapping("/api/user/password")
  public ApiResponse<Void> changePassword(
      @RequestHeader("X-User-Id") Long userId, @RequestBody Map<String, Object> request) {
    authClient.changePassword(userId, request);
    return ApiResponse.ok("PASSWORD_CHANGED", "Password changed successfully");
  }

  @GetMapping("/api/user/me/statistics")
  public ApiResponse<Map<String, Object>> statistics(@RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok("USER_STATISTICS_RETRIEVED", "User statistics", userProfileService.statistics(userId));
  }

  @GetMapping("/api/user/dashboard")
  public ApiResponse<String> dashboard() {
    return ApiResponse.ok("Admin Dashboard - Access Granted");
  }

  @PostMapping("/api/user/fcm-token")
  public ApiResponse<Void> registerFcmToken(
      @RequestHeader("X-User-Id") Long userId, @RequestBody Map<String, Object> request) {
    Map<String, Object> payload = new HashMap<>(request);
    payload.put("userId", userId);
    notificationClient.saveFcmToken(payload);
    return ApiResponse.ok("FCM_TOKEN_REGISTERED", "FCM token registered");
  }

  @DeleteMapping("/api/user/fcm-token")
  public ApiResponse<Void> removeFcmToken(
      @RequestHeader("X-User-Id") Long userId, @RequestParam String fcmToken) {
    notificationClient.deleteFcmToken(Map.of("userId", userId, "token", fcmToken));
    return ApiResponse.ok("FCM_TOKEN_REMOVED", "FCM token removed");
  }

  @GetMapping("/api/admin/users")
  public ApiResponse<List<UserSummary>> adminUsers() {
    return list();
  }

  @PostMapping("/api/admin/users")
  public ApiResponse<UserSummary> adminCreate(@RequestBody UserProfileRequest request) {
    return create(request);
  }

  @GetMapping("/api/admin/users/{id}")
  public ApiResponse<UserSummary> adminGet(@PathVariable Long id) {
    return get(id);
  }

  @PutMapping("/api/admin/users/{id}")
  public ApiResponse<UserSummary> adminUpdate(@PathVariable Long id, @RequestBody UserProfileRequest request) {
    return update(id, request);
  }

  @PutMapping("/api/admin/users/{id}/status")
  public ApiResponse<UserSummary> adminStatus(
      @PathVariable Long id,
      @RequestParam(required = false) String status,
      @RequestBody(required = false) Map<String, Object> request) {
    String resolved = status != null ? status : String.valueOf(request == null ? "ACTIVE" : request.get("status"));
    return ApiResponse.ok("USER_STATUS_UPDATED", "User status updated", userProfileService.updateStatus(id, resolved));
  }

  @PutMapping("/api/admin/users/{id}/roles")
  public ApiResponse<UserSummary> adminRoles(@PathVariable Long id, @RequestBody Map<String, Set<String>> request) {
    return ApiResponse.ok("USER_ROLES_UPDATED", "User roles updated", userProfileService.updateRoles(id, request.get("roles")));
  }

  @DeleteMapping("/api/admin/users/{id}")
  public ApiResponse<Void> adminDelete(@PathVariable Long id) {
    userProfileService.delete(id);
    return ApiResponse.ok("USER_DELETED", "User deleted");
  }

  @GetMapping("/api/admin/audit-logs")
  public ApiResponse<List<AuditLog>> auditLogs() {
    return ApiResponse.ok(auditLogRepository.findAll());
  }

  @GetMapping("/api/admin/audit-logs/entity/{entityType}/{entityId}")
  public ApiResponse<List<AuditLog>> entityAuditLogs(
      @PathVariable String entityType, @PathVariable Long entityId) {
    return ApiResponse.ok(auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId));
  }

  @GetMapping("/api/admin/audit-logs/user/{userId}")
  public ApiResponse<List<AuditLog>> userAuditLogs(@PathVariable Long userId) {
    return ApiResponse.ok(auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId));
  }

  @GetMapping("/api/admin/audit-logs/statistics")
  public ApiResponse<Map<String, Long>> auditStatistics() {
    return ApiResponse.ok(Map.of("total", auditLogRepository.count()));
  }

  @GetMapping("/api/admin/hello")
  public ApiResponse<String> helloAdmin() {
    return ApiResponse.ok("Hello Admin");
  }

  @GetMapping("/")
  public ApiResponse<String> home() {
    return ApiResponse.ok("Welcome to Laundry Locker");
  }

  @GetMapping("/secured")
  public ApiResponse<String> secured() {
    return ApiResponse.ok("Secured endpoint");
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
