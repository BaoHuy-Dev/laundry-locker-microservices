package com.huynqb.laundrylocker.user.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", path = "/internal/auth")
public interface AuthClient {

  @PostMapping("/users/{userId}/password")
  ApiResponse<Void> changePassword(@PathVariable Long userId, @RequestBody Map<String, Object> request);
}
