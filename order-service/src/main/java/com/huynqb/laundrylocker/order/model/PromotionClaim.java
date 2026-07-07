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

/// "Ví voucher" của user: user lưu một mã khuyến mãi từ trang ưu đãi;
/// khi mã được áp vào đơn thì claim chuyển SAVED -> USED.
@Entity
@Table(name = "promotion_claims")
@Getter
@Setter
public class PromotionClaim {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "promotion_id", nullable = false)
  private Long promotionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(nullable = false, length = 20)
  private String status = "SAVED";

  @Column(name = "used_at")
  private LocalDateTime usedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
