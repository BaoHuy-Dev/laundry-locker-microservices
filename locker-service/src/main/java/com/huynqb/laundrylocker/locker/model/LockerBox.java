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
@Table(name = "locker_boxes")
@Getter
@Setter
public class LockerBox {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "locker_id", nullable = false)
  private Long lockerId;

  @Column(name = "box_number", nullable = false)
  private Integer boxNumber;

  @Column(length = 30)
  private String size = "MEDIUM";

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;

  @Column(length = 500)
  private String description;

  @Column(nullable = false, length = 30)
  private String status = "AVAILABLE";

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
