package com.huynqb.laundrylocker.payment.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.payment.dto.CreatePaymentRequest;
import com.huynqb.laundrylocker.payment.dto.PaymentResponse;
import com.huynqb.laundrylocker.payment.dto.RefundRequest;
import com.huynqb.laundrylocker.payment.dto.RefundResponse;
import com.huynqb.laundrylocker.payment.dto.UpdatePaymentStatusRequest;
import com.huynqb.laundrylocker.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/api/payments")
  public ApiResponse<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
    return ApiResponse.ok("PAYMENT_CREATED", "Payment created", paymentService.create(request));
  }

  @PatchMapping("/api/payments/{id}/status")
  public ApiResponse<PaymentResponse> updateStatus(
      @PathVariable Long id, @Valid @RequestBody UpdatePaymentStatusRequest request) {
    return ApiResponse.ok("PAYMENT_STATUS_UPDATED", "Payment status updated", paymentService.updateStatus(id, request));
  }

  @GetMapping("/api/payments/{id}")
  public ApiResponse<PaymentResponse> get(@PathVariable Long id) {
    return ApiResponse.ok(paymentService.get(id));
  }

  @GetMapping("/api/payments")
  public ApiResponse<List<PaymentResponse>> listByOrder(@RequestParam Long orderId) {
    return ApiResponse.ok(paymentService.listByOrder(orderId));
  }

  @GetMapping("/api/payments/vnpay/return")
  public ApiResponse<PaymentResponse> vnpayReturn(@RequestParam Map<String, String> params) {
    return ApiResponse.ok("PAYMENT_RETURN_PROCESSED", "VNPay return processed", paymentService.handleVnPayReturn(params));
  }

  @GetMapping("/api/payments/vnpay/ipn")
  public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
    paymentService.handleVnPayReturn(params);
    return Map.of("RspCode", "00", "Message", "Success");
  }

  @PostMapping("/api/payments/momo/callback")
  public ApiResponse<Void> momoCallback(@RequestBody Map<String, Object> params) {
    return ApiResponse.ok("MOMO_CALLBACK_RECEIVED", "MoMo callback received");
  }

  @GetMapping("/api/payments/momo/return")
  public ApiResponse<Void> momoReturn() {
    return ApiResponse.ok("MOMO_RETURN_RECEIVED", "MoMo return received");
  }

  @PostMapping("/api/payments/{paymentId}/refund")
  public ApiResponse<RefundResponse> refund(
      @PathVariable Long paymentId,
      @Valid @RequestBody RefundRequest request,
      @RequestHeader(value = "X-User-Id", required = false) Long userId) {
    return ApiResponse.ok("REFUND_CREATED", "Refund created", paymentService.refund(paymentId, request, userId));
  }

  @GetMapping("/api/payments/order/{orderId}/refunds")
  public ApiResponse<List<RefundResponse>> refunds(@PathVariable Long orderId) {
    return ApiResponse.ok(paymentService.refundsByOrder(orderId));
  }

  @GetMapping("/api/admin/payments")
  public ApiResponse<List<PaymentResponse>> adminList() {
    return ApiResponse.ok(paymentService.listAll());
  }

  @PatchMapping("/api/admin/payments/{id}/status")
  public ApiResponse<PaymentResponse> adminStatus(
      @PathVariable Long id, @Valid @RequestBody UpdatePaymentStatusRequest request) {
    return updateStatus(id, request);
  }

  @GetMapping("/internal/payments/{id}")
  public ApiResponse<PaymentResponse> getInternal(@PathVariable Long id) {
    return ApiResponse.ok(paymentService.get(id));
  }
}
