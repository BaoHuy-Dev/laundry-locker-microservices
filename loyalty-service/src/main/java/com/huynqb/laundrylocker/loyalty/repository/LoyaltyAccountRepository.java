package com.huynqb.laundrylocker.loyalty.repository;

import com.huynqb.laundrylocker.loyalty.model.LoyaltyAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {

  Optional<LoyaltyAccount> findByUserId(Long userId);
}
