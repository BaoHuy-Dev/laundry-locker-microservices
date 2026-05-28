package com.huynqb.laundrylocker.order.service;

import com.huynqb.laundrylocker.common.dto.NotificationRequest;
import com.huynqb.laundrylocker.common.dto.OrderSummary;
import com.huynqb.laundrylocker.common.event.DomainEvent;
import com.huynqb.laundrylocker.common.event.DomainEventNames;
import com.huynqb.laundrylocker.common.exception.BusinessException;
import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.order.client.LaundryClient;
import com.huynqb.laundrylocker.order.client.LockerClient;
import com.huynqb.laundrylocker.order.client.NotificationClient;
import com.huynqb.laundrylocker.order.client.UserClient;
import com.huynqb.laundrylocker.order.dto.CreateOrderRequest;
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
import com.huynqb.laundrylocker.order.model.LaundryOrder;
import com.huynqb.laundrylocker.order.model.OrderComplaint;
import com.huynqb.laundrylocker.order.model.OrderDetail;
import com.huynqb.laundrylocker.order.model.OrderRating;
import com.huynqb.laundrylocker.order.model.OrderStatusHistory;
import com.huynqb.laundrylocker.order.model.Promotion;
import com.huynqb.laundrylocker.order.repository.LaundryOrderRepository;
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

  private final LaundryOrderRepository orderRepository;
  private final OrderDetailRepository detailRepository;
  private final OrderStatusHistoryRepository historyRepository;
  private final OrderRatingRepository ratingRepository;
  private final OrderComplaintRepository complaintRepository;
  private final PromotionRepository promotionRepository;
  private final RabbitTemplate rabbitTemplate;
  private final UserClient userClient;
  private final LockerClient lockerClient;
  private final LaundryClient laundryClient;
  private final NotificationClient notificationClient;

  @Value("${app.order.pickup-hours-limit:24}")
  private int pickupHoursLimit;

  @Value("${app.order.pickup-overtime-fee-per-hour:500}")
  private int overtimeFeePerHour;

  @Value("${app.order.pickup-max-overtime-fee:50000}")
  private int maxOvertimeFee;

  @Value("${app.order.pickup-max-overtime-percent:50}")
  private int maxOvertimePercent;

  @Transactional
  public OrderResponse create(CreateOrderRequest request) {
    userClient.getUser(request.userId());
    LaundryOrder order = new LaundryOrder();
    order.setOrderCode(generateOrderCode());
    order.setUserId(request.userId());
    order.setReceiverId(request.receiverId());
    order.setReceiverPhone(request.receiverPhone());
    order.setReceiverName(request.receiverName());
    order.setLockerId(request.lockerId());
    order.setStoreId(request.storeId());
    order.setSendBoxId(resolveAndReserveSendBox(request));
    order.setType(StringUtils.hasText(request.type()) ? request.type().toUpperCase() : "LAUNDRY");
    order.setServiceCategory(
        StringUtils.hasText(request.serviceCategory()) ? request.serviceCategory().toUpperCase() : "LAUNDRY");
    order.setStatus("INITIALIZED");
    order.setPinCode(generatePinCode());
    order.setPinCodeIssuedAt(LocalDateTime.now());
    order.setCustomerNote(request.customerNote());
    order.setDeliveryAddress(request.deliveryAddress());
    order.setIntendedReceiveAt(request.intendedReceiveAt());
    if (request.estimatedWeight() != null) {
      order.setActualWeight(request.estimatedWeight());
    }

    LaundryOrder saved = orderRepository.save(order);
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
    LaundryOrder order = find(id);
    return transition(order, request.status(), request.staffId(), request.receiveBoxId(), null);
  }

  @Transactional
  public OrderResponse confirm(Long id, Long userId) {
    LaundryOrder order = find(id);
    assertOwner(order, userId);
    return transition(order, "WAITING", userId, null, "Customer confirmed items dropped");
  }

  @Transactional
  public OrderResponse collect(Long id, Long staffId) {
    LaundryOrder order = find(id);
    validateStatus(order, Set.of("WAITING"));
    if (order.getSendBoxId() != null) {
      lockerClient.releaseBox(order.getSendBoxId());
    }
    return transition(order, "COLLECTED", staffId, null, "Staff collected order");
  }

  @Transactional
  public OrderResponse updateWeight(Long id, UpdateOrderWeightRequest request, Long staffId) {
    LaundryOrder order = find(id);
    validateStatus(order, Set.of("COLLECTED", "PROCESSING"));
    order.setActualWeight(request.actualWeight());
    order.setWeightUnit(StringUtils.hasText(request.weightUnit()) ? request.weightUnit() : "kg");
    order.setStaffId(staffId);
    order.setStaffNote(request.staffNote());
    if (request.items() != null && !request.items().isEmpty()) {
      detailRepository.deleteByOrderId(id);
      BigDecimal total = saveItemDetails(id, request.items(), order.getActualWeight());
      order.setOriginalPrice(total);
      order.setTotalPrice(total.subtract(order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount()).max(BigDecimal.ZERO));
    }
    return toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse process(Long id, Long staffId) {
    LaundryOrder order = find(id);
    validateStatus(order, Set.of("COLLECTED"));
    return transition(order, "PROCESSING", staffId, null, "Processing started");
  }

  @Transactional
  public OrderResponse ready(Long id, Long staffId) {
    LaundryOrder order = find(id);
    validateStatus(order, Set.of("PROCESSING"));
    return transition(order, "READY", staffId, null, "Order is ready");
  }

  @Transactional
  public OrderResponse returnOrder(Long id, Long receiveBoxId, Long staffId) {
    LaundryOrder order = find(id);
    validateStatus(order, Set.of("READY"));
    lockerClient.reserveBox(receiveBoxId);
    order.setReceiveBoxId(receiveBoxId);
    order.setReturnedAt(LocalDateTime.now());
    order.setPickupDeadline(order.getReturnedAt().plusHours(pickupHoursLimit));
    order.setPinCode(generatePinCode());
    order.setPinCodeIssuedAt(LocalDateTime.now());
    return transition(order, "RETURNED", staffId, receiveBoxId, "Order returned to locker");
  }

  @Transactional
  public OrderResponse complete(Long id, Long userId) {
    LaundryOrder order = find(id);
    assertOwner(order, userId);
    validateStatus(order, Set.of("RETURNED"));
    BigDecimal overtime = calculatePickupOvertimeFee(order);
    if (overtime.compareTo(BigDecimal.ZERO) > 0) {
      order.setExtraFee(order.getExtraFee().add(overtime));
      order.setTotalPrice(order.getTotalPrice().add(overtime));
    }
    if (order.getReceiveBoxId() != null) {
      lockerClient.releaseBox(order.getReceiveBoxId());
    }
    order.setCompletedAt(LocalDateTime.now());
    order.setPinCode(null);
    return transition(order, "COMPLETED", userId, order.getReceiveBoxId(), "Customer completed pickup");
  }

  @Transactional
  public OrderResponse cancel(Long id, Integer reason, Long userId) {
    LaundryOrder order = find(id);
    if (!CANCELABLE.contains(order.getStatus())) {
      throw new BusinessException("ORDER_STATUS_INVALID", "Order cannot be canceled at this status");
    }
    order.setCancelReason(reason);
    releaseBoxes(order);
    return transition(order, "CANCELED", userId, null, "Order canceled");
  }

  @Transactional
  public OrderResponse resetPin(Long id, Long userId) {
    LaundryOrder order = find(id);
    assertOwner(order, userId);
    order.setPinCode(generatePinCode());
    order.setPinCodeIssuedAt(LocalDateTime.now());
    return toResponse(orderRepository.save(order));
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
    LaundryOrder order = find(id);
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

  @Transactional
  public OrderRatingResponse rate(Long orderId, OrderRatingRequest request, Long userId) {
    LaundryOrder order = find(orderId);
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
    LaundryOrder order = find(orderId);
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
  public List<Promotion> activePromotions() {
    return promotionRepository.findByStatus("ACTIVE").stream().filter(Promotion::activeNow).toList();
  }

  private OrderResponse transition(
      LaundryOrder order, String newStatus, Long actorId, Long receiveBoxId, String note) {
    String oldStatus = order.getStatus();
    order.setStatus(newStatus.toUpperCase());
    if (actorId != null && !"COMPLETED".equals(order.getStatus()) && !"CANCELED".equals(order.getStatus())) {
      order.setStaffId(actorId);
    }
    if (receiveBoxId != null) {
      order.setReceiveBoxId(receiveBoxId);
    }
    LaundryOrder saved = orderRepository.save(order);
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
    if (request.serviceIds() == null || request.serviceIds().isEmpty()) {
      return BigDecimal.ZERO;
    }
    BigDecimal estimate =
        laundryClient
            .estimate(request.serviceIds(), request.estimatedWeight())
            .data();
    BigDecimal perService = request.serviceIds().isEmpty() ? BigDecimal.ZERO : estimate.divide(BigDecimal.valueOf(request.serviceIds().size()), 2, RoundingMode.HALF_UP);
    request.serviceIds().forEach(serviceId -> saveDetail(orderId, serviceId, BigDecimal.ONE, perService, null));
    return estimate;
  }

  private BigDecimal saveItemDetails(Long orderId, List<OrderItemRequest> items, BigDecimal defaultQuantity) {
    BigDecimal total = BigDecimal.ZERO;
    for (OrderItemRequest item : items) {
      BigDecimal quantity = item.quantity() == null ? (defaultQuantity == null ? BigDecimal.ONE : defaultQuantity) : item.quantity();
      BigDecimal price = laundryClient.estimate(List.of(item.serviceId()), quantity).data();
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

  private void applyPromotion(LaundryOrder order, String promotionCode, List<String> promotionCodes) {
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

  private void releaseBoxes(LaundryOrder order) {
    if (order.getSendBoxId() != null) {
      lockerClient.releaseBox(order.getSendBoxId());
    }
    if (order.getReceiveBoxId() != null) {
      lockerClient.releaseBox(order.getReceiveBoxId());
    }
  }

  private BigDecimal calculatePickupOvertimeFee(LaundryOrder order) {
    if (order.getPickupDeadline() == null || !LocalDateTime.now().isAfter(order.getPickupDeadline())) {
      return BigDecimal.ZERO;
    }
    long hours = ChronoUnit.HOURS.between(order.getPickupDeadline(), LocalDateTime.now());
    BigDecimal raw = BigDecimal.valueOf(Math.max(0, hours) * overtimeFeePerHour);
    BigDecimal percentageCap =
        order.getTotalPrice().multiply(BigDecimal.valueOf(maxOvertimePercent)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    return raw.min(BigDecimal.valueOf(maxOvertimeFee)).min(percentageCap);
  }

  private void validateStatus(LaundryOrder order, Set<String> statuses) {
    if (!statuses.contains(order.getStatus())) {
      throw new BusinessException("ORDER_STATUS_INVALID", "Order status does not allow this action");
    }
  }

  private void assertOwner(LaundryOrder order, Long userId) {
    if (userId != null && !userId.equals(order.getUserId())) {
      throw new BusinessException("ORDER_FORBIDDEN", "Order does not belong to user");
    }
  }

  private LaundryOrder find(Long id) {
    return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order", id));
  }

  private OrderSummary toSummary(LaundryOrder order) {
    return new OrderSummary(order.getId(), order.getUserId(), order.getStatus(), order.getTotalPrice());
  }

  private OrderResponse toResponse(LaundryOrder order) {
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

  private String nextAction(LaundryOrder order) {
    return switch (order.getStatus()) {
      case "INITIALIZED" -> "STORAGE".equals(order.getServiceCategory()) ? "PAY_AND_DROP" : "DROP_ITEMS";
      case "WAITING" -> "WAIT_FOR_STAFF";
      case "COLLECTED", "PROCESSING" -> "WAIT_FOR_PROCESSING";
      case "READY" -> "WAIT_FOR_RETURN";
      case "RETURNED" -> paymentRequired(order) ? "PAY_AND_PICKUP" : "PICKUP";
      case "COMPLETED" -> "DONE";
      case "CANCELED" -> "CANCELED";
      default -> "UNKNOWN";
    };
  }

  private String nextActionMessage(LaundryOrder order) {
    return switch (nextAction(order)) {
      case "DROP_ITEMS" -> "Place items in locker and confirm the order.";
      case "PAY_AND_DROP" -> "Pay and place storage items in locker.";
      case "WAIT_FOR_STAFF" -> "Waiting for staff to collect items.";
      case "WAIT_FOR_PROCESSING" -> "Items are being processed.";
      case "WAIT_FOR_RETURN" -> "Waiting for staff to return items to locker.";
      case "PAY_AND_PICKUP" -> "Pay the order and pick up items.";
      case "PICKUP" -> "Pick up items from locker.";
      default -> order.getStatus();
    };
  }

  private boolean paymentRequired(LaundryOrder order) {
    if ("STORAGE".equals(order.getServiceCategory())) {
      return "INITIALIZED".equals(order.getStatus());
    }
    return "RETURNED".equals(order.getStatus());
  }

  private String statusDescription(String status) {
    return switch (status) {
      case "INITIALIZED" -> "Order created";
      case "WAITING" -> "Waiting for staff collection";
      case "COLLECTED" -> "Collected by staff";
      case "PROCESSING" -> "Processing";
      case "READY" -> "Ready to return";
      case "RETURNED" -> "Returned to locker";
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

  private void publishStatusChanged(LaundryOrder order, String oldStatus) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("orderId", order.getId());
    payload.put("userId", order.getUserId());
    payload.put("oldStatus", oldStatus);
    payload.put("newStatus", order.getStatus());
    publish(DomainEventNames.ORDER_STATUS_CHANGED, order, payload);
  }

  private void notifyStatus(LaundryOrder order, String oldStatus) {
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

  private void publish(String eventName, LaundryOrder order, Map<String, Object> payload) {
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
    String randomPart = Integer.toString(RANDOM.nextInt(36 * 36 * 36 * 36 * 36 * 36), 36).toUpperCase();
    return "ORD-" + LocalDate.now().toString().replace("-", "") + "-" + randomPart;
  }
}
