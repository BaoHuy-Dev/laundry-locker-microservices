package com.huynqb.laundrylocker.notification.service;

import com.huynqb.laundrylocker.common.dto.NotificationRequest;
import com.huynqb.laundrylocker.common.event.DomainEvent;
import com.huynqb.laundrylocker.common.event.DomainEventNames;
import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.notification.dto.FcmTokenRequest;
import com.huynqb.laundrylocker.notification.dto.NotificationResponse;
import com.huynqb.laundrylocker.notification.model.FcmToken;
import com.huynqb.laundrylocker.notification.model.NotificationMessage;
import com.huynqb.laundrylocker.notification.repository.FcmTokenRepository;
import com.huynqb.laundrylocker.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final FcmTokenRepository fcmTokenRepository;
  private final RabbitTemplate rabbitTemplate;
  private final FcmPushNotificationService fcmPushNotificationService;

  @Transactional
  public NotificationResponse create(NotificationRequest request) {
    NotificationMessage notification = new NotificationMessage();
    notification.setUserId(request.userId());
    notification.setTitle(request.title());
    notification.setMessage(request.message());
    notification.setType(StringUtils.hasText(request.type()) ? request.type() : "SYSTEM");
    notification.setReferenceId(request.referenceId());
    notification.setReferenceType(request.referenceType());
    NotificationMessage saved = notificationRepository.save(notification);
    fcmPushNotificationService.sendToUser(
        saved.getUserId(),
        saved.getTitle(),
        saved.getMessage(),
        Map.of(
            "notificationId",
            String.valueOf(saved.getId()),
            "type",
            saved.getType(),
            "referenceType",
            saved.getReferenceType() == null ? "" : saved.getReferenceType(),
            "referenceId",
            saved.getReferenceId() == null ? "" : String.valueOf(saved.getReferenceId())));
    publishNotificationRequested(saved);
    return toResponse(saved);
  }

  @Transactional
  public List<NotificationResponse> broadcast(NotificationRequest request) {
    List<Long> userIds = fcmTokenRepository.findAll().stream().map(FcmToken::getUserId).distinct().toList();
    List<NotificationResponse> responses =
        userIds.stream()
            .map(userId -> create(new NotificationRequest(userId, request.title(), request.message(), request.type(), request.referenceId(), request.referenceType())))
            .toList();
    fcmPushNotificationService.broadcast(request.title(), request.message(), Map.of("type", request.type() == null ? "SYSTEM" : request.type()));
    return responses;
  }

  @Transactional
  public void upsertFcmToken(FcmTokenRequest request) {
    FcmToken token =
        fcmTokenRepository
            .findByToken(request.token())
            .orElseGet(FcmToken::new);
    token.setUserId(request.userId());
    token.setToken(request.token());
    token.setDeviceType(request.deviceType());
    fcmTokenRepository.save(token);
  }

  @Transactional
  public void deleteFcmToken(Long userId, String token) {
    fcmTokenRepository.deleteByUserIdAndToken(userId, token);
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> getByUser(Long userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> getUnreadByUser(Long userId) {
    return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public long countUnread(Long userId) {
    return notificationRepository.countByUserIdAndIsReadFalse(userId);
  }

  @Transactional
  public NotificationResponse markRead(Long id) {
    NotificationMessage notification =
        notificationRepository.findById(id).orElseThrow(() -> new NotFoundException("Notification", id));
    notification.setIsRead(true);
    notification.setStatus("READ");
    notification.setReadAt(java.time.LocalDateTime.now());
    return toResponse(notificationRepository.save(notification));
  }

  @Transactional
  public int markAllRead(Long userId) {
    return notificationRepository.markAllAsRead(userId);
  }

  @Transactional
  public void deleteAll(Long userId) {
    notificationRepository.deleteByUserId(userId);
  }

  @Transactional
  public void delete(Long id) {
    if (!notificationRepository.existsById(id)) {
      throw new NotFoundException("Notification", id);
    }
    notificationRepository.deleteById(id);
  }

  @Transactional
  public void consumeDomainEvent(DomainEvent event) {
    Map<String, Object> payload = event.payload();
    Long userId = asLong(payload.get("userId"));
    if (userId == null) {
      log.debug("Ignoring {} without userId", event.type());
      return;
    }
    String title = switch (event.type()) {
      case DomainEventNames.ORDER_STATUS_CHANGED -> "Order status changed";
      case DomainEventNames.PAYMENT_COMPLETED -> "Payment completed";
      case DomainEventNames.PAYMENT_FAILED -> "Payment failed";
      default -> "Notification";
    };
    String message = event.type() + " event received";
    create(new NotificationRequest(userId, title, message, event.type(), asLong(payload.get("orderId")), "EVENT"));
  }

  private NotificationResponse toResponse(NotificationMessage notification) {
    return new NotificationResponse(
        notification.getId(), notification.getUserId(), notification.getTitle(), notification.getMessage(),
        notification.getType(), notification.getIsRead(), notification.getReferenceId(), notification.getReferenceType(),
        notification.getStatus(), notification.getReadAt(), notification.getCreatedAt());
  }

  private void publishNotificationRequested(NotificationMessage notification) {
    try {
      rabbitTemplate.convertAndSend(
          DomainEventNames.EXCHANGE,
          DomainEventNames.NOTIFICATION_REQUESTED,
          DomainEvent.of(
              DomainEventNames.NOTIFICATION_REQUESTED,
              "notification-service",
              Map.of("notificationId", notification.getId(), "userId", notification.getUserId())));
    } catch (AmqpException ex) {
      log.warn("Could not publish notification.requested: {}", ex.getMessage());
    }
  }

  private Long asLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text && StringUtils.hasText(text)) {
      return Long.parseLong(text);
    }
    return null;
  }
}
