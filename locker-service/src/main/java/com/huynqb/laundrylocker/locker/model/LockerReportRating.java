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

/// Customer feedback on how maintenance handled their fault report — the
/// other half of the claim/resolve notification loop (closes it both ways).
@Entity
@Table(name = "locker_report_ratings")
@Getter
@Setter
public class LockerReportRating {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "report_id", nullable = false, unique = true)
  private Long reportId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Integer rating;

  @Column(length = 1000)
  private String comment;

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
