package com.huynqb.laundrylocker.payment.repository;

import com.huynqb.laundrylocker.payment.model.WalletTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
  List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

  boolean existsBySourceAndReferenceId(String source, String referenceId);
}
