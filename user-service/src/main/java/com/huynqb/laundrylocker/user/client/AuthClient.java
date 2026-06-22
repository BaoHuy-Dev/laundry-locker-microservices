package com.huynqb.laundrylocker.user.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service", path = "/internal/auth")
public interface AuthClient {

  @PostMapping("/accounts")
  ApiResponse<Map<String, Object>> createAccount(@RequestBody Map<String, Object> request);

  @PostMapping("/users/{userId}/password")
  ApiResponse<Void> changePassword(@PathVariable Long userId, @RequestBody Map<String, Object> request);

  @GetMapping("/accounts/by-users")
  ApiResponse<List<Map<String, Object>>> accountsByUsers(@RequestParam("userIds") List<Long> userIds);
}
