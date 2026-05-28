package com.huynqb.laundrylocker.locker.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.common.dto.LockerBoxSummary;
import com.huynqb.laundrylocker.locker.dto.BoxRequest;
import com.huynqb.laundrylocker.locker.dto.LockerRequest;
import com.huynqb.laundrylocker.locker.dto.LockerReportRequest;
import com.huynqb.laundrylocker.locker.dto.LockerReportResponse;
import com.huynqb.laundrylocker.locker.dto.LockerResponse;
import com.huynqb.laundrylocker.locker.service.LockerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LockerController {

  private final LockerService lockerService;

  @PostMapping("/api/lockers")
  public ApiResponse<LockerResponse> createLocker(@Valid @RequestBody LockerRequest request) {
    return ApiResponse.ok("LOCKER_CREATED", "Locker created", lockerService.createLocker(request));
  }

  @GetMapping("/api/lockers")
  public ApiResponse<List<LockerResponse>> listLockers(@RequestParam(required = false) Long storeId) {
    return ApiResponse.ok(lockerService.listLockers(storeId));
  }

  @GetMapping("/api/lockers/{id}")
  public ApiResponse<LockerResponse> getLocker(@PathVariable Long id) {
    return ApiResponse.ok(lockerService.getLocker(id));
  }

  @PostMapping("/api/boxes")
  public ApiResponse<LockerBoxSummary> createBox(@Valid @RequestBody BoxRequest request) {
    return ApiResponse.ok("BOX_CREATED", "Box created", lockerService.createBox(request));
  }

  @GetMapping("/api/lockers/{lockerId}/boxes")
  public ApiResponse<List<LockerBoxSummary>> listBoxes(@PathVariable Long lockerId) {
    return ApiResponse.ok(lockerService.listBoxes(lockerId));
  }

  @GetMapping("/api/lockers/{lockerId}/boxes/available")
  public ApiResponse<List<LockerBoxSummary>> listAvailableBoxes(@PathVariable Long lockerId) {
    return ApiResponse.ok(lockerService.listAvailableBoxes(lockerId));
  }

  @PostMapping("/api/boxes/{id}/open")
  public ApiResponse<LockerBoxSummary> openBox(@PathVariable Long id) {
    return ApiResponse.ok("BOX_OPENED", "Box open event published", lockerService.openBox(id));
  }

  @GetMapping("/internal/boxes/{id}")
  public ApiResponse<LockerBoxSummary> getBoxInternal(@PathVariable Long id) {
    return ApiResponse.ok(lockerService.getBox(id));
  }

  @PostMapping("/internal/boxes/{id}/reserve")
  public ApiResponse<LockerBoxSummary> reserveBox(@PathVariable Long id) {
    return ApiResponse.ok("BOX_RESERVED", "Box reserved", lockerService.reserveBox(id));
  }

  @PostMapping("/internal/boxes/{id}/release")
  public ApiResponse<LockerBoxSummary> releaseBox(@PathVariable Long id) {
    return ApiResponse.ok("BOX_RELEASED", "Box released", lockerService.releaseBox(id));
  }

  @PostMapping("/api/lockers/{id}/report")
  public ApiResponse<LockerReportResponse> report(@PathVariable Long id, @Valid @RequestBody LockerReportRequest request) {
    return ApiResponse.ok("LOCKER_REPORTED", "Locker report created", lockerService.report(id, request));
  }

  @GetMapping("/api/lockers/my-reports")
  public ApiResponse<List<LockerReportResponse>> myReports(@RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok(lockerService.myReports(userId));
  }

  @GetMapping("/api/admin/lockers/reports")
  public ApiResponse<List<LockerReportResponse>> adminReports(@RequestParam Long userId) {
    return ApiResponse.ok(lockerService.myReports(userId));
  }

  @PutMapping("/api/admin/lockers/reports/{id}/resolve")
  public ApiResponse<LockerReportResponse> resolve(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
    return ApiResponse.ok("LOCKER_REPORT_RESOLVED", "Locker report resolved", lockerService.resolveReport(id, userId));
  }
}
