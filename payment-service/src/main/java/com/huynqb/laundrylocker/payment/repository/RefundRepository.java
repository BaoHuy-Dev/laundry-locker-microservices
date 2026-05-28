package com.huynqb.laundrylocker.payment.repository;

import com.huynqb.laundrylocker.payment.model.RefundRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<RefundRecord, Long> {

  List<RefundRecord> findByOrderId(Long orderId);
}
