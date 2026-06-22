package com.huynqb.laundrylocker.payment.repository;

import com.huynqb.laundrylocker.payment.model.Wallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
  Optional<Wallet> findByUserId(Long userId);
}
