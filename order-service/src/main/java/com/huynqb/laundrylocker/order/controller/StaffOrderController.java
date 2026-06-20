package com.huynqb.laundrylocker.order.controller;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.order.dto.OrderResponse;
import com.huynqb.laundrylocker.order.service.OrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order visibility for STAFF role. Gateway enforces STAFF|ADMIN.
 * Delegates to OrderService.manageList — same query as MANAGER.
 */
@RestController
@RequiredArgsConstructor
public class StaffOrderController {

    private final OrderService orderService;

    /**
     * Returns orders filtered by optional status, type, and lockerId.
     * STAFF sees the same order list as MANAGER — all orders, not only their own.
     */
    @GetMapping("/api/staff/orders")
    public ApiResponse<List<OrderResponse>> activeOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long lockerId) {
        return ApiResponse.ok(orderService.manageList(status, type, lockerId));
    }
}
