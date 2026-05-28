package com.huynqb.laundrylocker.order.repository;

import com.huynqb.laundrylocker.order.model.OrderComplaint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderComplaintRepository extends JpaRepository<OrderComplaint, Long> {

  List<OrderComplaint> findByOrderIdOrderByCreatedAtDesc(Long orderId);

  List<OrderComplaint> findByUserIdOrderByCreatedAtDesc(Long userId);
}
