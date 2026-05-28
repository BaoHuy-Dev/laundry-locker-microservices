package com.huynqb.laundrylocker.laundry.dto;

import java.math.BigDecimal;

public record LaundryCatalogResponse(
    Long id,
    Long storeId,
    String name,
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
