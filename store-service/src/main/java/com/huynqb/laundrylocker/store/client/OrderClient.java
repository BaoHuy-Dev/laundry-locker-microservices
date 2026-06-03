package com.huynqb.laundrylocker.store.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", path = "/internal/orders")
public interface OrderClient {

  @GetMapping("/stores/{storeId}/ratings")
  ApiResponse<List<Map<String, Object>>> storeRatings(@PathVariable Long storeId);
}
