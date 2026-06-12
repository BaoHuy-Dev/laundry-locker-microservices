package com.huynqb.laundrylocker.iot.service;

import com.huynqb.laundrylocker.common.event.DomainEvent;
import com.huynqb.laundrylocker.common.event.DomainEventNames;
import com.huynqb.laundrylocker.iot.client.LockerClient;
import com.huynqb.laundrylocker.iot.client.OrderClient;

import com.huynqb.laundrylocker.iot.dto.BoxStatusUpdateRequest;
import com.huynqb.laundrylocker.iot.dto.DeviceStatusRequest;
import com.huynqb.laundrylocker.iot.dto.DeviceStatusResponse;
import com.huynqb.laundrylocker.iot.dto.OrderLookupResponse;
import com.huynqb.laundrylocker.iot.dto.PickupRequest;
import com.huynqb.laundrylocker.iot.dto.PickupResponse;
import com.huynqb.laundrylocker.iot.dto.UnlockRequest;
import com.huynqb.laundrylocker.iot.dto.VerifyPinRequest;
import com.huynqb.laundrylocker.iot.dto.VerifyPinResponse;
import com.huynqb.laundrylocker.iot.model.DeviceStatus;
import com.huynqb.laundrylocker.iot.repository.DeviceStatusRepository;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IotService {

  private final DeviceStatusRepository repository;
  private final RabbitTemplate rabbitTemplate;
  private final OrderClient orderClient;
  private final LockerClient lockerClient;
  private final LockerMqttService lockerMqttService;


  @Transactional
  public DeviceStatusResponse updateStatus(DeviceStatusRequest request) {
    DeviceStatus device = repository.findByDeviceId(request.deviceId()).orElseGet(DeviceStatus::new);
    device.setDeviceId(request.deviceId());
    device.setLockerId(request.lockerId());
    device.setStatus(request.status().toUpperCase());
    device.setLastSeenAt(LocalDateTime.now());
    DeviceStatus saved = repository.save(device);
    publishDeviceStatus(saved);
    return toResponse(saved);
  }

  public Map<String, Object> unlock(UnlockRequest request) {
    VerifyPinResponse verification = verifyPin(new VerifyPinRequest(request.boxId(), request.pinCode()));
    if (!Boolean.TRUE.equals(verification.valid())) {
      return Map.of("accepted", false, "boxId", request.boxId(), "message", verification.message());
    }
    try {
      com.fasterxml.jackson.databind.JsonNode node = lockerMqttService.sendUnlockCommandAsync(request.lockerId(), request.boxId())
          .get(20, java.util.concurrent.TimeUnit.SECONDS);
      
      if (node.has("status") && "FAILED".equals(node.get("status").asText())) {
          return Map.of("accepted", false, "lockerId", request.lockerId(), "boxId", request.boxId(), "message", "Hardware failed to open");
      }
      lockerClient.openBox(request.boxId());
      return Map.of("accepted", true, "lockerId", request.lockerId(), "boxId", request.boxId(), "message", "Unlock command accepted");
    } catch (Exception e) {
      log.error("Timeout or error waiting for IoT device", e);
      return Map.of("accepted", false, "lockerId", request.lockerId(), "boxId", request.boxId(), "message", "IoT device timeout");
    }
  }

  public VerifyPinResponse verifyPin(VerifyPinRequest request) {
    return verifyAccess(request.boxId(), request.pinCode());
  }

  /** Accepts either a 6-digit PIN or a signed QR token (LLQR.*) as credential. */
  public VerifyPinResponse verifyAccess(Long boxId, String code) {
    try {
      OrderLookupResponse order = orderClient.getByAccess(code).data();
      boolean validBox =
          boxId.equals(order.sendBoxId()) || boxId.equals(order.receiveBoxId());
      if (!validBox) {
        return new VerifyPinResponse(false, order.id(), boxId, order.status(), "Access code does not match this box");
      }
      return new VerifyPinResponse(true, order.id(), boxId, order.status(), "Access verified");
    } catch (Exception ex) {
      return new VerifyPinResponse(false, null, boxId, null, "Invalid access code");
    }
  }

  public PickupResponse pickup(PickupRequest request, Long userId) {
    OrderLookupResponse response = orderClient.complete(request.orderId(), userId).data();
    return new PickupResponse(response.id(), response.status(), response.completedAt(), "Pickup confirmed");
  }



  @Transactional
  public void updateBoxStatus(BoxStatusUpdateRequest request) {
    publishRawDeviceStatus("box-" + request.boxId(), null, request.status().toUpperCase(), Map.of("boxId", request.boxId()));
  }

  private DeviceStatusResponse toResponse(DeviceStatus device) {
    return new DeviceStatusResponse(device.getId(), device.getDeviceId(), device.getLockerId(), device.getStatus(), device.getLastSeenAt());
  }

  private void publishDeviceStatus(DeviceStatus device) {
    try {
      rabbitTemplate.convertAndSend(
          DomainEventNames.EXCHANGE,
          DomainEventNames.IOT_DEVICE_STATUS_CHANGED,
          DomainEvent.of(
              DomainEventNames.IOT_DEVICE_STATUS_CHANGED,
              "iot-service",
              eventPayload(device.getDeviceId(), device.getLockerId(), device.getStatus(), Map.of())));
    } catch (AmqpException ex) {
      log.warn("Could not publish iot.device.status.changed: {}", ex.getMessage());
    }
  }

  private void publishRawDeviceStatus(String deviceId, Long lockerId, String status, Map<String, Object> extra) {
    try {
      rabbitTemplate.convertAndSend(
          DomainEventNames.EXCHANGE,
          DomainEventNames.IOT_DEVICE_STATUS_CHANGED,
          DomainEvent.of(DomainEventNames.IOT_DEVICE_STATUS_CHANGED, "iot-service", eventPayload(deviceId, lockerId, status, extra)));
    } catch (AmqpException ex) {
      log.warn("Could not publish iot.device.status.changed: {}", ex.getMessage());
    }
  }

  private Map<String, Object> eventPayload(String deviceId, Long lockerId, String status, Map<String, Object> extra) {
    java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
    payload.put("deviceId", deviceId);
    if (lockerId != null) {
      payload.put("lockerId", lockerId);
    }
    payload.put("status", status);
    payload.putAll(extra);
    return payload;
  }
}
