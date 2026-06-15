package com.huynqb.laundrylocker.locker.dto;

import java.time.LocalDateTime;

public record MaintenanceScheduleResponse(
    Long id,
    Long lockerId,
    String lockerName,
    String lockerCode,
    String title,
    Integer intervalDays,
    LocalDateTime lastDoneAt,
    LocalDateTime nextDueAt,
    Boolean active,
    Boolean due) {}
