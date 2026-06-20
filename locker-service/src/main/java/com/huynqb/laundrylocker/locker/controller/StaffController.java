package com.huynqb.laundrylocker.locker.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.locker.dto.LockerLayoutResponse;
import com.huynqb.laundrylocker.locker.dto.LockerResponse;
import com.huynqb.laundrylocker.locker.dto.LockerStatsResponse;
import com.huynqb.laundrylocker.locker.service.LockerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only locker views for STAFF role. Gateway enforces STAFF|ADMIN before
 * the request reaches this controller; no re-validation needed here.
 */
@RestController
@RequiredArgsConstructor
public class StaffController {

    private final LockerService lockerService;

    /** List all lockers, optionally filtered by store. */
    @GetMapping("/api/staff/lockers")
    public ApiResponse<List<LockerResponse>> listLockers(
            @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(lockerService.listLockers(storeId));
    }

    /** Grid layout (rows/columns) of a single locker's boxes. */
    @GetMapping("/api/staff/lockers/{id}/layout")
    public ApiResponse<LockerLayoutResponse> layout(@PathVariable Long id) {
        return ApiResponse.ok(lockerService.layout(id));
    }

    /** Per-locker occupancy stats: available / reserved / occupied / fault counts. */
    @GetMapping("/api/staff/lockers/stats")
    public ApiResponse<List<LockerStatsResponse>> stats(
            @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(lockerService.stats(storeId));
    }
}
