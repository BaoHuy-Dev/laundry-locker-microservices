package com.huynqb.laundrylocker.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/// Một lần áp mã vào một đơn — dùng để enforce per_user_limit và hoàn lượt
/// khi đơn bị hủy.
@Entity
@Table(name = "promotion_usages")
@Getter
@Setter
public class PromotionUsage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "promotion_id", nullable = false)
  private Long promotionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "discount_applied", precision = 12, scale = 2)
  private BigDecimal discountApplied;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
