package com.huynqb.laundrylocker.partner.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service")
public interface OrderClient {

  @GetMapping("/api/orders")
  ApiResponse<List<Map<String, Object>>> orders(@RequestParam(required = false) String status);

  @GetMapping("/api/orders/{id}")
  ApiResponse<Map<String, Object>> order(@PathVariable Long id);

  @PutMapping("/api/admin/orders/{id}/status")
  ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> request);

  @PutMapping("/api/orders/{id}/collect")
  ApiResponse<Map<String, Object>> collect(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId);

  @PutMapping("/api/orders/{id}/process")
  ApiResponse<Map<String, Object>> process(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId);

  @PutMapping("/api/orders/{id}/ready")
  ApiResponse<Map<String, Object>> ready(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId);

  @PutMapping("/api/orders/{id}/weight")
  ApiResponse<Map<String, Object>> updateWeight(
      @PathVariable Long id, @RequestBody Map<String, Object> request, @RequestHeader("X-User-Id") Long userId);
}
