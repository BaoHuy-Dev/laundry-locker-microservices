package com.huynqb.laundrylocker.order.repository;

import com.huynqb.laundrylocker.order.model.LockerOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockerOrderRepository extends JpaRepository<LockerOrder, Long> {

  List<LockerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<LockerOrder> findByStatusOrderByCreatedAtDesc(String status);

  List<LockerOrder> findByStaffIdOrderByCreatedAtDesc(Long staffId);

  Optional<LockerOrder> findByOrderCode(String orderCode);

  Optional<LockerOrder> findByPinCode(String pinCode);
}
