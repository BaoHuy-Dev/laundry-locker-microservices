package com.huynqb.laundrylocker.iot.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "partner-service", path = "/internal/partners")
public interface PartnerClient {

  @GetMapping("/access-codes/verify/{code}")
  ApiResponse<Map<String, Object>> verifyCode(@PathVariable String code);
}
