package com.huynqb.laundrylocker.store.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.store.dto.StoreRequest;
import com.huynqb.laundrylocker.store.dto.StoreResponse;
import com.huynqb.laundrylocker.store.service.StoreService;
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
public class StoreController {

  private final StoreService service;

  @PostMapping("/api/stores")
  public ApiResponse<StoreResponse> create(@Valid @RequestBody StoreRequest request) {
    return ApiResponse.ok("STORE_CREATED", "Store created", service.create(request));
  }

  @GetMapping("/api/stores")
  public ApiResponse<List<StoreResponse>> list(
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude,
      @RequestParam(required = false) Double radiusKm) {
    if (latitude != null && longitude != null) {
      return ApiResponse.ok(service.nearby(latitude, longitude, radiusKm));
    }
    return ApiResponse.ok(service.list());
  }

  @GetMapping("/api/stores/nearby")
  public ApiResponse<List<StoreResponse>> nearby(
      @RequestParam Double latitude,
      @RequestParam Double longitude,
      @RequestParam(required = false) Double radiusKm) {
    return ApiResponse.ok(service.nearby(latitude, longitude, radiusKm));
  }

  @GetMapping("/api/stores/{id}")
  public ApiResponse<StoreResponse> get(@PathVariable Long id) {
    return ApiResponse.ok(service.get(id));
  }

  @GetMapping("/internal/stores/{id}")
  public ApiResponse<StoreResponse> getInternal(@PathVariable Long id) {
    return ApiResponse.ok(service.get(id));
  }

  @PutMapping("/api/admin/stores/{id}")
  public ApiResponse<StoreResponse> adminUpdate(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
    return ApiResponse.ok("STORE_UPDATED", "Store updated", service.update(id, request));
  }

  @PutMapping("/api/admin/stores/{id}/status")
  public ApiResponse<StoreResponse> adminStatus(@PathVariable Long id, @RequestParam String status) {
    return ApiResponse.ok("STORE_STATUS_UPDATED", "Store status updated", service.updateStatus(id, status));
  }
}
