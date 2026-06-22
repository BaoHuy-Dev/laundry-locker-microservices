package com.huynqb.laundrylocker.iot.repository;

import com.huynqb.laundrylocker.iot.model.BoxAccessLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoxAccessLogRepository extends JpaRepository<BoxAccessLog, Long> {

  List<BoxAccessLog> findByLockerIdOrderByCreatedAtDesc(Long lockerId);
}
