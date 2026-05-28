package com.huynqb.laundrylocker.auth.repository;

import com.huynqb.laundrylocker.auth.model.AuthAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {

  Optional<AuthAccount> findByEmail(String email);

  Optional<AuthAccount> findByPhoneNumber(String phoneNumber);
}
