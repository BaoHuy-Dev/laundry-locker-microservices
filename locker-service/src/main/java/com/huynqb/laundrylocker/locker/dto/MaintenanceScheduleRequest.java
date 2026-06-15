package com.huynqb.laundrylocker.locker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaintenanceScheduleRequest(
    @NotNull Long lockerId,
    @NotBlank String title,
    @NotNull @Min(1) @Max(365) Integer intervalDays) {}
