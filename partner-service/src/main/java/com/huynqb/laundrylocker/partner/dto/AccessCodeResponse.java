package com.huynqb.laundrylocker.partner.dto;

import java.time.LocalDateTime;

public record AccessCodeResponse(Long id, Long partnerId, Long orderId, String code, String action, String status, LocalDateTime expiresAt) {}
