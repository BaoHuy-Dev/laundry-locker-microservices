package com.huynqb.laundrylocker.locker.dto;

public record FaultCellResponse(
    Long lockerId,
    String lockerCode,
    String lockerName,
    Long boxId,
    Integer boxNumber,
    String cellType,
    Integer rowIndex,
    Integer colIndex,
    String faultReason,
    Long openReportId) {}
