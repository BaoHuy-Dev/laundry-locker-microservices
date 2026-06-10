package com.huynqb.laundrylocker.order.service;

import com.huynqb.laundrylocker.common.dto.NotificationRequest;
import com.huynqb.laundrylocker.common.dto.OrderSummary;
import com.huynqb.laundrylocker.common.event.DomainEvent;
import com.huynqb.laundrylocker.common.event.DomainEventNames;
import com.huynqb.laundrylocker.common.exception.BusinessException;
import com.huynqb.laundrylocker.common.exception.NotFoundException;
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
    return transition(order, request.status(), request.staffId(), request.receiveBoxId(), null);
  }

  @Transactional
  public OrderResponse confirm(Long id, Long userId) {
    LockerOrder order = find(id);
    assertOwner(order, userId);
    return transition(order, "STORING", userId, null, "Customer confirmed items dropped in locker");
  }



  @Transactional
  public OrderResponse complete(Long id, Long userId) {
    LockerOrder order = find(id);
    assertOwner(order, userId);
    validateStatus(order, Set.of("STORING", "RETURNED"));
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
  public OrderResponse pickupStorage(Long id, Long userId) {
    LockerOrder order = find(id);
    assertOwner(order, userId);
    if (!"STORAGE".equalsIgnoreCase(order.getType())
        && !"STORAGE".equalsIgnoreCase(order.getServiceCategory())) {
      throw new BusinessException("ORDER_STATUS_INVALID", "Only storage orders can use pickup-storage");
    }
    validateStatus(order, Set.of("STORING", "INITIALIZED", "RETURNED"));
    releaseBoxes(order);
    order.setCompletedAt(LocalDateTime.now());
    order.setPinCode(null);
    return transition(order, "COMPLETED", userId, order.getReceiveBoxId(), "Storage order picked up");
  }

  @Transactional
  public OrderResponse reorder(Long originalOrderId, Long userId) {
    LockerOrder original = find(originalOrderId);
    assertOwner(original, userId);
    if (!Set.of("COMPLETED", "CANCELED").contains(original.getStatus())) {
      throw new BusinessException("ORDER_STATUS_INVALID", "Only completed or canceled orders can be reordered");
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
    result.put("totalOrders", orders.size());
    result.put(
        "byStatus",
        orders.stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    LockerOrder::getStatus, java.util.stream.Collectors.counting())));
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

  @Transactional
  public Map<String, Object> autoCancelUnconfirmedOrders() {
    List<LockerOrder> canceled =
        orderRepository.findByStatusOrderByCreatedAtDesc("INITIALIZED").stream()
            .filter(order -> order.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24)))
            .peek(order -> order.setStatus("CANCELED"))
            .toList();
    return Map.of("canceledOrders", canceled.size());
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
    String randomPart = Integer.toString(RANDOM.nextInt(36 * 36 * 36 * 36 * 36 * 36), 36).toUpperCase();
    return "ORD-" + LocalDate.now().toString().replace("-", "") + "-" + randomPart;
  }
}
