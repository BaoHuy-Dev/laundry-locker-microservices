package com.huynqb.laundrylocker.staff.dto;

import jakarta.validation.constraints.NotNull;

public record StaffAssignmentRequest(@NotNull Long staffId, @NotNull Long orderId, Long lockerId, String status) {}
