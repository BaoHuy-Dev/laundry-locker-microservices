package com.huynqb.laundrylocker.partner.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "locker-service")
public interface LockerClient {

  @GetMapping("/api/lockers")
  ApiResponse<List<Map<String, Object>>> lockers();

  @GetMapping("/api/lockers/{lockerId}/boxes/available")
  ApiResponse<List<Map<String, Object>>> availableBoxes(@PathVariable Long lockerId);
}
