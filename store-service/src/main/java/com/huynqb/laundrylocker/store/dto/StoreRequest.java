package com.huynqb.laundrylocker.store.dto;

import jakarta.validation.constraints.NotBlank;

public record StoreRequest(
    Long partnerId,
    @NotBlank String name,
    String contactPhone,
    String address,
    Double latitude,
    Double longitude,
    String image,
    String description,
    Boolean active,
    String status) {}
