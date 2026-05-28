package com.huynqb.laundrylocker.order.repository;

import com.huynqb.laundrylocker.order.model.OrderStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

  List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
