package com.huynqb.laundrylocker.locker.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "iot-service", path = "/internal/iot")
public interface IotClient {

  @PostMapping("/force-unlock")
  ApiResponse<Map<String, Object>> forceUnlock(@RequestBody ForceUnlockRequest request);

  record ForceUnlockRequest(Long lockerId, Long boxId, Long actorUserId) {}

  /// Booking → IoT sync (GAP 1): tell the cabinet a box's lifecycle state changed
  /// (RESERVED/OCCUPIED/AVAILABLE/FAULT). Best-effort — callers swallow failures.
  @PostMapping("/box-sync")
  ApiResponse<Map<String, Object>> syncBoxState(@RequestBody BoxStateSyncRequest request);

  record BoxStateSyncRequest(Long lockerId, Long boxId, String state, Long orderId) {}
}
