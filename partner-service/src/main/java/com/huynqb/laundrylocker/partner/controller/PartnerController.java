package com.huynqb.laundrylocker.partner.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.partner.dto.AccessCodeRequest;
import com.huynqb.laundrylocker.partner.dto.AccessCodeResponse;
import com.huynqb.laundrylocker.partner.dto.PartnerRequest;
import com.huynqb.laundrylocker.partner.dto.PartnerResponse;
import com.huynqb.laundrylocker.partner.service.PartnerService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
public class PartnerController {

  private final PartnerService partnerService;

  @PostMapping("/api/partners")
  public ApiResponse<PartnerResponse> create(@Valid @RequestBody PartnerRequest request) {
    return ApiResponse.ok("PARTNER_CREATED", "Partner created", partnerService.create(request));
  }

  @PostMapping("/api/partner")
  public ApiResponse<PartnerResponse> createLegacy(@Valid @RequestBody PartnerRequest request) {
    return create(request);
  }

  @GetMapping("/api/partners")
  public ApiResponse<List<PartnerResponse>> list() {
    return ApiResponse.ok(partnerService.list());
  }

  @GetMapping("/api/partner")
  public ApiResponse<?> listLegacy(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
    return userId == null ? list() : ApiResponse.ok(partnerService.getByUser(userId));
  }

  @PutMapping("/api/partner")
  public ApiResponse<PartnerResponse> updatePartner(
      @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody PartnerRequest request) {
    return ApiResponse.ok("PARTNER_UPDATED", "Partner updated", partnerService.updateByUser(userId, request));
  }

  @GetMapping("/api/partners/{id}")
  public ApiResponse<PartnerResponse> get(@PathVariable Long id) {
    return ApiResponse.ok(partnerService.get(id));
  }

  @PostMapping("/api/partners/access-codes")
  public ApiResponse<AccessCodeResponse> generateCode(@Valid @RequestBody AccessCodeRequest request) {
    return ApiResponse.ok("ACCESS_CODE_CREATED", "Access code created", partnerService.generateAccessCode(request));
  }

  @PostMapping("/api/partner/access-codes/generate")
  public ApiResponse<AccessCodeResponse> generateCodeLegacy(@Valid @RequestBody AccessCodeRequest request) {
    return generateCode(request);
  }

  @GetMapping("/api/partner/access-codes")
  public ApiResponse<List<AccessCodeResponse>> accessCodes(@RequestParam(required = false) Long partnerId) {
    return ApiResponse.ok(partnerService.accessCodes(partnerId, null));
  }

  @GetMapping("/api/partner/access-codes/order/{orderId}")
  public ApiResponse<List<AccessCodeResponse>> accessCodesByOrder(@PathVariable Long orderId) {
    return ApiResponse.ok(partnerService.accessCodes(null, orderId));
  }

  @PostMapping("/api/partner/access-codes/{id}/cancel")
  public ApiResponse<AccessCodeResponse> cancelCode(@PathVariable Long id) {
    return ApiResponse.ok("ACCESS_CODE_CANCELLED", "Access code cancelled", partnerService.cancelCode(id));
  }

  @GetMapping("/internal/partners/access-codes/verify/{code}")
  public ApiResponse<AccessCodeResponse> verifyCode(@PathVariable String code) {
    return ApiResponse.ok(partnerService.verifyCode(code));
  }

  @PostMapping("/api/admin/partners/{partnerId}/approve")
  public ApiResponse<PartnerResponse> approve(@PathVariable Long partnerId) {
    return ApiResponse.ok("PARTNER_APPROVED", "Partner approved", partnerService.updateStatus(partnerId, "ACTIVE"));
  }

  @PostMapping("/api/admin/partners/{partnerId}/reject")
  public ApiResponse<PartnerResponse> reject(@PathVariable Long partnerId) {
    return ApiResponse.ok("PARTNER_REJECTED", "Partner rejected", partnerService.updateStatus(partnerId, "REJECTED"));
  }

  @PostMapping("/api/admin/partners/{partnerId}/suspend")
  public ApiResponse<PartnerResponse> suspend(@PathVariable Long partnerId) {
    return ApiResponse.ok("PARTNER_SUSPENDED", "Partner suspended", partnerService.updateStatus(partnerId, "SUSPENDED"));
  }

  @GetMapping("/api/admin/partners")
  public ApiResponse<List<PartnerResponse>> adminList() {
    return list();
  }

  @GetMapping("/api/admin/partners/{partnerId}")
  public ApiResponse<PartnerResponse> adminGet(@PathVariable Long partnerId) {
    return get(partnerId);
  }

  @GetMapping("/api/partner/dashboard")
  public ApiResponse<Map<String, Object>> dashboard(@RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok(partnerService.dashboard(userId));
  }

  @GetMapping("/api/partner/orders/pending")
  public ApiResponse<List<Map<String, Object>>> pendingOrders() {
    return ApiResponse.ok(partnerService.orders("INITIALIZED"));
  }

  @GetMapping("/api/partner/orders")
  public ApiResponse<List<Map<String, Object>>> orders(@RequestParam(required = false) String status) {
    return ApiResponse.ok(partnerService.orders(status));
  }

  @GetMapping("/api/partner/orders/{orderId}")
  public ApiResponse<Map<String, Object>> order(@PathVariable Long orderId) {
    return ApiResponse.ok(partnerService.order(orderId));
  }

  @PostMapping("/api/partner/orders/{orderId}/accept")
  public ApiResponse<Map<String, Object>> accept(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok("ORDER_ACCEPTED", "Order accepted", partnerService.acceptOrder(orderId, userId));
  }

  @PostMapping("/api/partner/orders/{orderId}/collect")
  public ApiResponse<Map<String, Object>> collect(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok("ORDER_COLLECTED", "Order collected", partnerService.collectOrder(orderId, userId));
  }

  @PostMapping("/api/partner/orders/{orderId}/process")
  public ApiResponse<Map<String, Object>> process(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok("ORDER_PROCESSING", "Order processing", partnerService.processOrder(orderId, userId));
  }

  @PostMapping("/api/partner/orders/{orderId}/ready")
  public ApiResponse<Map<String, Object>> ready(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok("ORDER_READY", "Order ready", partnerService.readyOrder(orderId, userId));
  }

  @PutMapping("/api/partner/orders/{orderId}/weight")
  public ApiResponse<Map<String, Object>> weight(
      @PathVariable Long orderId, @RequestBody Map<String, Object> request, @RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok("ORDER_WEIGHT_UPDATED", "Order weight updated", partnerService.updateOrderWeight(orderId, request, userId));
  }

  @GetMapping("/api/partner/orders/statistics")
  public ApiResponse<Map<String, Object>> orderStatistics() {
    return ApiResponse.ok(partnerService.orderStatistics());
  }

  @GetMapping("/api/partner/revenue")
  public ApiResponse<Map<String, Object>> revenue() {
    return ApiResponse.ok(partnerService.revenue());
  }

  @GetMapping("/api/partner/stores")
  public ApiResponse<List<Map<String, Object>>> stores() {
    return ApiResponse.ok(partnerService.stores());
  }

  @GetMapping("/api/partner/lockers")
  public ApiResponse<List<Map<String, Object>>> lockers() {
    return ApiResponse.ok(partnerService.lockers());
  }

  @GetMapping("/api/partner/lockers/{lockerId}/boxes/available")
  public ApiResponse<List<Map<String, Object>>> availableBoxes(@PathVariable Long lockerId) {
    return ApiResponse.ok(partnerService.availableBoxes(lockerId));
  }

  @GetMapping("/api/partner/staff")
  public ApiResponse<List<Map<String, Object>>> staff() {
    return ApiResponse.ok(List.of());
  }

  @PostMapping("/api/partner/staff/{staffId}")
  public ApiResponse<Map<String, Object>> addStaff(@PathVariable Long staffId) {
    return ApiResponse.ok("PARTNER_STAFF_ADDED", "Staff added", Map.of("staffId", staffId));
  }

  @DeleteMapping("/api/partner/staff/{staffId}")
  public ApiResponse<Map<String, Object>> removeStaff(@PathVariable Long staffId) {
    return ApiResponse.ok("PARTNER_STAFF_REMOVED", "Staff removed", Map.of("staffId", staffId));
  }

  @PutMapping("/api/partners/{id}/status")
  public ApiResponse<PartnerResponse> status(@PathVariable Long id, @RequestParam String status) {
    return ApiResponse.ok(partnerService.updateStatus(id, status));
  }

  @GetMapping("/internal/partners/{id}")
  public ApiResponse<PartnerResponse> getInternal(@PathVariable Long id) {
    return ApiResponse.ok(partnerService.get(id));
  }
}
