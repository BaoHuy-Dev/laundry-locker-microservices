package com.huynqb.laundrylocker.partner.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_access_codes")
@Getter
@Setter
public class StaffAccessCode {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "partner_id", nullable = false)
  private Long partnerId;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false, length = 30)
  private String action;

  @Column(nullable = false, length = 30)
  private String status = "ACTIVE";

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;
}
