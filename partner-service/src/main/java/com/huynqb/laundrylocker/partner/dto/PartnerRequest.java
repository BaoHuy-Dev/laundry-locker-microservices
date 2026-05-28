package com.huynqb.laundrylocker.partner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PartnerRequest(
    @NotNull Long userId,
    @NotBlank String businessName,
    String contactPhone,
    String contactEmail,
    String status) {}
