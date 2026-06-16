package com.huynqb.laundrylocker.iot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/// Audit trail for every box-open attempt: who (actor), with what credential
/// (PIN/QR vs MASTER override), and the outcome. Written by both the regular
/// customer unlock path and the maintenance force-unlock path.
@Entity
@Table(name = "box_access_logs")
@Getter
@Setter
public class BoxAccessLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "box_id", nullable = false)
  private Long boxId;

  @Column(name = "locker_id")
  private Long lockerId;

  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "actor_user_id")
  private Long actorUserId;

  @Column(name = "credential_type", nullable = false, length = 20)
  private String credentialType;

  @Column(nullable = false, length = 20)
  private String result;

  @Column(length = 255)
  private String message;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
