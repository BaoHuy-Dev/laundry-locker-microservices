package com.huynqb.laundrylocker.payment.repository;

import com.huynqb.laundrylocker.payment.model.PaymentRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentRecord, Long> {

  List<PaymentRecord> findByOrderId(Long orderId);
}
