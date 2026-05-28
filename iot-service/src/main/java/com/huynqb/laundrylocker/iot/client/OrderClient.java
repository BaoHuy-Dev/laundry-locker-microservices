package com.huynqb.laundrylocker.iot.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.iot.dto.OrderLookupResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service", path = "/api/orders")
public interface OrderClient {

  @GetMapping("/pin/{pinCode}")
  ApiResponse<OrderLookupResponse> getByPin(@PathVariable String pinCode);

  @PutMapping("/{orderId}/complete")
  ApiResponse<OrderLookupResponse> complete(
      @PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId);
}
