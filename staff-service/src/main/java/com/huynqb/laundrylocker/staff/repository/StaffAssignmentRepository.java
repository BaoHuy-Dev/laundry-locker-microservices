package com.huynqb.laundrylocker.staff.repository;

import com.huynqb.laundrylocker.staff.model.StaffAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {

  List<StaffAssignment> findByStaffId(Long staffId);
}
