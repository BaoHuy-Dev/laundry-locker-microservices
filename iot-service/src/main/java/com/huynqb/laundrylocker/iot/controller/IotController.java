package com.huynqb.laundrylocker.iot.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.iot.dto.BoxStateSyncRequest;
import com.huynqb.laundrylocker.iot.dto.BoxStatusUpdateRequest;
import com.huynqb.laundrylocker.iot.dto.DeviceStatusRequest;
import com.huynqb.laundrylocker.iot.dto.DeviceStatusResponse;
import com.huynqb.laundrylocker.iot.dto.ForceUnlockRequest;
import com.huynqb.laundrylocker.iot.dto.PickupRequest;
import com.huynqb.laundrylocker.iot.dto.PickupResponse;
import com.huynqb.laundrylocker.iot.dto.UnlockRequest;
import com.huynqb.laundrylocker.iot.dto.VerifyPinRequest;
import com.huynqb.laundrylocker.iot.dto.VerifyPinResponse;
import com.huynqb.laundrylocker.iot.service.IotService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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

  // Operations visibility for Manager/Admin — cabinet heartbeat was collected
  // but never surfaced anywhere until now.
  @GetMapping("/api/manage/iot/device-status")
  public ApiResponse<List<DeviceStatusResponse>> listDeviceStatuses() {
    return ApiResponse.ok(iotService.listDeviceStatuses());
  }

  @PostMapping("/api/iot/unlock")
  public ApiResponse<Map<String, Object>> unlock(
      @Valid @RequestBody UnlockRequest request,
      @RequestHeader(value = "X-User-Id", required = false) Long userId) {
    return ApiResponse.ok("IOT_UNLOCK_ACCEPTED", "Unlock command accepted", iotService.unlock(request, userId));
  }

  // Service-to-service only (blocked at gateway): maintenance force-open,
  // called from locker-service's /api/maintenance/boxes/{id}/force-open.
  @PostMapping("/internal/iot/force-unlock")
  public ApiResponse<Map<String, Object>> forceUnlock(@Valid @RequestBody ForceUnlockRequest request) {
    return ApiResponse.ok("IOT_FORCE_UNLOCK_ACCEPTED", "Force unlock accepted", iotService.forceUnlock(request));
  }

  // Service-to-service only (blocked at gateway): booking → IoT sync (GAP 1),
  // called from locker-service when a box is reserved/occupied/released/faulted.
  // Mirrors the box lifecycle state down to the cabinet over MQTT.
  @PostMapping("/internal/iot/box-sync")
  public ApiResponse<Map<String, Object>> boxSync(@Valid @RequestBody BoxStateSyncRequest request) {
    return ApiResponse.ok("IOT_BOX_SYNC_PUBLISHED", "Box state sync published", iotService.syncBoxState(request));
  }


  @PostMapping("/api/iot/verify-pin")
  public ApiResponse<VerifyPinResponse> verifyPin(@Valid @RequestBody VerifyPinRequest request) {
    return ApiResponse.ok(iotService.verifyPin(request));
  }

  // PIN or signed QR token — what the cabinet touchscreen scans/keys in.
  @PostMapping("/api/iot/verify-access")
  public ApiResponse<VerifyPinResponse> verifyAccess(@Valid @RequestBody VerifyPinRequest request) {
    return ApiResponse.ok(iotService.verifyAccess(request.boxId(), request.pinCode()));
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
