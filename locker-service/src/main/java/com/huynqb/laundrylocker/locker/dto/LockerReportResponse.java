package com.huynqb.laundrylocker.locker.dto;

import java.time.LocalDateTime;

public record LockerReportResponse(
    Long id,
    Long lockerId,
    Long userId,
    String title,
    String description,
    String status,
    Long resolvedByUserId,
    LocalDateTime resolvedAt,
    LocalDateTime createdAt) {}
