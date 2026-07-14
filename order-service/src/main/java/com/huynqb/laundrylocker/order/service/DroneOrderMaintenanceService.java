package com.huynqb.laundrylocker.order.service;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.common.exception.BusinessException;
import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.order.client.LockerDroneClient;
import com.huynqb.laundrylocker.order.dto.AcceptDroneOrderRequest;
import com.huynqb.laundrylocker.order.dto.DroneMissionResponse;
import com.huynqb.laundrylocker.order.dto.DroneStatusUpdateRequest;
import com.huynqb.laundrylocker.order.dto.DroneUnitDto;
import com.huynqb.laundrylocker.order.dto.LockerLayoutDto;
import com.huynqb.laundrylocker.order.model.DroneMission;
import com.huynqb.laundrylocker.order.model.LockerOrder;
import com.huynqb.laundrylocker.order.repository.DroneMissionRepository;
import com.huynqb.laundrylocker.order.repository.LockerOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DroneOrderMaintenanceService {

  private static final int MIN_PREFLIGHT_BATTERY_PERCENT = 20;

  private final LockerOrderRepository orderRepository;
  private final DroneMissionRepository missionRepository;
  private final LockerDroneClient lockerDroneClient;

  @Transactional(readOnly = true)
  public List<DroneMissionResponse> queue(String deliveryStage) {
    return orderRepository.findByTypeAndStatusOrderByCreatedAtAsc("DRONE_DELIVERY", "AWAITING_DISPATCH").stream()
        .filter(order -> !StringUtils.hasText(deliveryStage) || deliveryStage.equalsIgnoreCase(order.getDeliveryStage()))
        .map(order -> toResponse(order, missionRepository.findByOrderId(order.getId()).orElse(null), null))
        .toList();
  }

  @Transactional
  public DroneMissionResponse accept(
      Long orderId, Long userId, String idempotencyKey, AcceptDroneOrderRequest request) {
    LockerOrder order = findDroneOrder(orderId);
    if (!"AWAITING_DISPATCH".equals(order.getStatus())) {
      throw new BusinessException("DRONE_ORDER_STATUS_INVALID", "Drone order is not awaiting dispatch");
    }

    DroneMission existingMission = missionRepository.findByOrderId(orderId).orElse(null);
    if (existingMission != null
        && idempotencyKey != null
        && idempotencyKey.equals(existingMission.getLastAcceptIdempotencyKey())) {
      DroneUnitDto currentDrone = fetchDrone(existingMission.getDroneUnitId());
      return toResponse(order, existingMission, currentDrone);
    }

    DroneUnitDto drone = fetchDrone(request.droneUnitId());
    validateDronePreflight(drone);
    LockerLayoutDto layout = fetchLockerLayout(requireDestinationLockerId(order));
    validateLockerPreflight(layout);

    DroneMission mission = existingMission == null ? new DroneMission() : existingMission;
    mission.setOrderId(order.getId());
    mission.setDroneUnitId(drone.id());
    mission.setSourceLockerId(drone.lockerId());
    mission.setDestinationLockerId(requireDestinationLockerId(order));
    mission.setAssignedByUserId(userId);
    mission.setStatus("READY_TO_LAUNCH");
    mission.setLastAcceptIdempotencyKey(idempotencyKey);
    mission.setReadyToLaunchAt(LocalDateTime.now());
    mission = missionRepository.save(mission);

    order.setDeliveryStage("ACCEPTED");
    order.setStaffId(userId);
    orderRepository.save(order);
    return toResponse(order, mission, drone);
  }

  @Transactional
  public DroneMissionResponse launch(Long orderId, Long userId, String idempotencyKey) {
    LockerOrder order = findDroneOrder(orderId);
    DroneMission mission =
        missionRepository.findByOrderId(orderId).orElseThrow(() -> new NotFoundException("DroneMission", orderId));
    if (idempotencyKey != null
        && idempotencyKey.equals(mission.getLastLaunchIdempotencyKey())
        && "LAUNCHING".equals(mission.getStatus())) {
      return toResponse(order, mission, fetchDrone(mission.getDroneUnitId()));
    }
    if (!"READY_TO_LAUNCH".equals(mission.getStatus())) {
      throw new BusinessException("DRONE_MISSION_STATUS_INVALID", "Drone mission is not ready to launch");
    }

    DroneStatusUpdateRequest statusRequest = new DroneStatusUpdateRequest("IN_FLIGHT", null);
    DroneUnitDto updatedDrone = requireData(lockerDroneClient.updateDroneStatus(mission.getDroneUnitId(), statusRequest), "DRONE_STATUS_SYNC_FAILED");
    mission.setStatus("LAUNCHING");
    mission.setLastLaunchIdempotencyKey(idempotencyKey);
    mission.setLaunchingAt(LocalDateTime.now());
    missionRepository.save(mission);

    order.setDeliveryStage("LAUNCHING");
    order.setStaffId(userId);
    orderRepository.save(order);
    return toResponse(order, mission, updatedDrone);
  }

  private LockerOrder findDroneOrder(Long orderId) {
    LockerOrder order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order", orderId));
    if (!"DRONE_DELIVERY".equals(order.getType())) {
      throw new BusinessException("DRONE_ORDER_REQUIRED", "Order is not a drone delivery order");
    }
    return order;
  }

  private Long requireDestinationLockerId(LockerOrder order) {
    Long lockerId = order.getDestinationLockerId() != null ? order.getDestinationLockerId() : order.getLockerId();
    if (lockerId == null) {
      throw new BusinessException("DRONE_DESTINATION_REQUIRED", "Drone order is missing destination locker");
    }
    return lockerId;
  }

  private DroneUnitDto fetchDrone(Long droneUnitId) {
    if (droneUnitId == null) {
      throw new BusinessException("DRONE_REQUIRED", "Drone unit is required");
    }
    return requireData(lockerDroneClient.getDroneUnit(droneUnitId), "DRONE_LOOKUP_FAILED");
  }

  private LockerLayoutDto fetchLockerLayout(Long lockerId) {
    return requireData(lockerDroneClient.getLockerLayout(lockerId), "LOCKER_LAYOUT_LOOKUP_FAILED");
  }

  private void validateDronePreflight(DroneUnitDto drone) {
    if (!Boolean.TRUE.equals(drone.active())) {
      throw new BusinessException("DRONE_INACTIVE", "Drone is inactive");
    }
    if (!"IDLE".equals(drone.status())) {
      throw new BusinessException("DRONE_NOT_IDLE", "Drone must be IDLE before acceptance");
    }
    if (drone.batteryPercent() != null && drone.batteryPercent() <= MIN_PREFLIGHT_BATTERY_PERCENT) {
      throw new BusinessException("DRONE_BATTERY_TOO_LOW", "Drone battery is too low for launch");
    }
  }

  private void validateLockerPreflight(LockerLayoutDto layout) {
    if (!"ACTIVE".equals(layout.status())) {
      throw new BusinessException("LOCKER_INACTIVE", "Destination locker is not active");
    }
    if (!Boolean.TRUE.equals(layout.landingPad())) {
      throw new BusinessException("LANDING_PAD_ABSENT", "Destination locker has no landing pad");
    }
    if (!"OK".equals(layout.landingPadStatus())) {
      throw new BusinessException("LANDING_PAD_UNAVAILABLE", "Landing pad is not ready");
    }
  }

  private <T> T requireData(ApiResponse<T> response, String fallbackCode) {
    if (response == null || response.data() == null) {
      throw new BusinessException(fallbackCode, "Required downstream data is missing");
    }
    return response.data();
  }

  private DroneMissionResponse toResponse(LockerOrder order, DroneMission mission, DroneUnitDto drone) {
    return new DroneMissionResponse(
        order.getId(),
        mission == null ? null : mission.getId(),
        mission == null ? null : mission.getStatus(),
        order.getDeliveryStage(),
        mission == null ? null : mission.getDroneUnitId(),
        drone == null ? null : drone.code(),
        mission == null ? null : mission.getSourceLockerId(),
        mission == null ? requireDestinationLockerId(order) : mission.getDestinationLockerId(),
        order.getReservedBoxId(),
        order.getDescription());
  }
}
