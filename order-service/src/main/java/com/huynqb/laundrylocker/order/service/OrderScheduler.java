package com.huynqb.laundrylocker.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

  private final OrderService orderService;

  @Scheduled(fixedDelayString = "${app.order.reminder-interval-ms:600000}", initialDelay = 60000)
  public void remindOverduePickups() {
    try {
      orderService.sendPickupReminders();
    } catch (Exception ex) {
      log.warn("Pickup reminder job failed: {}", ex.getMessage());
    }
  }

  // Safety net: orders completed through the blunt PATCH /status endpoint by
  // legacy clients may leave cells held; sweep them nightly.
  @Scheduled(cron = "${app.order.release-sweep-cron:0 15 3 * * *}")
  public void releaseCompletedBoxes() {
    try {
      orderService.releaseBoxesAfterCompletion();
    } catch (Exception ex) {
      log.warn("Release sweep job failed: {}", ex.getMessage());
    }
  }
}
