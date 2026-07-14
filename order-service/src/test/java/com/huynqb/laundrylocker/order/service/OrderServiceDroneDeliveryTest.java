package com.huynqb.laundrylocker.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.common.dto.LockerBoxSummary;
import com.huynqb.laundrylocker.common.dto.UserSummary;
import com.huynqb.laundrylocker.order.client.LockerCellClient;
import com.huynqb.laundrylocker.order.client.LockerClient;
import com.huynqb.laundrylocker.order.client.NotificationClient;
import com.huynqb.laundrylocker.order.client.UserClient;
import com.huynqb.laundrylocker.order.dto.CellDto;
import com.huynqb.laundrylocker.order.dto.CreateDroneDeliveryOrderRequest;
import com.huynqb.laundrylocker.order.dto.DroneDeliveryOrderResponse;
import com.huynqb.laundrylocker.order.model.LockerOrder;
import com.huynqb.laundrylocker.order.repository.LockerOrderRepository;
import com.huynqb.laundrylocker.order.repository.OrderComplaintRepository;
import com.huynqb.laundrylocker.order.repository.OrderDetailRepository;
import com.huynqb.laundrylocker.order.repository.OrderRatingRepository;
import com.huynqb.laundrylocker.order.repository.OrderStatusHistoryRepository;
import com.huynqb.laundrylocker.order.repository.PromotionClaimRepository;
import com.huynqb.laundrylocker.order.repository.PromotionRepository;
import com.huynqb.laundrylocker.order.repository.PromotionUsageRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceDroneDeliveryTest {

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
    orderService =
        new OrderService(
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
    ReflectionTestUtils.setField(orderService, "sendBaseFee", 15000L);
  }

  @Test
  void createDroneDeliveryReturnsExistingOrderForSameIdempotencyKey() {
    LockerOrder existing = droneOrder(11L, 44L, 5L, 9001L, "idem-1");
    when(orderRepository.findByUserIdAndIdempotencyKey(44L, "idem-1")).thenReturn(Optional.of(existing));

    DroneDeliveryOrderResponse response =
        orderService.createDroneDelivery(
            new CreateDroneDeliveryOrderRequest(5L, 9001L, "Tai lieu", 1200, "CASH"), 44L, "idem-1");

    assertEquals(existing.getId(), response.orderId());
    assertEquals(existing.getReservedBoxId(), response.reservedBoxId());
    verify(lockerClient, never()).reserveBox(any(), any());
    verify(orderRepository, never()).save(any());
  }

  @Test
  void createDroneDeliveryReservesDroneBoxAndPersistsOrder() {
    when(orderRepository.findByUserIdAndIdempotencyKey(44L, "idem-2")).thenReturn(Optional.empty());
    when(userClient.getUser(44L))
        .thenReturn(ApiResponse.ok(new UserSummary(44L, "u@test", "0901", "User", "ACTIVE")));
    when(lockerCellClient.getCell(9001L))
        .thenReturn(ApiResponse.ok(new CellDto(9001L, 7, "M", "DRONE", 0, 0, "AVAILABLE", null)));
    when(lockerClient.reserveBox(9001L, "DRONE"))
        .thenReturn(ApiResponse.ok(new LockerBoxSummary(5L, 9001L, "CAB-05", 7, "RESERVED")));
    when(orderRepository.save(any(LockerOrder.class)))
        .thenAnswer(invocation -> {
          LockerOrder saved = invocation.getArgument(0);
          if (saved.getId() == null) {
            saved.setId(77L);
          }
          return saved;
        });

    DroneDeliveryOrderResponse response =
        orderService.createDroneDelivery(
            new CreateDroneDeliveryOrderRequest(5L, 9001L, "Tai lieu", 1200, "CASH"), 44L, "idem-2");

    assertEquals(77L, response.orderId());
    assertEquals(9001L, response.reservedBoxId());
    assertEquals("DRONE_DELIVERY", response.type());
    assertEquals("PENDING_PAYMENT", response.deliveryStage());
    verify(lockerClient).reserveBox(9001L, "DRONE");
  }

  @Test
  void cancelDroneDeliveryReleasesReservedBox() {
    LockerOrder order = droneOrder(21L, 44L, 5L, 9001L, "idem-3");
    when(orderRepository.findById(21L)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(LockerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

    DroneDeliveryOrderResponse response = orderService.getDroneDelivery(21L, 44L);
    assertSame(order.getId(), response.orderId());

    orderService.cancel(21L, null, 44L);

    verify(lockerClient).releaseBox(9001L);
  }

  private LockerOrder droneOrder(Long id, Long userId, Long lockerId, Long boxId, String idempotencyKey) {
    LockerOrder order = new LockerOrder();
    order.setId(id);
    order.setOrderCode("ORD-DRONE-" + id);
    order.setUserId(userId);
    order.setReceiverId(userId);
    order.setLockerId(lockerId);
    order.setDestinationLockerId(lockerId);
    order.setSendBoxId(boxId);
    order.setReservedBoxId(boxId);
    order.setType("DRONE_DELIVERY");
    order.setServiceCategory("DRONE_DELIVERY");
    order.setStatus("INITIALIZED");
    order.setPaymentStatus("UNPAID");
    order.setDeliveryStage("PENDING_PAYMENT");
    order.setParcelWeightGrams(1200);
    order.setIdempotencyKey(idempotencyKey);
    order.setTotalPrice(BigDecimal.valueOf(15000));
    order.setOriginalPrice(BigDecimal.valueOf(15000));
    return order;
  }
}
