package com.huynqb.laundrylocker.order.service;

import com.huynqb.laundrylocker.order.client.LockerCellClient;
import com.huynqb.laundrylocker.order.client.LockerClient;
import com.huynqb.laundrylocker.order.client.NotificationClient;
import com.huynqb.laundrylocker.order.client.UserClient;
import com.huynqb.laundrylocker.order.dto.CellDto;
import com.huynqb.laundrylocker.order.model.LockerOrder;
import com.huynqb.laundrylocker.order.repository.*;
import com.huynqb.laundrylocker.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceRentalPaymentTest {

    @Mock private LockerOrderRepository orderRepository;
    @Mock private OrderDetailRepository detailRepository;
    @Mock private OrderStatusHistoryRepository historyRepository;
    @Mock private OrderRatingRepository ratingRepository;
    @Mock private OrderComplaintRepository complaintRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionUsageRepository promotionUsageRepository;
    @Mock private PromotionClaimRepository promotionClaimRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private UserClient userClient;
    @Mock private LockerClient lockerClient;
    @Mock private LockerCellClient lockerCellClient;
    @Mock private NotificationClient notificationClient;
    @Mock private QrTokenService qrTokenService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                detailRepository,
                historyRepository,
                ratingRepository,
                complaintRepository,
                promotionRepository,
                promotionUsageRepository,
                promotionClaimRepository,
                rabbitTemplate,
                userClient,
                lockerClient,
                lockerCellClient,
                notificationClient,
                qrTokenService);
        ReflectionTestUtils.setField(orderService, "rentalRateStandard", 5000L);
        ReflectionTestUtils.setField(orderService, "requirePaymentBeforeDrop", true);
    }

    @Test
    void extendRentalMarksPreviouslyPaidRentalAsUnpaidUntilExtraChargeIsSettled() {
        LockerOrder order = new LockerOrder();
        order.setId(12L);
        order.setUserId(44L);
        order.setType("RENTAL");
        order.setStatus("STORING");
        order.setSendBoxId(901L);
        order.setPickupDeadline(LocalDateTime.now().plusHours(2));
        order.setTotalPrice(BigDecimal.valueOf(10000));
        order.setOriginalPrice(BigDecimal.valueOf(10000));
        order.setPaymentStatus("PAID");
        order.setPaidAt(LocalDateTime.now().minusHours(1));

        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));
        when(lockerCellClient.getCell(901L)).thenReturn(com.huynqb.laundrylocker.common.dto.ApiResponse.ok(
                new CellDto(901L, 1, "S", "STANDARD", 0, 0, "OCCUPIED", null)));
        when(orderRepository.save(any(LockerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.extendRental(12L, 44L, 2);

        assertEquals(BigDecimal.valueOf(20000), order.getTotalPrice());
        assertEquals(BigDecimal.valueOf(20000), order.getOriginalPrice());
        assertEquals("UNPAID", order.getPaymentStatus());
        assertNull(order.getPaidAt());
    }

    @Test
    void pickupStorageRejectsRentalWhenExtensionBalanceIsStillUnpaid() {
        LockerOrder order = new LockerOrder();
        order.setId(13L);
        order.setUserId(44L);
        order.setType("RENTAL");
        order.setStatus("STORING");
        order.setSendBoxId(902L);
        order.setTotalPrice(BigDecimal.valueOf(15000));
        order.setPaymentStatus("UNPAID");

        when(orderRepository.findById(13L)).thenReturn(Optional.of(order));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> orderService.pickupStorage(13L, 44L));

        assertEquals("ORDER_UNPAID", error.getCode());
        verify(lockerClient, never()).releaseBox(any());
    }
}
