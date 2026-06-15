package com.huynqb.laundrylocker.locker.repository;

import com.huynqb.laundrylocker.locker.model.RepairLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepairLogRepository extends JpaRepository<RepairLog, Long> {

  List<RepairLog> findByReportIdOrderByCreatedAtAsc(Long reportId);
}
