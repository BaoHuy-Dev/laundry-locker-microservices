package com.huynqb.laundrylocker.partner.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.partner.dto.AccessCodeRequest;
import com.huynqb.laundrylocker.partner.dto.AccessCodeResponse;
import com.huynqb.laundrylocker.partner.dto.PartnerRequest;
import com.huynqb.laundrylocker.partner.dto.PartnerResponse;
import com.huynqb.laundrylocker.partner.service.PartnerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  public ApiResponse<List<PartnerResponse>> listLegacy() {
    return list();
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

  @PutMapping("/api/partners/{id}/status")
  public ApiResponse<PartnerResponse> status(@PathVariable Long id, @RequestParam String status) {
    return ApiResponse.ok(partnerService.updateStatus(id, status));
  }

  @GetMapping("/internal/partners/{id}")
  public ApiResponse<PartnerResponse> getInternal(@PathVariable Long id) {
    return ApiResponse.ok(partnerService.get(id));
  }
}
