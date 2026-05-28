package com.huynqb.laundrylocker.loyalty.repository;

import com.huynqb.laundrylocker.loyalty.model.PointTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

  List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
