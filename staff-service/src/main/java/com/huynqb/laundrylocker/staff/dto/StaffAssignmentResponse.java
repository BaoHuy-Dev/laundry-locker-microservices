package com.huynqb.laundrylocker.staff.dto;

public record StaffAssignmentResponse(Long id, Long staffId, Long orderId, Long lockerId, String status) {}
