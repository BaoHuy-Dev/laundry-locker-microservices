package com.huynqb.laundrylocker.locker.repository;

import com.huynqb.laundrylocker.locker.model.DroneDeliveryRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DroneDeliveryRequestRepository extends JpaRepository<DroneDeliveryRequest, Long> {

  List<DroneDeliveryRequest> findAllByOrderByCreatedAtDesc();

  List<DroneDeliveryRequest> findByStatusOrderByCreatedAtDesc(String status);

  List<DroneDeliveryRequest> findByRequesterUserIdOrderByCreatedAtDesc(Long requesterUserId);
}
