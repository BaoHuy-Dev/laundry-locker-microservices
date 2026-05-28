package com.huynqb.laundrylocker.partner.dto;

import jakarta.validation.constraints.NotNull;

public record AccessCodeRequest(@NotNull Long partnerId, @NotNull Long orderId, String action, Integer expiresInHours) {}
