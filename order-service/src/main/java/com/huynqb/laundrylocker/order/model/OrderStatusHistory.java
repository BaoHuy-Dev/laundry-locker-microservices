package com.huynqb.laundrylocker.order.model;

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
@Table(name = "order_status_history")
@Getter
@Setter
public class OrderStatusHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Column(name = "old_status", length = 30)
  private String oldStatus;

  @Column(name = "new_status", nullable = false, length = 30)
  private String newStatus;

  @Column(name = "changed_by_user_id")
  private Long changedByUserId;

  @Column(length = 1000)
  private String note;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
