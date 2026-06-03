package com.huynqb.laundrylocker.partner.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "store-service")
public interface StoreClient {

  @GetMapping("/api/stores")
  ApiResponse<List<Map<String, Object>>> stores();
}
