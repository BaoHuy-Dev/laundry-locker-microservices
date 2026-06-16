package com.huynqb.laundrylocker.order.service;

import com.huynqb.laundrylocker.common.dto.NotificationRequest;
import com.huynqb.laundrylocker.common.dto.OrderSummary;
import com.huynqb.laundrylocker.common.event.DomainEvent;
import com.huynqb.laundrylocker.common.event.DomainEventNames;
import com.huynqb.laundrylocker.common.exception.BusinessException;
import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.order.client.LockerCellClient;
import com.huynqb.laundrylocker.order.client.LockerClient;
import com.huynqb.laundrylocker.order.client.NotificationClient;
import com.huynqb.laundrylocker.order.client.UserClient;
import com.huynqb.laundrylocker.order.dto.CellDto;
import com.huynqb.laundrylocker.order.dto.CreateOrderRequest;
import com.huynqb.laundrylocker.order.dto.DelegateOrderRequest;
import com.huynqb.laundrylocker.order.dto.RentalOrderRequest;
import com.huynqb.laundrylocker.order.dto.SendOrderRequest;
import com.huynqb.laundrylocker.order.dto.OrderComplaintRequest;
import com.huynqb.laundrylocker.order.dto.OrderComplaintResponse;
import com.huynqb.laundrylocker.order.dto.OrderDetailResponse;
import com.huynqb.laundrylocker.order.dto.OrderItemRequest;
import com.huynqb.laundrylocker.order.dto.OrderRatingRequest;
import com.huynqb.laundrylocker.order.dto.OrderRatingResponse;
import com.huynqb.laundrylocker.order.dto.OrderResponse;
import com.huynqb.laundrylocker.order.dto.OrderStatusResponse;
import com.huynqb.laundrylocker.order.dto.OrderTimelineEvent;
import com.huynqb.laundrylocker.order.dto.PromotionRequest;
import com.huynqb.laundrylocker.order.dto.UpdateOrderStatusRequest;
import com.huynqb.laundrylocker.order.dto.UpdateOrderWeightRequest;
import com.huynqb.laundrylocker.order.model.LockerOrder;
import com.huynqb.laundrylocker.order.model.OrderComplaint;
import com.huynqb.laundrylocker.order.model.OrderDetail;
import com.huynqb.laundrylocker.order.model.OrderRating;
import com.huynqb.laundrylocker.order.model.OrderStatusHistory;
import com.huynqb.laundrylocker.order.model.Promotion;
import com.huynqb.laundrylocker.order.repository.LockerOrderRepository;
import com.huynqb.laundrylocker.order.repository.OrderComplaintRepository;
import com.huynqb.laundrylocker.order.repository.OrderDetailRepository;
import com.huynqb.laundrylocker.order.repository.OrderRatingRepository;
import com.huynqb.laundrylocker.order.repository.OrderStatusHistoryRepository;
import com.huynqb.laundrylocker.order.repository.PromotionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Set<String> CANCELABLE = Set.of("INITIALIZED", "RESERVED", "WAITING");

  private final LockerOrderRepository orderRepository;
  private final OrderDetailRepository detailRepository;
  private final OrderStatusHistoryRepository historyRepository;
  private final OrderRatingRepository ratingRepository;
  private final OrderComplaintRepository complaintRepository;
  private final PromotionRepository promotionRepository;
  private final RabbitTemplate rabbitTemplate;
  private final UserClient userClient;
  private final LockerClient lockerClient;
  private final LockerCellClient lockerCellClient;
  private final NotificationClient notificationClient;
  private final QrTokenService qrTokenService;

  @Value("${app.order.pickup-hours-limit:24}")
  private int pickupHoursLimit;

  @Value("${app.order.pickup-overtime-fee-per-hour:500}")
  private int overtimeFeePerHour;

  @Value("${app.order.pickup-max-overtime-fee:50000}")
  private int maxOvertimeFee;

  @Value("${app.order.pickup-max-overtime-percent:50}")
  private int maxOvertimePercent;

  @Value("${app.order.send-pickup-hours-limit:48}")
  private int sendPickupHoursLimit;

  @Value("${app.order.send-base-fee:15000}")
  private long sendBaseFee;

  @Value("${app.order.rental-rate-standard:5000}")
  private long rentalRateStandard;

  @Value("${app.order.rental-rate-xl:10000}")
  private long rentalRateXl;

  @Value("${app.order.reminder-cooldown-minutes:60}")
  private int reminderCooldownMinutes;

  @Value("${app.order.auto-cancel-hours:24}")
  private int autoCancelHours;

  @Transactional
  public OrderResponse create(CreateOrderRequest request) {
    userClient.getUser(request.userId());
    LockerOrder order = new LockerOrder();
    order.setOrderCode(generateOrderCode());
    order.setUserId(request.userId());
    order.setReceiverId(request.receiverId());
    order.setReceiverPhone(request.receiverPhone());
    order.setReceiverName(request.receiverName());
    order.setLockerId(request.lockerId());
    order.setStoreId(request.storeId());
    order.setSendBoxId(resolveAndReserveSendBox(request));
    order.setType(StringUtils.hasText(request.type()) ? request.type().toUpperCase() : "STORAGE");
    order.setServiceCategory(
        StringUtils.hasText(request.serviceCategory()) ? request.serviceCategory().toUpperCase() : "STORAGE");
    order.setStatus("INITIALIZED");
    order.setPinCode(generatePinCode());
    order.setPinCodeIssuedAt(LocalDateTime.now());
    order.setCustomerNote(request.customerNote());
    order.setDeliveryAddress(request.deliveryAddress());
    order.setIntendedReceiveAt(request.intendedReceiveAt());
    if (request.estimatedWeight() != null) {
      order.setActualWeight(request.estimatedWeight());
    }

    LockerOrder saved = orderRepository.save(order);
    BigDecimal calculatedTotal = saveDetailsAndCalculate(saved.getId(), request);
    saved.setTotalPrice(request.totalPrice() == null ? calculatedTotal : request.totalPrice());
    saved.setOriginalPrice(saved.getTotalPrice());
    applyPromotion(saved, request.promotionCode(), request.promotionCodes());
    saved = orderRepository.save(saved);
    addHistory(saved.getId(), null, saved.getStatus(), saved.getUserId(), "Order created");
    publish(DomainEventNames.ORDER_CREATED, saved, Map.of("orderId", saved.getId(), "userId", saved.getUserId()));
    return toResponse(saved);
  }

  @Transactional
  public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
    LockerOrder order = find(id);
    String target = request.status() == null ? "" : request.status().toUpperCase();
    if ("CANCELED".equals(target) || "COMPLETED".equals(target)) {
      releaseBoxes(order);
    }
    if ("RETURNED".equals(target)) {
      occupyBoxQuietly(request.receiveBoxId());
    }
    return transition(order, request.status(), request.staffId(), request.receiveBoxId(), null);
  }

  @Transactional
  public OrderResponse confirm(Long id, Long userId) {
    LockerOrder order = find(id);
    assertOwner(order, userId);
    validateStatus(order, Set.of("INITIALIZED"));
    occupyBoxQuietly(order.getSendBoxId());
    if ("SEND".equalsIgnoreCase(order.getType())) {
      // Stage 2 of the SEND flow: the drop PIN dies here, a fresh pickup PIN
      // goes to the receiver — the sender can no longer open the cell.
      order.setPinCode(generatePinCode());
      order.setPinCodeIssuedAt(LocalDateTime.now());
      order.setPickupDeadline(LocalDateTime.now().plusHours(sendPickupHoursLimit));
      notifyParcelReadyForReceiver(order);
      return transition(order, "STORING", userId, null,
          "Sender dropped parcel; pickup PIN issued to receiver " + order.getReceiverPhone());
    }
    if ("RENTAL".equalsIgnoreCase(order.getType())) {
      return transition(order, "STORING", userId, null, "Renter placed items; multi-use PIN active until deadline");
    }
    return transition(order, "STORING", userId, null, "Customer confirmed items dropped in locker");
  }

  @Transactional
  public OrderResponse createSend(SendOrderRequest request, Long userId) {
    Long boxId = request.boxId();
    if (boxId == null) {
      boxId = findAvailableCell(request.lockerId(), request.size(), "STANDARD");
    }
    BigDecimal price = request.totalPrice() == null ? BigDecimal.valueOf(sendBaseFee) : request.totalPrice();
    return create(
        new CreateOrderRequest(
            userId, request.lockerId(), boxId, null, null,
            "SEND", "PARCEL",
            null, request.receiverPhone(), request.receiverName(),
            null, null, request.note(), null, null, null, request.promotionCode(), null, price));
  }

  @Transactional
  public OrderResponse createRental(RentalOrderRequest request, Long userId) {
    String cellType = StringUtils.hasText(request.cellType()) ? request.cellType().toUpperCase() : "STANDARD";
    if ("DRONE".equals(cellType)) {
      throw new BusinessException("DRONE_CELL_RESTRICTED", "Drone cells cannot be rented");
    }
    Long boxId = request.boxId();
    if (boxId == null) {
      boxId = findAvailableCell(request.lockerId(), null, cellType);
    }
    BigDecimal price = rentalRate(cellType).multiply(BigDecimal.valueOf(request.hours()));
    OrderResponse created =
        create(
            new CreateOrderRequest(
                userId, request.lockerId(), boxId, null, null,
                "RENTAL", "RENTAL",
                null, null, null, null, null, request.note(), null, null, null, request.promotionCode(), null, price));
    LockerOrder order = find(created.id());
    order.setPickupDeadline(LocalDateTime.now().plusHours(request.hours()));
    return toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse extendRental(Long id, Long userId, int hours) {
    LockerOrder order = find(id);
    assertOwner(order, userId);
    if (!"RENTAL".equalsIgnoreCase(order.getType())) {
      throw new BusinessException("ORDER_STATUS_INVALID", "Only rental orders can be extended");
    }
    validateStatus(order, Set.of("INITIALIZED", "STORING"));
    LocalDateTime base =
        order.getPickupDeadline() == null || order.getPickupDeadline().isBefore(LocalDateTime.now())
            ? LocalDateTime.now()
            : order.getPickupDeadline();
    order.setPickupDeadline(base.plusHours(hours));
    BigDecimal extra = rentalRate(cellTypeOfRental(order)).multiply(BigDecimal.valueOf(hours));
    order.setTotalPrice(order.getTotalPrice().add(extra));
    order.setOriginalPrice(order.getOriginalPrice().add(extra));
    LockerOrder saved = orderRepository.save(order);
    addHistory(saved.getId(), saved.getStatus(), saved.getStatus(), userId,
        "Rental extended by " + hours + "h until " + saved.getPickupDeadline());
    notifyQuietly(saved.getUserId(), "Rental extended",
        "Rental " + saved.getOrderCode() + " extended until " + saved.getPickupDeadline(),
        "ORDER_RENTAL_EXTENDED", saved.getId());
    return toResponse(saved);
  }

  private String cellTypeOfRental(LockerOrder order) {
    if (order.getSendBoxId() == null) {
      return "STANDARD";
    }
    try {
      CellDto cell = lockerCellClient.getCell(order.getSendBoxId()).data();
      return cell == null || cell.cellType() == null ? "STANDARD" : cell.cellType();
    } catch (Exception ex) {
      return "STANDARD";
    }
  }

  private BigDecimal rentalRate(String cellType) {
    return BigDecimal.valueOf("XL".equalsIgnoreCase(cellType) ? rentalRateXl : rentalRateStandard);
  }

  private Long findAvailableCell(Long lockerId, String size, String cellType) {
    try {
      CellDto cell = lockerCellClient.findAvailable(lockerId, size, cellType).data();
      if (cell == null || cell.id() == null) {
        throw new BusinessException("BOX_NOT_AVAILABLE", "No available cell of requested type");
      }
      return cell.id();
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException("BOX_NOT_AVAILABLE", "No available cell of requested type");
    }
  }

  private void notifyParcelReadyForReceiver(LockerOrder order) {
    String message =
        "Parcel " + order.getOrderCode() + " is waiting in locker " + order.getLockerId()
            + ". Pickup PIN: " + order.getPinCode()
            + ". Deadline: " + order.getPickupDeadline();
    try {
      var receiver = userClient.getUserByPhone(order.getReceiverPhone()).data();
      if (receiver != null && receiver.id() != null) {
        order.setReceiverId(receiver.id());
        notificationClient.requestNotification(
            new NotificationRequest(receiver.id(), "Parcel waiting for you", message, "ORDER_PARCEL_READY", order.getId(), "ORDER"));
      }
    } catch (Exception ex) {
      // Receiver has no account (or user-service is down): the sender keeps the
      // PIN in their order detail and shares it out-of-band (SMS gateway is a
      // production integration point).
      log.info("Receiver {} not notified in-app for order {}: {}", order.getReceiverPhone(), order.getId(), ex.getMessage());
    }
    notifyQuietly(order.getUserId(), "Parcel stored",
        "Parcel " + order.getOrderCode() + " stored. Receiver " + order.getReceiverPhone()
            + " can pick up with PIN " + order.getPinCode() + " before " + order.getPickupDeadline(),
        "ORDER_PARCEL_STORED", order.getId());
  }

  private void notifyQuietly(Long userId, String title, String message, String type, Long orderId) {
    try {
      notificationClient.requestNotification(new NotificationRequest(userId, title, message, type, orderId, "ORDER"));
    } catch (Exception ex) {
      log.warn("Could not notify user {} for order {}: {}", userId, orderId, ex.getMessage());
    }
  }

  @Transactional
  public OrderResponse collect(Long id, Long staffId) {
    LockerOrder order = find(id);
    validateStatus(order, Set.of("STORING", "INITIALIZED"));
    if (order.getSendBoxId() != null) {
      lockerClient.releaseBox(order.getSendBoxId());
    }
    return transition(order, "COLLECTED", staffId, null, "Staff collected items from locker");
  }

  @Transactional
  public OrderResponse updateWeight(Long id, UpdateOrderWeightRequest request, Long staffId) {
    LockerOrder order = find(id);
    validateStatus(order, Set.of("COLLECTED", "PROCESSING"));
    order.setActualWeight(request.actualWeight());
    if (StringUtils.hasText(request.weightUnit())) {
      order.setWeightUnit(request.weightUnit().toUpperCase());
    }
    if (request.items() != null && !request.items().isEmpty()) {
      detailRepository.deleteByOrderId(order.getId());
      BigDecimal newTotal = saveItemDetails(order.getId(), request.items(), request.actualWeight());
      order.setOriginalPrice(newTotal);
      BigDecimal discount = order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount();
      BigDecimal extraFee = order.getExtraFee() == null ? BigDecimal.ZERO : order.getExtraFee();
      order.setTotalPrice(newTotal.subtract(discount).max(BigDecimal.ZERO).add(extraFee));
    }
    order.setStaffId(staffId);
    LockerOrder saved = orderRepository.save(order);
    addHistory(saved.getId(), saved.getStatus(), saved.getStatus(), staffId,
        StringUtils.hasText(request.staffNote()) ? request.staffNote() : "Staff updated order weight");
    return toResponse(saved);
  }

  @Transactional
  public OrderResponse process(Long id, Long staffId) {
    LockerOrder order = find(id);
    validateStatus(order, Set.of("COLLECTED"));
    return transition(order, "PROCESSING", staffId, null, "Staff started processing order");
  }

  @Transactional
  public OrderResponse ready(Long id, Long staffId) {
    LockerOrder order = find(id);
    validateStatus(order, Set.of("PROCESSING", "COLLECTED"));
    return transition(order, "READY", staffId, null, "Order is ready for return");
  }

  @Transactional
  public OrderResponse returnOrder(Long id, Long boxId, Long staffId) {
    LockerOrder order = find(id);
    validateStatus(order, Set.of("READY", "PROCESSING"));
    if (boxId != null) {
      lockerClient.reserveBox(boxId);
      occupyBoxQuietly(boxId);
    }
    order.setPinCode(generatePinCode());
    order.setPinCodeIssuedAt(LocalDateTime.now());
    order.setReturnedAt(LocalDateTime.now());
    order.setPickupDeadline(LocalDateTime.now().plusHours(pickupHoursLimit));
    return transition(order, "RETURNED", staffId, boxId, "Staff returned items to locker");
  }

  @Transactional
  public OrderResponse checkout(Long id, Long staffId, String note) {
    LockerOrder order = find(id);
    validateStatus(order, Set.of("READY", "RETURNED", "STORING"));
    releaseBoxes(order);
    order.setCompletedAt(LocalDateTime.now());
    order.setPinCode(null);
    return transition(order, "COMPLETED", staffId, order.getReceiveBoxId(),
        StringUtils.hasText(note) ? note : "Order checked out by staff");
  }

  @Transactional
  public OrderResponse complete(Long id, Long userId) {
    LockerOrder order = find(id);
    assertOwnerOrReceiver(order, userId);
    validateStatus(order, Set.of("STORING", "RETURNED"));
    BigDecimal overtime = calculatePickupOvertimeFee(order);
    if (overtime.compareTo(BigDecimal.ZERO) > 0) {
      order.setExtraFee(order.getExtraFee().add(overtime));
      order.setTotalPrice(order.getTotalPrice().add(overtime));
    }
    releaseBoxes(order);
    order.setCompletedAt(LocalDateTime.now());
    order.setPinCode(null);
    return transition(order, "COMPLETED", userId, order.getReceiveBoxId(), "Customer completed pickup");
  }

  @Transactional
  public OrderResponse cancel(Long id, Integer reason, Long userId) {
    LockerOrder order = find(id);
    if (!CANCELABLE.contains(order.getStatus())) {
      throw new BusinessException("ORDER_STATUS_INVALID", "Order cannot be canceled at this status");
    }
    order.setCancelReason(reason);
    releaseBoxes(order);
    return transition(order, "CANCELED", userId, null, "Order canceled");
  }

  @Transactional
  public OrderResponse resetPin(Long id, Long userId) {
    LockerOrder order = find(id);
    assertOwner(order, userId);
    order.setPinCode(generatePinCode());
    order.setPinCodeIssuedAt(LocalDateTime.now());
    return toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse delegate(Long id, Long userId, DelegateOrderRequest request) {
    LockerOrder order = find(id);
    assertOwner(order, userId);
    // Chỉ ủy quyền khi đồ đang nằm trong tủ chờ lấy
    validateStatus(order, Set.of("STORING", "RETURNED"));
    order.setPinCode(generatePinCode());
    order.setPinCodeIssuedAt(LocalDateTime.now());
    order.setReceiverPhone(request.phone());
    if (StringUtils.hasText(request.name())) {
      order.setReceiverName(request.name());
    }
    LockerOrder saved = orderRepository.save(order);
    addHistory(
        saved.getId(),
        saved.getStatus(),
        saved.getStatus(),
        userId,
        "Delegated pickup to " + request.phone()
            + (StringUtils.hasText(request.note()) ? " - " + request.note() : ""));
    try {
      notificationClient.requestNotification(
          new NotificationRequest(
              saved.getUserId(),
              "Order delegated",
              "Order " + saved.getOrderCode() + " pickup delegated to " + request.phone()
                  + ". New PIN issued.",
              "ORDER_DELEGATED",
              saved.getId(),
              "ORDER"));
    } catch (Exception ex) {
      log.warn("Could not notify delegation for order {}: {}", saved.getId(), ex.getMessage());
    }
    return toResponse(saved);
  }



  @Transactional
  public OrderResponse pickupStorage(Long id, Long userId) {
    LockerOrder order = find(id);
    assertOwner(order, userId);
    boolean storageLike =
        "STORAGE".equalsIgnoreCase(order.getType())
            || "STORAGE".equalsIgnoreCase(order.getServiceCategory())
            || "RENTAL".equalsIgnoreCase(order.getType())
            || "RENTAL".equalsIgnoreCase(order.getServiceCategory());
    if (!storageLike) {
      throw new BusinessException("ORDER_STATUS_INVALID", "Only storage/rental orders can use pickup-storage");
    }
    validateStatus(order, Set.of("STORING", "INITIALIZED", "RETURNED"));
    BigDecimal overtime = calculatePickupOvertimeFee(order);
    if (overtime.compareTo(BigDecimal.ZERO) > 0) {
      order.setExtraFee(order.getExtraFee().add(overtime));
      order.setTotalPrice(order.getTotalPrice().add(overtime));
    }
    releaseBoxes(order);
    order.setCompletedAt(LocalDateTime.now());
    order.setPinCode(null);
    return transition(order, "COMPLETED", userId, order.getReceiveBoxId(),
        "RENTAL".equalsIgnoreCase(order.getType()) ? "Rental ended, cell released" : "Storage order picked up");
  }

  @Transactional
  public OrderResponse reorder(Long originalOrderId, Long userId) {
    LockerOrder original = find(originalOrderId);
    assertOwner(original, userId);
    if (!Set.of("COMPLETED", "CANCELED").contains(original.getStatus())) {
      throw new BusinessException("ORDER_STATUS_INVALID", "Only completed or canceled orders can be reordered");
    }
    String type = original.getType() == null ? "" : original.getType().toUpperCase();
    // SEND/RENTAL carry box reservation + pricing rules that the generic create()
    // path below cannot reproduce (no cellType/hours field on CreateOrderRequest),
    // so they reorder through the same entry points a fresh booking would use.
    if ("SEND".equals(type)) {
      return createSend(
          new SendOrderRequest(
              original.getLockerId(),
              null,
              null,
              original.getReceiverPhone(),
              original.getReceiverName(),
              original.getCustomerNote(),
              null,
              null),
          userId);
    }
    if ("RENTAL".equals(type)) {
      long hours = 1;
      if (original.getPickupDeadline() != null && original.getCreatedAt() != null) {
        hours = Math.min(720, Math.max(1, ChronoUnit.HOURS.between(original.getCreatedAt(), original.getPickupDeadline())));
      }
      return createRental(
          new RentalOrderRequest(
              original.getLockerId(),
              null,
              cellTypeOfRental(original),
              (int) hours,
              original.getCustomerNote(),
              null),
          userId);
    }
    List<OrderItemRequest> items =
        detailRepository.findByOrderId(originalOrderId).stream()
            .map(d -> new OrderItemRequest(d.getServiceId(), d.getQuantity(), d.getDescription()))
            .toList();
    return create(
        new CreateOrderRequest(
            original.getUserId(),
            original.getLockerId(),
            null,
            null,
            original.getStoreId(),
            original.getType(),
            original.getServiceCategory(),
            original.getReceiverId(),
            original.getReceiverPhone(),
            original.getReceiverName(),
            original.getIntendedReceiveAt(),
            original.getActualWeight(),
            original.getCustomerNote(),
            original.getDeliveryAddress(),
            null,
            items,
            null,
            null,
            null));
  }

  @Transactional(readOnly = true)
  public OrderResponse get(Long id) {
    return toResponse(find(id));
  }

  @Transactional(readOnly = true)
  public OrderSummary getSummary(Long id) {
    return toSummary(find(id));
  }

  @Transactional(readOnly = true)
  public OrderStatusResponse status(Long id) {
    LockerOrder order = find(id);
    return new OrderStatusResponse(
        order.getId(),
        order.getStatus(),
        statusDescription(order.getStatus()),
        order.getPinCode(),
        order.getLockerId(),
        order.getReceiveBoxId() == null ? order.getSendBoxId() : order.getReceiveBoxId(),
        order.getIntendedReceiveAt(),
        "COMPLETED".equals(order.getStatus()),
        nextAction(order));
  }

  @Transactional(readOnly = true)
  public OrderResponse getByCode(String orderCode) {
    return toResponse(
        orderRepository
            .findByOrderCode(orderCode)
            .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found: " + orderCode)));
  }

  @Transactional(readOnly = true)
  public OrderResponse getByPin(String pinCode) {
    return toResponse(
        orderRepository
            .findByPinCode(pinCode)
            .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found for PIN")));
  }

  // Single entry point for the cabinet screen: a 6-digit PIN or a signed QR
  // token are interchangeable access credentials.
  @Transactional(readOnly = true)
  public OrderResponse getByAccess(String code) {
    if (!StringUtils.hasText(code)) {
      throw new BusinessException("INVALID_ACCESS_CODE", "Access code is required");
    }
    String trimmed = code.trim();
    if (trimmed.startsWith(QrTokenService.PREFIX)) {
      Long orderId = qrTokenService.parseOrderId(trimmed);
      if (orderId == null) {
        throw new BusinessException("INVALID_ACCESS_CODE", "Malformed QR token");
      }
      LockerOrder order = find(orderId);
      if (!qrTokenService.matches(trimmed, order.getId(), order.getPinCode())) {
        throw new BusinessException("INVALID_ACCESS_CODE", "QR token expired or revoked");
      }
      return toResponse(order);
    }
    return getByPin(trimmed);
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> manageList(String status, String type, Long lockerId) {
    return orderRepository.findAll().stream()
        .filter(o -> status == null || o.getStatus().equalsIgnoreCase(status))
        .filter(o -> type == null || o.getType().equalsIgnoreCase(type))
        .filter(o -> lockerId == null || lockerId.equals(o.getLockerId()))
        .sorted(java.util.Comparator.comparing(LockerOrder::getCreatedAt).reversed())
        .limit(500)
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public int sendPickupReminders() {
    LocalDateTime now = LocalDateTime.now();
    int sent = 0;
    List<LockerOrder> candidates = new java.util.ArrayList<>();
    candidates.addAll(orderRepository.findByStatusOrderByCreatedAtDesc("RETURNED"));
    candidates.addAll(orderRepository.findByStatusOrderByCreatedAtDesc("STORING"));
    for (LockerOrder order : candidates) {
      if (order.getPickupDeadline() == null || now.isBefore(order.getPickupDeadline())) {
        continue;
      }
      if (order.getLastReminderAt() != null
          && order.getLastReminderAt().isAfter(now.minusMinutes(reminderCooldownMinutes))) {
        continue;
      }
      boolean rental = "RENTAL".equalsIgnoreCase(order.getType());
      notifyQuietly(order.getUserId(),
          rental ? "Rental expired" : "Pickup overdue",
          (rental
                  ? "Rental " + order.getOrderCode() + " expired at " + order.getPickupDeadline()
                  : "Order " + order.getOrderCode() + " passed its pickup deadline " + order.getPickupDeadline())
              + ". Overtime fee applies.",
          "ORDER_PICKUP_OVERDUE", order.getId());
      if (order.getReceiverId() != null) {
        notifyQuietly(order.getReceiverId(), "Pickup overdue",
            "Parcel " + order.getOrderCode() + " is still waiting. Overtime fee applies.",
            "ORDER_PICKUP_OVERDUE", order.getId());
      }
      order.setLastReminderAt(now);
      orderRepository.save(order);
      sent++;
    }
    if (sent > 0) {
      log.info("Pickup reminders sent: {}", sent);
    }
    return sent;
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> listByUser(Long userId) {
    return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> list(String status, Long staffId) {
    if (staffId != null) {
      return orderRepository.findByStaffIdOrderByCreatedAtDesc(staffId).stream().map(this::toResponse).toList();
    }
    if (StringUtils.hasText(status)) {
      return orderRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase()).stream().map(this::toResponse).toList();
    }
    return orderRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> statistics() {
    Map<String, Object> result = new HashMap<>();
    List<LockerOrder> orders = orderRepository.findAll();
    var startOfToday = LocalDate.now().atStartOfDay();

    result.put("totalOrders", orders.size());
    result.put(
        "byStatus",
        orders.stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    LockerOrder::getStatus, java.util.stream.Collectors.counting())));

    // Order-owned dashboard metrics (computed from this service's data only).
    long ordersToday =
        orders.stream()
            .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(startOfToday))
            .count();
    result.put("ordersToday", ordersToday);

    long pendingOrders =
        orders.stream()
            .filter(
                o -> {
                  String s = o.getStatus() == null ? "" : o.getStatus().toUpperCase();
                  return !s.equals("COMPLETED") && !s.equals("CANCELED") && !s.equals("CANCELLED");
                })
            .count();
    result.put("pendingOrders", pendingOrders);

    BigDecimal totalRevenue =
        orders.stream()
            .filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()))
            .map(o -> o.getTotalPrice() == null ? BigDecimal.ZERO : o.getTotalPrice())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    result.put("totalRevenue", totalRevenue);

    BigDecimal revenueToday =
        orders.stream()
            .filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()))
            .filter(
                o -> {
                  var when = o.getCompletedAt() != null ? o.getCompletedAt() : o.getCreatedAt();
                  return when != null && !when.isBefore(startOfToday);
                })
            .map(o -> o.getTotalPrice() == null ? BigDecimal.ZERO : o.getTotalPrice())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    result.put("revenueToday", revenueToday);

    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> revenue() {
    BigDecimal total =
        orderRepository.findAll().stream()
            .filter(order -> "COMPLETED".equalsIgnoreCase(order.getStatus()))
            .map(LockerOrder::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return Map.of("totalRevenue", total);
  }

  @Transactional
  public OrderRatingResponse rate(Long orderId, OrderRatingRequest request, Long userId) {
    LockerOrder order = find(orderId);
    assertOwner(order, userId);
    validateStatus(order, Set.of("COMPLETED"));
    OrderRating rating = ratingRepository.findByOrderId(orderId).orElseGet(OrderRating::new);
    rating.setOrderId(orderId);
    rating.setUserId(userId);
    rating.setRating(request.rating());
    rating.setComment(request.comment());
    return toRating(ratingRepository.save(rating));
  }

  @Transactional(readOnly = true)
  public List<OrderRatingResponse> myRatings(Long userId) {
    return ratingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toRating).toList();
  }

  @Transactional(readOnly = true)
  public OrderRatingResponse rating(Long orderId) {
    return ratingRepository.findByOrderId(orderId).map(this::toRating).orElseThrow(() -> new NotFoundException("OrderRating", orderId));
  }

  @Transactional
  public OrderComplaintResponse complain(Long orderId, OrderComplaintRequest request, Long userId) {
    LockerOrder order = find(orderId);
    assertOwner(order, userId);
    OrderComplaint complaint = new OrderComplaint();
    complaint.setOrderId(orderId);
    complaint.setUserId(userId);
    complaint.setType(StringUtils.hasText(request.type()) ? request.type().toUpperCase() : "OTHER");
    complaint.setDescription(request.description());
    return toComplaint(complaintRepository.save(complaint));
  }

  @Transactional(readOnly = true)
  public List<OrderComplaintResponse> complaints(Long orderId) {
    return complaintRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream().map(this::toComplaint).toList();
  }

  @Transactional(readOnly = true)
  public List<OrderComplaintResponse> myComplaints(Long userId) {
    return complaintRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toComplaint).toList();
  }

  @Transactional(readOnly = true)
  public List<OrderTimelineEvent> timeline(Long orderId) {
    return historyRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
        .map(h -> new OrderTimelineEvent(h.getOldStatus(), h.getNewStatus(), h.getChangedByUserId(), h.getNote(), h.getCreatedAt()))
        .toList();
  }

  @Transactional
  public Promotion createPromotion(PromotionRequest request, Long createdByUserId) {
    Promotion promotion = new Promotion();
    promotion.setCode(request.code().toUpperCase());
    promotion.setName(request.name());
    promotion.setDiscountType(StringUtils.hasText(request.discountType()) ? request.discountType().toUpperCase() : "FIXED_AMOUNT");
    promotion.setDiscountValue(request.discountValue() == null ? BigDecimal.ZERO : request.discountValue());
    promotion.setMaxDiscountAmount(request.maxDiscountAmount());
    promotion.setMinOrderAmount(request.minOrderAmount());
    promotion.setStackable(Boolean.TRUE.equals(request.stackable()));
    promotion.setStatus(StringUtils.hasText(request.status()) ? request.status().toUpperCase() : "ACTIVE");
    promotion.setStartAt(request.startAt());
    promotion.setEndAt(request.endAt());
    promotion.setCreatedByUserId(createdByUserId);
    return promotionRepository.save(promotion);
  }

  @Transactional(readOnly = true)
  public List<Promotion> promotions() {
    return promotionRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Promotion promotion(Long id) {
    return promotionRepository.findById(id).orElseThrow(() -> new NotFoundException("Promotion", id));
  }

  @Transactional
  public Promotion updatePromotion(Long id, PromotionRequest request) {
    Promotion promotion = promotion(id);
    promotion.setCode(request.code().toUpperCase());
    promotion.setName(request.name());
    promotion.setDiscountType(StringUtils.hasText(request.discountType()) ? request.discountType().toUpperCase() : promotion.getDiscountType());
    promotion.setDiscountValue(request.discountValue() == null ? BigDecimal.ZERO : request.discountValue());
    promotion.setMaxDiscountAmount(request.maxDiscountAmount());
    promotion.setMinOrderAmount(request.minOrderAmount());
    promotion.setStackable(Boolean.TRUE.equals(request.stackable()));
    promotion.setStatus(StringUtils.hasText(request.status()) ? request.status().toUpperCase() : promotion.getStatus());
    promotion.setStartAt(request.startAt());
    promotion.setEndAt(request.endAt());
    return promotionRepository.save(promotion);
  }

  @Transactional
  public void deletePromotion(Long id) {
    promotionRepository.delete(promotion(id));
  }

  @Transactional(readOnly = true)
  public List<Promotion> promotionsByStatus(String status) {
    return promotionRepository.findByStatus(status.toUpperCase());
  }

  @Transactional(readOnly = true)
  public List<Promotion> searchPromotions(String keyword) {
    String lower = keyword == null ? "" : keyword.toLowerCase();
    return promotionRepository.findAll().stream()
        .filter(p -> p.getCode().toLowerCase().contains(lower) || p.getName().toLowerCase().contains(lower))
        .toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> validatePromotion(String code) {
    Promotion promotion = promotionRepository.findByCodeIgnoreCase(code).orElse(null);
    boolean valid = promotion != null && promotion.activeNow();
    Map<String, Object> result = new HashMap<>();
    result.put("code", code);
    result.put("valid", valid);
    result.put("promotion", promotion);
    return result;
  }

  @Transactional(readOnly = true)
  public List<Promotion> activePromotions() {
    return promotionRepository.findByStatus("ACTIVE").stream().filter(Promotion::activeNow).toList();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> storeRatings(Long storeId) {
    return ratingRepository.findAll().stream()
        .filter(rating -> find(rating.getOrderId()).getStoreId().equals(storeId))
        .map(
            rating -> {
              Map<String, Object> result = new HashMap<>();
              result.put("id", rating.getId());
              result.put("orderId", rating.getOrderId());
              result.put("userId", rating.getUserId());
              result.put("rating", rating.getRating());
              result.put("comment", rating.getComment());
              result.put("createdAt", rating.getCreatedAt());
              return result;
            })
        .toList();
  }

  // Reservation TTL: an unconfirmed (INITIALIZED) order keeps its send cell in
  // RESERVED. If the customer never drops their items within the hold window,
  // sweep the order to CANCELED *and release the cell* — otherwise the cell
  // stays stuck RESERVED forever (the old version only flipped the status).
  @Transactional
  public Map<String, Object> autoCancelUnconfirmedOrders() {
    LocalDateTime cutoff = LocalDateTime.now().minusHours(autoCancelHours);
    int canceled = 0;
    for (LockerOrder order : orderRepository.findByStatusOrderByCreatedAtDesc("INITIALIZED")) {
      if (order.getCreatedAt() == null || order.getCreatedAt().isAfter(cutoff)) {
        continue;
      }
      releaseBoxes(order);
      transition(
          order,
          "CANCELED",
          null,
          null,
          "Tự hủy: không xác nhận bỏ đồ trong " + autoCancelHours + " giờ; đã nhả ô");
      canceled++;
    }
    if (canceled > 0) {
      log.info("Auto-canceled unconfirmed orders and released their cells: {}", canceled);
    }
    return Map.of("canceledOrders", canceled);
  }

  @Transactional
  public Map<String, Object> releaseBoxesAfterCompletion() {
    orderRepository.findByStatusOrderByCreatedAtDesc("COMPLETED").forEach(this::releaseBoxes);
    return Map.of("status", "completed");
  }

  @Transactional(readOnly = true)
  public Map<String, Object> pickupReminders() {
    long count =
        orderRepository.findByStatusOrderByCreatedAtDesc("RETURNED").stream()
            .filter(order -> order.getPickupDeadline() != null)
            .count();
    return Map.of("reminders", count);
  }

  private OrderResponse transition(
      LockerOrder order, String newStatus, Long actorId, Long receiveBoxId, String note) {
    String oldStatus = order.getStatus();
    order.setStatus(newStatus.toUpperCase());
    if (actorId != null && !"COMPLETED".equals(order.getStatus()) && !"CANCELED".equals(order.getStatus())) {
      order.setStaffId(actorId);
    }
    if (receiveBoxId != null) {
      order.setReceiveBoxId(receiveBoxId);
    }
    LockerOrder saved = orderRepository.save(order);
    addHistory(saved.getId(), oldStatus, saved.getStatus(), actorId, note);
    publishStatusChanged(saved, oldStatus);
    notifyStatus(saved, oldStatus);
    return toResponse(saved);
  }

  private Long resolveAndReserveSendBox(CreateOrderRequest request) {
    Long boxId = request.sendBoxId();
    if ((boxId == null) && request.boxIds() != null && !request.boxIds().isEmpty()) {
      boxId = request.boxIds().iterator().next();
    }
    if (boxId != null) {
      lockerClient.reserveBox(boxId);
    }
    return boxId;
  }

  private BigDecimal saveDetailsAndCalculate(Long orderId, CreateOrderRequest request) {
    if (request.items() != null && !request.items().isEmpty()) {
      return saveItemDetails(orderId, request.items(), request.estimatedWeight());
    }
    return BigDecimal.ZERO;
  }

  private BigDecimal saveItemDetails(Long orderId, List<OrderItemRequest> items, BigDecimal defaultQuantity) {
    BigDecimal total = BigDecimal.ZERO;
    for (OrderItemRequest item : items) {
      BigDecimal quantity = item.quantity() == null ? (defaultQuantity == null ? BigDecimal.ONE : defaultQuantity) : item.quantity();
      BigDecimal price = BigDecimal.valueOf(5000).multiply(quantity); // Base storage fee
      saveDetail(orderId, item.serviceId(), quantity, price, item.description());
      total = total.add(price);
    }
    return total;
  }

  private void saveDetail(Long orderId, Long serviceId, BigDecimal quantity, BigDecimal price, String description) {
    OrderDetail detail = new OrderDetail();
    detail.setOrderId(orderId);
    detail.setServiceId(serviceId);
    detail.setQuantity(quantity);
    detail.setPrice(price == null ? BigDecimal.ZERO : price);
    detail.setDescription(description);
    detailRepository.save(detail);
  }

  private void applyPromotion(LockerOrder order, String promotionCode, List<String> promotionCodes) {
    List<String> codes = new ArrayList<>();
    if (StringUtils.hasText(promotionCode)) {
      codes.add(promotionCode);
    }
    if (promotionCodes != null) {
      promotionCodes.stream().filter(StringUtils::hasText).forEach(codes::add);
    }
    BigDecimal discount = BigDecimal.ZERO;
    List<String> applied = new ArrayList<>();
    for (String code : codes) {
      Promotion promotion = promotionRepository.findByCodeIgnoreCase(code).orElse(null);
      if (promotion == null || !promotion.activeNow()) {
        continue;
      }
      if (!applied.isEmpty() && !Boolean.TRUE.equals(promotion.getStackable())) {
        continue;
      }
      if (promotion.getMinOrderAmount() != null && order.getTotalPrice().compareTo(promotion.getMinOrderAmount()) < 0) {
        continue;
      }
      BigDecimal amount = discountAmount(promotion, order.getTotalPrice());
      discount = discount.add(amount);
      applied.add(promotion.getCode());
      promotion.setUsageCount(promotion.getUsageCount() + 1);
    }
    if (!applied.isEmpty()) {
      order.setPromotionCode(applied.get(0));
      order.setAppliedPromotionCodes(String.join(",", applied));
      order.setDiscount(discount);
      order.setTotalPrice(order.getTotalPrice().subtract(discount).max(BigDecimal.ZERO));
    }
  }

  private BigDecimal discountAmount(Promotion promotion, BigDecimal orderTotal) {
    BigDecimal discount =
        "PERCENTAGE".equalsIgnoreCase(promotion.getDiscountType())
            ? orderTotal.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            : promotion.getDiscountValue();
    if (promotion.getMaxDiscountAmount() != null && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
      discount = promotion.getMaxDiscountAmount();
    }
    return discount.min(orderTotal);
  }

  private void releaseBoxes(LockerOrder order) {
    if (order.getSendBoxId() != null) {
      lockerClient.releaseBox(order.getSendBoxId());
    }
    if (order.getReceiveBoxId() != null) {
      lockerClient.releaseBox(order.getReceiveBoxId());
    }
  }

  // Cell state is a best-effort mirror of the physical cabinet: an occupy
  // failure (e.g. legacy box already OCCUPIED) must not abort the order flow.
  private void occupyBoxQuietly(Long boxId) {
    if (boxId == null) {
      return;
    }
    try {
      lockerClient.occupyBox(boxId);
    } catch (Exception ex) {
      log.warn("Could not occupy box {}: {}", boxId, ex.getMessage());
    }
  }

  private BigDecimal calculatePickupOvertimeFee(LockerOrder order) {
    if (order.getPickupDeadline() == null || !LocalDateTime.now().isAfter(order.getPickupDeadline())) {
      return BigDecimal.ZERO;
    }
    long hours = ChronoUnit.HOURS.between(order.getPickupDeadline(), LocalDateTime.now());
    BigDecimal raw = BigDecimal.valueOf(Math.max(0, hours) * overtimeFeePerHour);
    BigDecimal percentageCap =
        order.getTotalPrice().multiply(BigDecimal.valueOf(maxOvertimePercent)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    return raw.min(BigDecimal.valueOf(maxOvertimeFee)).min(percentageCap);
  }

  private void validateStatus(LockerOrder order, Set<String> statuses) {
    if (!statuses.contains(order.getStatus())) {
      throw new BusinessException("ORDER_STATUS_INVALID", "Order status does not allow this action");
    }
  }

  private void assertOwner(LockerOrder order, Long userId) {
    if (userId != null && !userId.equals(order.getUserId())) {
      throw new BusinessException("ORDER_FORBIDDEN", "Order does not belong to user");
    }
  }

  private void assertOwnerOrReceiver(LockerOrder order, Long userId) {
    if (userId == null
        || userId.equals(order.getUserId())
        || userId.equals(order.getReceiverId())) {
      return;
    }
    throw new BusinessException("ORDER_FORBIDDEN", "Order does not belong to user");
  }

  private LockerOrder find(Long id) {
    return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order", id));
  }

  private OrderSummary toSummary(LockerOrder order) {
    return new OrderSummary(order.getId(), order.getUserId(), order.getStatus(), order.getTotalPrice());
  }

  private OrderResponse toResponse(LockerOrder order) {
    List<OrderDetailResponse> details =
        detailRepository.findByOrderId(order.getId()).stream()
            .map(d -> new OrderDetailResponse(d.getServiceId(), d.getQuantity(), d.getPrice(), d.getDescription()))
            .toList();
    return new OrderResponse(
        order.getId(),
        order.getOrderCode(),
        order.getUserId(),
        order.getReceiverId(),
        order.getLockerId(),
        order.getSendBoxId(),
        order.getReceiveBoxId(),
        order.getStoreId(),
        order.getStaffId(),
        order.getType(),
        order.getServiceCategory(),
        order.getStatus(),
        order.getPinCode(),
        qrTokenService.issue(order.getId(), order.getPinCode()),
        order.getActualWeight(),
        order.getWeightUnit(),
        order.getExtraFee(),
        order.getDiscount(),
        order.getTotalPrice(),
        order.getOriginalPrice(),
        order.getPromotionCode(),
        order.getAppliedPromotionCodes(),
        nextAction(order),
        nextActionMessage(order),
        paymentRequired(order),
        order.getPickupDeadline() != null && LocalDateTime.now().isAfter(order.getPickupDeadline()),
        order.getPickupDeadline(),
        order.getReturnedAt(),
        order.getCompletedAt(),
        order.getCreatedAt(),
        order.getUpdatedAt(),
        details);
  }

  private OrderRatingResponse toRating(OrderRating rating) {
    return new OrderRatingResponse(rating.getId(), rating.getOrderId(), rating.getUserId(), rating.getRating(), rating.getComment(), rating.getCreatedAt());
  }

  private OrderComplaintResponse toComplaint(OrderComplaint complaint) {
    return new OrderComplaintResponse(
        complaint.getId(), complaint.getOrderId(), complaint.getUserId(), complaint.getType(), complaint.getDescription(), complaint.getStatus(), complaint.getCreatedAt());
  }

  private String nextAction(LockerOrder order) {
    return switch (order.getStatus()) {
      case "INITIALIZED" -> "PAY_AND_DROP";
      case "STORING" -> "PICKUP";
      case "COMPLETED" -> "DONE";
      case "CANCELED" -> "CANCELED";
      default -> "UNKNOWN";
    };
  }

  private String nextActionMessage(LockerOrder order) {
    return switch (nextAction(order)) {
      case "PAY_AND_DROP" -> "Pay and place storage items in locker.";
      case "PICKUP" -> "Pick up items from locker.";
      default -> order.getStatus();
    };
  }

  private boolean paymentRequired(LockerOrder order) {
    return "INITIALIZED".equals(order.getStatus());
  }

  private String statusDescription(String status) {
    return switch (status) {
      case "INITIALIZED" -> "Order created";
      case "STORING" -> "Items stored in locker";
      case "COMPLETED" -> "Completed";
      case "CANCELED" -> "Canceled";
      default -> status;
    };
  }

  private void addHistory(Long orderId, String oldStatus, String newStatus, Long actorId, String note) {
    OrderStatusHistory history = new OrderStatusHistory();
    history.setOrderId(orderId);
    history.setOldStatus(oldStatus);
    history.setNewStatus(newStatus);
    history.setChangedByUserId(actorId);
    history.setNote(note);
    historyRepository.save(history);
  }

  private void publishStatusChanged(LockerOrder order, String oldStatus) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("orderId", order.getId());
    payload.put("userId", order.getUserId());
    payload.put("oldStatus", oldStatus);
    payload.put("newStatus", order.getStatus());
    publish(DomainEventNames.ORDER_STATUS_CHANGED, order, payload);
  }

  private void notifyStatus(LockerOrder order, String oldStatus) {
    try {
      notificationClient.requestNotification(
          new NotificationRequest(
              order.getUserId(),
              "Order " + order.getStatus().toLowerCase(),
              "Order " + order.getOrderCode() + " changed from " + oldStatus + " to " + order.getStatus(),
              "ORDER_STATUS",
              order.getId(),
              "ORDER"));
    } catch (Exception ex) {
      log.warn("Could not call notification-service for order {}: {}", order.getId(), ex.getMessage());
    }
  }

  private void publish(String eventName, LockerOrder order, Map<String, Object> payload) {
    try {
      rabbitTemplate.convertAndSend(
          DomainEventNames.EXCHANGE,
          eventName,
          DomainEvent.of(eventName, "order-service", payload));
    } catch (AmqpException ex) {
      log.warn("Could not publish {} for order {}: {}", eventName, order.getId(), ex.getMessage());
    }
  }

  private String generatePinCode() {
    return String.format("%06d", RANDOM.nextInt(1_000_000));
  }

  private String generateOrderCode() {
    // 36^6 vượt quá Integer.MAX_VALUE nên phải sinh bằng long
    String randomPart = Long.toString(RANDOM.nextLong(2_176_782_336L), 36).toUpperCase();
    return "ORD-" + LocalDate.now().toString().replace("-", "") + "-" + randomPart;
  }
}
