package com.huynqb.laundrylocker.staff.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.staff.dto.StaffAssignmentRequest;
import com.huynqb.laundrylocker.staff.dto.StaffAssignmentResponse;
import com.huynqb.laundrylocker.staff.service.StaffService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StaffController {

  private final StaffService staffService;

  @PostMapping("/api/staff/assignments")
  public ApiResponse<StaffAssignmentResponse> assign(@Valid @RequestBody StaffAssignmentRequest request) {
    return ApiResponse.ok("STAFF_ASSIGNED", "Staff assigned", staffService.assign(request));
  }

  @PostMapping("/api/staff/orders/{orderId}/assign")
  public ApiResponse<StaffAssignmentResponse> assignOrder(
      @PathVariable Long orderId,
      @RequestParam(required = false) Long lockerId,
      @RequestHeader("X-User-Id") Long staffId) {
    return assign(new StaffAssignmentRequest(staffId, orderId, lockerId, "ASSIGNED"));
  }

  @GetMapping("/api/staff/orders/my-assigned")
  public ApiResponse<List<StaffAssignmentResponse>> myAssigned(@RequestHeader("X-User-Id") Long staffId) {
    return ApiResponse.ok(staffService.list(staffId));
  }

  @GetMapping("/api/staff/orders")
  public ApiResponse<List<StaffAssignmentResponse>> orders(@RequestHeader(value = "X-User-Id", required = false) Long staffId) {
    return ApiResponse.ok(staffService.list(staffId));
  }

  @GetMapping("/api/staff/orders/waiting")
  public ApiResponse<List<StaffAssignmentResponse>> waiting(@RequestHeader(value = "X-User-Id", required = false) Long staffId) {
    return orders(staffId);
  }

  @GetMapping("/api/staff/orders/processing")
  public ApiResponse<List<StaffAssignmentResponse>> processing(@RequestHeader(value = "X-User-Id", required = false) Long staffId) {
    return orders(staffId);
  }

  @GetMapping("/api/staff/orders/ready")
  public ApiResponse<List<StaffAssignmentResponse>> ready(@RequestHeader(value = "X-User-Id", required = false) Long staffId) {
    return orders(staffId);
  }

  @GetMapping("/api/staff/lockers")
  public ApiResponse<List<Map<String, Object>>> lockers(@RequestParam(required = false) Long storeId) {
    return ApiResponse.ok(staffService.lockers(storeId));
  }

  @PostMapping("/api/staff/unlock-box")
  public ApiResponse<Map<String, Object>> unlockBox(
      @RequestBody Map<String, Object> request,
      @RequestHeader(value = "X-User-Id", required = false) Long staffId) {
    return ApiResponse.ok("BOX_UNLOCKED", "Unlock processed", staffService.unlockBox(request, staffId));
  }

  @GetMapping("/api/staff/assignments")
  public ApiResponse<List<StaffAssignmentResponse>> list(@RequestParam(required = false) Long staffId) {
    return ApiResponse.ok(staffService.list(staffId));
  }

  @GetMapping("/api/staff/assignments/{id}")
  public ApiResponse<StaffAssignmentResponse> get(@PathVariable Long id) {
    return ApiResponse.ok(staffService.get(id));
  }

  @GetMapping("/internal/staff/assignments/{id}")
  public ApiResponse<StaffAssignmentResponse> getInternal(@PathVariable Long id) {
    return ApiResponse.ok(staffService.get(id));
  }
}
