package com.huynqb.laundrylocker.notification.model;

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

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class NotificationMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String message;

  @Column(nullable = false, length = 50)
  private String type = "SYSTEM";

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(name = "reference_type", length = 50)
  private String referenceType;

  @Column(nullable = false, length = 20)
  private String status = "UNREAD";

  @Column(name = "is_read", nullable = false)
  private Boolean isRead = false;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    if (isRead == null) {
      isRead = false;
    }
    if (status == null) {
      status = "UNREAD";
    }
    createdAt = LocalDateTime.now();
  }
}
