package com.huynqb.laundrylocker.iot.repository;

import com.huynqb.laundrylocker.iot.model.BoxHardwareStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoxHardwareStatusRepository extends JpaRepository<BoxHardwareStatus, Long> {

  List<BoxHardwareStatus> findAllByOrderByLastReportedAtDesc();

  List<BoxHardwareStatus> findByLockerIdOrderByLastReportedAtDesc(Long lockerId);
}
