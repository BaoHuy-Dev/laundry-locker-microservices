package com.huynqb.laundrylocker.locker.repository;

import com.huynqb.laundrylocker.locker.model.DroneMaintenanceLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DroneMaintenanceLogRepository extends JpaRepository<DroneMaintenanceLog, Long> {

  List<DroneMaintenanceLog> findByDroneUnitIdOrderByCreatedAtAsc(Long droneUnitId);
}
