package com.huynqb.laundrylocker.staff.service;

import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.staff.dto.StaffAssignmentRequest;
import com.huynqb.laundrylocker.staff.dto.StaffAssignmentResponse;
import com.huynqb.laundrylocker.staff.model.StaffAssignment;
import com.huynqb.laundrylocker.staff.repository.StaffAssignmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StaffService {

  private final StaffAssignmentRepository repository;

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

  private StaffAssignmentResponse toResponse(StaffAssignment assignment) {
    return new StaffAssignmentResponse(
        assignment.getId(), assignment.getStaffId(), assignment.getOrderId(), assignment.getLockerId(), assignment.getStatus());
  }
}
