package com.huynqb.laundrylocker.iot.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.iot.dto.OrderLookupResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service")
public interface OrderClient {

  @GetMapping("/api/orders/pin/{pinCode}")
  ApiResponse<OrderLookupResponse> getByPin(@PathVariable String pinCode);

  @GetMapping("/internal/orders/by-access")
  ApiResponse<OrderLookupResponse> getByAccess(@RequestParam("code") String code);

  @PutMapping("/api/orders/{orderId}/complete")
  ApiResponse<OrderLookupResponse> complete(
      @PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId);
}
