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
    String htmlTemplate = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <style>
            body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 0; }
            .container { max-width: 500px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1); }
            .header { background: linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%); padding: 32px 24px; text-align: center; }
            .header h1 { margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; letter-spacing: -0.5px; }
            .content { padding: 40px 32px; color: #334155; text-align: center; }
            .greeting { font-size: 18px; font-weight: 600; color: #0f172a; margin-bottom: 16px; }
            .message { font-size: 15px; line-height: 1.6; color: #475569; margin-bottom: 32px; }
            .otp-container { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 24px; margin-bottom: 32px; }
            .otp-code { font-size: 42px; font-weight: 800; color: #1e293b; letter-spacing: 8px; font-family: monospace; }
            .expiry { font-size: 14px; color: #ef4444; font-weight: 500; display: inline-flex; align-items: center; justify-content: center; width: 100%; gap: 6px; }
            .footer { background-color: #f8fafc; padding: 24px; text-align: center; border-top: 1px solid #e2e8f0; }
            .footer-text { font-size: 13px; color: #64748b; line-height: 1.5; margin: 0; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>Smart Locker</h1>
            </div>
            <div class="content">
              <div class="greeting">Secure Verification</div>
              <div class="message">
                Here is your One-Time Password (OTP) to securely access your account. Please do not share this code with anyone.
              </div>
              <div class="otp-container">
                <div class="otp-code">%s</div>
              </div>
              <div class="expiry">
                \u23F3 This code expires in 5 minutes
              </div>
            </div>
            <div class="footer">
              <p class="footer-text">
                &copy; 2026 Smart Laundry Locker.<br>
                If you did not request this code, you can safely ignore this email.
              </p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(code);

    emailService.sendHtmlEmail(
        email,
        "Laundry Locker OTP",
        htmlTemplate);
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
