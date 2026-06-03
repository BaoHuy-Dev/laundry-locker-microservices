package com.huynqb.laundrylocker.staff.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "locker-service")
public interface LockerClient {

  @GetMapping("/api/lockers")
  ApiResponse<List<Map<String, Object>>> lockers(@RequestParam(required = false) Long storeId);

  @PostMapping("/api/boxes/{id}/open")
  ApiResponse<Map<String, Object>> openBox(@PathVariable Long id);
}
