package com.huynqb.laundrylocker.locker.dto;

public record CellResponse(
    Long id,
    Integer boxNumber,
    String size,
    String cellType,
    Integer rowIndex,
    Integer colIndex,
    String status,
    String faultReason) {}
