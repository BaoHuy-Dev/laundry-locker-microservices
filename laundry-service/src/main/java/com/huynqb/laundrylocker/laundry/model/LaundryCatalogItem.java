package com.huynqb.laundrylocker.laundry.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "laundry_catalog_items")
@Getter
@Setter
public class LaundryCatalogItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "store_id")
  private Long storeId;

  @Column(nullable = false)
  private String name;

  @Column(length = 50)
  private String category = "LAUNDRY";

  @Column(length = 50)
  private String serviceType = "WASH";

  @Column(name = "unit_price", precision = 12, scale = 2)
  private BigDecimal unitPrice = BigDecimal.ZERO;

  @Column(name = "max_price", precision = 12, scale = 2)
  private BigDecimal maxPrice;

  @Column(length = 50)
  private String unit = "kg";

  @Column(length = 2000)
  private String description;

  @Column(length = 1000)
  private String image;

  @Column(name = "is_addon")
  private Boolean addon = false;

  @Column(name = "is_monthly_package")
  private Boolean monthlyPackage = false;

  @Column(name = "estimated_hours")
  private Integer estimatedHours;

  @Column(length = 30)
  private String status = "ACTIVE";

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
