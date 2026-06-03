package com.huynqb.laundrylocker.auth.service;

import com.huynqb.laundrylocker.auth.email.EmailService;
import com.huynqb.laundrylocker.auth.model.EmailOtp;
import com.huynqb.laundrylocker.auth.repository.EmailOtpRepository;
import java.security.SecureRandom;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final EmailOtpRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  @Transactional
  public boolean sendOtp(String email, String purpose) {
    String code = String.format("%06d", RANDOM.nextInt(1_000_000));
    EmailOtp otp = new EmailOtp();
    otp.setEmail(normalize(email));
    otp.setPurpose(purpose);
    otp.setOtpHash(passwordEncoder.encode(code));
    otp.setExpiresAt(Instant.now().plusSeconds(300));
    repository.save(otp);
    emailService.sendSimpleEmail(
        email,
        "Laundry Locker OTP",
        "Your Laundry Locker OTP is " + code + ". It expires in 5 minutes.");
    log.info("OTP generated for {} purpose {}. Development OTP: {}", email, purpose, code);
    return true;
  }

  @Transactional
  public boolean verifyOtp(String email, String purpose, String code) {
    return repository
        .findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(normalize(email), purpose)
        .filter(otp -> otp.getExpiresAt().isAfter(Instant.now()))
        .filter(otp -> passwordEncoder.matches(code, otp.getOtpHash()))
        .map(
            otp -> {
              otp.setUsed(true);
              return true;
            })
        .orElse(false);
  }

  private String normalize(String email) {
    return email == null ? "" : email.trim().toLowerCase();
  }
}
