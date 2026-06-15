package com.huynqb.laundrylocker.locker.repository;

import com.huynqb.laundrylocker.locker.model.MaintenanceSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Long> {

  List<MaintenanceSchedule> findByActiveTrueOrderByNextDueAtAsc();
}
