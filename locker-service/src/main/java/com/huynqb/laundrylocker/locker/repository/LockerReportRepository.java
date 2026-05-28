package com.huynqb.laundrylocker.locker.repository;

import com.huynqb.laundrylocker.locker.model.LockerReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockerReportRepository extends JpaRepository<LockerReport, Long> {

  List<LockerReport> findByUserIdOrderByCreatedAtDesc(Long userId);
}
