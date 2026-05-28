package com.huynqb.laundrylocker.notification.service;

import com.huynqb.laundrylocker.common.event.DomainEvent;
import com.huynqb.laundrylocker.notification.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;

  @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
  public void onEvent(DomainEvent event) {
    notificationService.consumeDomainEvent(event);
  }
}
