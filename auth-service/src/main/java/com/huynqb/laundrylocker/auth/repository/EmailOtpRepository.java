package com.huynqb.laundrylocker.auth.repository;

import com.huynqb.laundrylocker.auth.model.EmailOtp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

  Optional<EmailOtp> findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
      String email, String purpose);
}
