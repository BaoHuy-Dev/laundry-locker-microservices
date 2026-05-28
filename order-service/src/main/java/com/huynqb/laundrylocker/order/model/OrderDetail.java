package com.huynqb.laundrylocker.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_details")
@Getter
@Setter
public class OrderDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Column(name = "service_id", nullable = false)
  private Long serviceId;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal quantity = BigDecimal.ONE;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price = BigDecimal.ZERO;

  @Column(length = 1000)
  private String description;
}
