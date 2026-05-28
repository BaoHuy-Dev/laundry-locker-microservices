package com.huynqb.laundrylocker.order.repository;

import com.huynqb.laundrylocker.order.model.LaundryOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaundryOrderRepository extends JpaRepository<LaundryOrder, Long> {

  List<LaundryOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<LaundryOrder> findByStatusOrderByCreatedAtDesc(String status);

  List<LaundryOrder> findByStaffIdOrderByCreatedAtDesc(Long staffId);

  Optional<LaundryOrder> findByOrderCode(String orderCode);

  Optional<LaundryOrder> findByPinCode(String pinCode);
}
