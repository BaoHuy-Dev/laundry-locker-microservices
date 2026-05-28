package com.huynqb.laundrylocker.locker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "locker_reports")
@Getter
@Setter
public class LockerReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "locker_id", nullable = false)
  private Long lockerId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, length = 2000)
  private String description;

  @Column(nullable = false, length = 30)
  private String status = "OPEN";

  @Column(name = "resolved_by_user_id")
  private Long resolvedByUserId;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
