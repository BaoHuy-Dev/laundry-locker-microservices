package com.huynqb.laundrylocker.order.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Stateless signed QR tokens for opening a locker cell.
 * Format: LLQR.&lt;orderId-base36&gt;.&lt;HMAC-SHA256(orderId:pinCode)-base64url&gt;
 * The signature binds the token to the CURRENT pin, so rotating the PIN
 * (delegation, reset, SEND hand-over) invalidates every QR issued before.
 */
@Service
public class QrTokenService {

  public static final String PREFIX = "LLQR.";

  @Value("${app.security.qr-secret:laundry-locker-dev-qr-secret}")
  private String secret;

  public String issue(Long orderId, String pinCode) {
    if (orderId == null || pinCode == null || pinCode.isBlank()) {
      return null;
    }
    return PREFIX + Long.toString(orderId, 36) + "." + sign(orderId, pinCode);
  }

  public Long parseOrderId(String token) {
    String[] parts = split(token);
    if (parts == null) {
      return null;
    }
    try {
      return Long.parseLong(parts[0], 36);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  public boolean matches(String token, Long orderId, String pinCode) {
    String[] parts = split(token);
    if (parts == null || orderId == null || pinCode == null) {
      return false;
    }
    byte[] expected = sign(orderId, pinCode).getBytes(StandardCharsets.UTF_8);
    byte[] actual = parts[1].getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expected, actual);
  }

  private String[] split(String token) {
    if (token == null || !token.startsWith(PREFIX)) {
      return null;
    }
    String[] parts = token.substring(PREFIX.length()).split("\\.", 2);
    return parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank() ? parts : null;
  }

  private String sign(Long orderId, String pinCode) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] sig = mac.doFinal((orderId + ":" + pinCode).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
    } catch (Exception ex) {
      throw new IllegalStateException("Cannot sign QR token", ex);
    }
  }
}
