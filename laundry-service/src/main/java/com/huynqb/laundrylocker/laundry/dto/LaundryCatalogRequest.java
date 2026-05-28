package com.huynqb.laundrylocker.laundry.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record LaundryCatalogRequest(
    Long storeId,
    @NotBlank String name,
    String category,
    String serviceType,
    BigDecimal unitPrice,
    BigDecimal maxPrice,
    String unit,
    String description,
    String image,
    Boolean addon,
    Boolean monthlyPackage,
    Integer estimatedHours,
    String status) {}
