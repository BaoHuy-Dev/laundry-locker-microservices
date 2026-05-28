package com.huynqb.laundrylocker.iot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BoxStatusUpdateRequest(@NotNull Long boxId, @NotBlank String status) {}
