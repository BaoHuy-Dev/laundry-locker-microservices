package com.huynqb.laundrylocker.staff.service;

import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.staff.client.LockerClient;
import com.huynqb.laundrylocker.staff.dto.StaffAssignmentRequest;
import com.huynqb.laundrylocker.staff.dto.StaffAssignmentResponse;
import com.huynqb.laundrylocker.staff.model.StaffAssignment;
import com.huynqb.laundrylocker.staff.repository.StaffAssignmentRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StaffService {

  private final StaffAssignmentRepository repository;
  private final LockerClient lockerClient;

  @Value("${app.staff.master-pin:000000}")
  private String masterPin;

  @Transactional
  public StaffAssignmentResponse assign(StaffAssignmentRequest request) {
    StaffAssignment assignment = new StaffAssignment();
    assignment.setStaffId(request.staffId());
    assignment.setOrderId(request.orderId());
    assignment.setLockerId(request.lockerId());
    assignment.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ASSIGNED");
    return toResponse(repository.save(assignment));
  }

  @Transactional(readOnly = true)
  public StaffAssignmentResponse get(Long id) {
    return toResponse(repository.findById(id).orElseThrow(() -> new NotFoundException("StaffAssignment", id)));
  }

  @Transactional(readOnly = true)
  public List<StaffAssignmentResponse> list(Long staffId) {
    List<StaffAssignment> rows = staffId == null ? repository.findAll() : repository.findByStaffId(staffId);
    return rows.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> lockers(Long storeId) {
    try {
      return lockerClient.lockers(storeId).data();
    } catch (Exception ex) {
      return List.of();
    }
  }

  public Map<String, Object> unlockBox(Map<String, Object> request, Long staffId) {
    Long boxId = Long.valueOf(String.valueOf(request.get("boxId")));
    String providedPin = String.valueOf(request.getOrDefault("masterPin", ""));
    if (!masterPin.equals(providedPin)) {
      return Map.of("success", false, "boxId", boxId, "message", "Invalid master PIN");
    }
    try {
      lockerClient.openBox(boxId);
      java.util.HashMap<String, Object> response = new java.util.HashMap<>();
      response.put("success", true);
      response.put("boxId", boxId);
      response.put("orderId", request.get("orderId"));
      response.put("staffId", staffId);
      response.put("unlockToken", java.util.UUID.randomUUID().toString());
      return response;
    } catch (Exception ex) {
      return Map.of("success", false, "boxId", boxId, "message", ex.getMessage());
    }
  }

  private StaffAssignmentResponse toResponse(StaffAssignment assignment) {
    return new StaffAssignmentResponse(
        assignment.getId(), assignment.getStaffId(), assignment.getOrderId(), assignment.getLockerId(), assignment.getStatus());
  }
}
