package com.huynqb.laundrylocker.payment.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.payment.dto.WalletAdjustRequest;
import com.huynqb.laundrylocker.payment.dto.WalletResponse;
import com.huynqb.laundrylocker.payment.dto.WalletTransactionResponse;
import com.huynqb.laundrylocker.payment.service.WalletService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WalletController {

  private final WalletService walletService;

  @GetMapping("/api/wallet")
  public ApiResponse<WalletResponse> balance(@RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok("WALLET_OK", "Wallet balance", walletService.getBalance(userId));
  }

  @GetMapping("/api/wallet/transactions")
  public ApiResponse<List<WalletTransactionResponse>> transactions(
      @RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok(walletService.history(userId));
  }

  @GetMapping("/internal/wallet/{userId}")
  public ApiResponse<WalletResponse> internalBalance(@PathVariable Long userId) {
    return ApiResponse.ok(walletService.getBalance(userId));
  }

  @GetMapping("/api/admin/wallet/{userId}")
  public ApiResponse<WalletResponse> adminBalance(@PathVariable Long userId) {
    return ApiResponse.ok(walletService.getBalance(userId));
  }

  @GetMapping("/api/admin/wallet/{userId}/transactions")
  public ApiResponse<List<WalletTransactionResponse>> adminTransactions(@PathVariable Long userId) {
    return ApiResponse.ok(walletService.history(userId));
  }

  @PostMapping("/api/admin/wallet/{userId}/adjust")
  public ApiResponse<WalletResponse> adjust(
      @PathVariable Long userId, @Valid @RequestBody WalletAdjustRequest request) {
    return ApiResponse.ok(
        "WALLET_ADJUSTED",
        "Wallet adjusted",
        walletService.adjust(userId, request.amount(), request.reason()));
  }
}
