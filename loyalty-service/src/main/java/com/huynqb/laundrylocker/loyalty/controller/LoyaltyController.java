package com.huynqb.laundrylocker.loyalty.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.loyalty.dto.AdjustPointsRequest;
import com.huynqb.laundrylocker.loyalty.dto.LoyaltyAccountResponse;
import com.huynqb.laundrylocker.loyalty.dto.PointTransactionResponse;
import com.huynqb.laundrylocker.loyalty.dto.RedeemPointsRequest;
import com.huynqb.laundrylocker.loyalty.dto.RedeemStampRequest;
import com.huynqb.laundrylocker.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LoyaltyController {

  private final LoyaltyService loyaltyService;

  @GetMapping("/api/loyalty/users/{userId}")
  public ApiResponse<LoyaltyAccountResponse> getByUser(@PathVariable Long userId) {
    return ApiResponse.ok(loyaltyService.getByUser(userId));
  }

  @GetMapping("/api/loyalty/summary")
  public ApiResponse<LoyaltyAccountResponse> summary(@org.springframework.web.bind.annotation.RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok(loyaltyService.getByUser(userId));
  }

  @GetMapping("/api/loyalty/points")
  public ApiResponse<LoyaltyAccountResponse> points(@org.springframework.web.bind.annotation.RequestHeader("X-User-Id") Long userId) {
    return summary(userId);
  }

  @GetMapping("/api/loyalty/points/history")
  public ApiResponse<List<PointTransactionResponse>> history(@org.springframework.web.bind.annotation.RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok(loyaltyService.history(userId));
  }

  @PostMapping("/api/loyalty/redeem-points")
  public ApiResponse<LoyaltyAccountResponse> redeemPoints(@Valid @RequestBody RedeemPointsRequest request) {
    return ApiResponse.ok("POINTS_REDEEMED", "Points redeemed", loyaltyService.redeemPoints(request));
  }

  @PostMapping("/api/loyalty/redeem-stamp")
  public ApiResponse<LoyaltyAccountResponse> redeemStamp(@Valid @RequestBody RedeemStampRequest request) {
    return ApiResponse.ok("STAMP_REDEEMED", "Stamp redeemed", loyaltyService.redeemStamp(request));
  }

  @PostMapping("/api/loyalty/points")
  public ApiResponse<LoyaltyAccountResponse> adjustPoints(@Valid @RequestBody AdjustPointsRequest request) {
    return ApiResponse.ok("LOYALTY_POINTS_ADJUSTED", "Loyalty points adjusted", loyaltyService.adjustPoints(request));
  }

  @PostMapping("/api/admin/loyalty/users/{userId}/points")
  public ApiResponse<LoyaltyAccountResponse> adminAdjust(
      @PathVariable Long userId,
      @Valid @RequestBody AdjustPointsRequest request) {
    return ApiResponse.ok("LOYALTY_POINTS_ADJUSTED", "Loyalty points adjusted",
        loyaltyService.adjustPoints(new AdjustPointsRequest(userId, request.orderId(), request.points(), request.type())));
  }

  @GetMapping("/api/admin/loyalty/users/{userId}/history")
  public ApiResponse<List<PointTransactionResponse>> adminHistory(@PathVariable Long userId) {
    return ApiResponse.ok(loyaltyService.history(userId));
  }

  @GetMapping("/internal/loyalty/users/{userId}")
  public ApiResponse<LoyaltyAccountResponse> getByUserInternal(@PathVariable Long userId) {
    return ApiResponse.ok(loyaltyService.getByUser(userId));
  }
}
