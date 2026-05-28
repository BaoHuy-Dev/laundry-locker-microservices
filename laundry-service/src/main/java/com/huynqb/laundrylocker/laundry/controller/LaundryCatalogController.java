package com.huynqb.laundrylocker.laundry.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.laundry.dto.LaundryCatalogRequest;
import com.huynqb.laundrylocker.laundry.dto.LaundryCatalogResponse;
import com.huynqb.laundrylocker.laundry.service.LaundryCatalogService;
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
      @RequestParam(required = false) String category) {
    return ApiResponse.ok(service.list(storeId, category));
  }

  @GetMapping("/api/services")
  public ApiResponse<List<LaundryCatalogResponse>> listLegacy(
      @RequestParam(required = false) Long storeId,
      @RequestParam(required = false) String category) {
    return list(storeId, category);
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

  @GetMapping("/internal/laundry-services/estimate")
  public ApiResponse<java.math.BigDecimal> estimate(
      @RequestParam List<Long> serviceIds,
      @RequestParam(required = false) java.math.BigDecimal quantity) {
    return ApiResponse.ok(service.estimate(serviceIds, quantity));
  }
}
