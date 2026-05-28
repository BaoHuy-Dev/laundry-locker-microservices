package com.huynqb.laundrylocker.store.dto;

public record StoreResponse(
    Long id,
    Long partnerId,
    String name,
    String contactPhone,
    String address,
    Double latitude,
    Double longitude,
    String image,
    String description,
    Boolean active,
    Double distanceKm,
    String status) {}
