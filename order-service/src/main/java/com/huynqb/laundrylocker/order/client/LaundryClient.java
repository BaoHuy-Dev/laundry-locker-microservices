package com.huynqb.laundrylocker.order.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "laundry-service", path = "/internal/laundry-services")
public interface LaundryClient {

  @GetMapping("/estimate")
  ApiResponse<BigDecimal> estimate(
      @RequestParam List<Long> serviceIds, @RequestParam(required = false) BigDecimal quantity);
}
