package com.huynqb.laundrylocker.iot.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.iot.dto.BoxStatusUpdateRequest;
import com.huynqb.laundrylocker.iot.dto.DeviceStatusRequest;
import com.huynqb.laundrylocker.iot.dto.DeviceStatusResponse;
import com.huynqb.laundrylocker.iot.dto.PickupRequest;
import com.huynqb.laundrylocker.iot.dto.PickupResponse;
import com.huynqb.laundrylocker.iot.dto.UnlockRequest;
import com.huynqb.laundrylocker.iot.dto.VerifyPinRequest;
import com.huynqb.laundrylocker.iot.dto.VerifyPinResponse;
import com.huynqb.laundrylocker.iot.service.IotService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IotController {

  private final IotService iotService;

  @PostMapping("/api/iot/device-status")
  public ApiResponse<DeviceStatusResponse> updateStatus(@Valid @RequestBody DeviceStatusRequest request) {
    return ApiResponse.ok("IOT_STATUS_UPDATED", "Device status updated", iotService.updateStatus(request));
  }

  @PostMapping("/api/iot/unlock")
  public ApiResponse<Map<String, Object>> unlock(@Valid @RequestBody UnlockRequest request) {
    return ApiResponse.ok("IOT_UNLOCK_ACCEPTED", "Unlock command accepted", iotService.unlock(request));
  }

  @PostMapping("/api/iot/unlock-with-code")
  public ApiResponse<Map<String, Object>> unlockWithCode(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok("IOT_UNLOCK_ACCEPTED", "Unlock command accepted", iotService.unlockWithCode(request));
  }

  @PostMapping("/api/iot/verify-pin")
  public ApiResponse<VerifyPinResponse> verifyPin(@Valid @RequestBody VerifyPinRequest request) {
    return ApiResponse.ok(iotService.verifyPin(request));
  }

  @PostMapping("/api/iot/pickup")
  public ApiResponse<PickupResponse> pickup(@Valid @RequestBody PickupRequest request, @RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok("PICKUP_CONFIRMED", "Pickup confirmed", iotService.pickup(request, userId));
  }

  @PostMapping("/api/iot/box-status")
  public ApiResponse<Void> boxStatus(@Valid @RequestBody BoxStatusUpdateRequest request) {
    iotService.updateBoxStatus(request);
    return ApiResponse.ok("BOX_STATUS_UPDATED", "Box status event published");
  }

  @PostMapping("/internal/iot/device-status")
  public ApiResponse<DeviceStatusResponse> updateStatusInternal(@Valid @RequestBody DeviceStatusRequest request) {
    return updateStatus(request);
  }
}
