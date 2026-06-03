package com.huynqb.laundrylocker.laundry.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "locker-service")
public interface LockerClient {

  @GetMapping("/api/lockers/{id}")
  ApiResponse<LockerView> getLocker(@PathVariable Long id);

  record LockerView(
      Long id,
      Long storeId,
      String code,
      String name,
      String status,
      String address,
      Double latitude,
      Double longitude) {}
}
