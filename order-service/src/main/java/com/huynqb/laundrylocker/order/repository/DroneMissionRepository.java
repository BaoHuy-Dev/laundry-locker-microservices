package com.huynqb.laundrylocker.order.repository;

import com.huynqb.laundrylocker.order.model.DroneMission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DroneMissionRepository extends JpaRepository<DroneMission, Long> {

  Optional<DroneMission> findByOrderId(Long orderId);
}
