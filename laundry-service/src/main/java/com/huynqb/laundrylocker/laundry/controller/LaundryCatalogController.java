package com.huynqb.laundrylocker.laundry.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.laundry.dto.LaundryCatalogRequest;
import com.huynqb.laundrylocker.laundry.dto.LaundryCatalogResponse;
import com.huynqb.laundrylocker.laundry.service.LaundryCatalogService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LaundryCatalogController {

  private final LaundryCatalogService service;

  @PostMapping("/api/laundry-services")
  public ApiResponse<LaundryCatalogResponse> create(@Valid @RequestBody LaundryCatalogRequest request) {
    return ApiResponse.ok("LAUNDRY_SERVICE_CREATED", "Laundry service created", service.create(request));
  }

  @PostMapping("/api/services")
  public ApiResponse<LaundryCatalogResponse> createLegacy(@Valid @RequestBody LaundryCatalogRequest request) {
    return create(request);
  }

  @GetMapping("/api/laundry-services")
  public ApiResponse<List<LaundryCatalogResponse>> list(
      @RequestParam(required = false) Long storeId,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Long lockerId) {
    if (lockerId != null) {
      return ApiResponse.ok(service.listByLocker(lockerId, category));
    }
    return ApiResponse.ok(service.list(storeId, category));
  }

  @GetMapping("/api/services")
  public ApiResponse<List<LaundryCatalogResponse>> listLegacy(
      @RequestParam(required = false) Long storeId,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Long lockerId) {
    return list(storeId, category, lockerId);
  }

  @GetMapping("/api/laundry-services/{id}")
  public ApiResponse<LaundryCatalogResponse> get(@PathVariable Long id) {
    return ApiResponse.ok(service.get(id));
  }

  @GetMapping("/api/services/{id}")
  public ApiResponse<LaundryCatalogResponse> getLegacy(@PathVariable Long id) {
    return get(id);
  }

  @GetMapping("/internal/laundry-services/{id}")
  public ApiResponse<LaundryCatalogResponse> getInternal(@PathVariable Long id) {
    return ApiResponse.ok(service.get(id));
  }

  @PutMapping("/api/admin/services/{id}")
  public ApiResponse<LaundryCatalogResponse> adminUpdate(@PathVariable Long id, @Valid @RequestBody LaundryCatalogRequest request) {
    return ApiResponse.ok("SERVICE_UPDATED", "Service updated", service.update(id, request));
  }

  @PostMapping("/api/admin/services")
  public ApiResponse<LaundryCatalogResponse> adminCreate(@Valid @RequestBody LaundryCatalogRequest request) {
    return createLegacy(request);
  }

  @GetMapping("/api/admin/services")
  public ApiResponse<List<LaundryCatalogResponse>> adminList() {
    return ApiResponse.ok(service.list(null, null));
  }

  @GetMapping("/api/admin/services/{id}")
  public ApiResponse<LaundryCatalogResponse> adminGet(@PathVariable Long id) {
    return getLegacy(id);
  }

  @PutMapping("/api/admin/services/{id}/price")
  public ApiResponse<LaundryCatalogResponse> adminPrice(@PathVariable Long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(
        "SERVICE_PRICE_UPDATED",
        "Service price updated",
        service.updatePrice(id, decimal(request.get("unitPrice")), decimal(request.get("maxPrice"))));
  }

  @PutMapping("/api/admin/services/{id}/status")
  public ApiResponse<LaundryCatalogResponse> adminStatus(
      @PathVariable Long id,
      @RequestParam(required = false) String status,
      @RequestBody(required = false) Map<String, Object> request) {
    String resolved = status != null ? status : String.valueOf(request == null ? "ACTIVE" : request.get("status"));
    return ApiResponse.ok("SERVICE_STATUS_UPDATED", "Service status updated", service.updateStatus(id, resolved));
  }

  @PutMapping("/api/admin/services/{id}/image")
  public ApiResponse<LaundryCatalogResponse> adminImage(@PathVariable Long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok("SERVICE_IMAGE_UPDATED", "Service image updated", service.updateImage(id, String.valueOf(request.get("imageUrl"))));
  }

  @DeleteMapping("/api/admin/services/{id}")
  public ApiResponse<Void> adminDelete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok("SERVICE_DELETED", "Service deleted");
  }

  @GetMapping("/internal/laundry-services/estimate")
  public ApiResponse<java.math.BigDecimal> estimate(
      @RequestParam List<Long> serviceIds,
      @RequestParam(required = false) java.math.BigDecimal quantity) {
    return ApiResponse.ok(service.estimate(serviceIds, quantity));
  }

  private java.math.BigDecimal decimal(Object value) {
    if (value == null) {
      return null;
    }
    return new java.math.BigDecimal(String.valueOf(value));
  }
}
