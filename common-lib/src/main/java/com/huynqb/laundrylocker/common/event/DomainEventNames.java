package com.huynqb.laundrylocker.common.event;

public final class DomainEventNames {

  public static final String EXCHANGE = "laundry.events";
  public static final String ORDER_CREATED = "order.created";
  public static final String ORDER_STATUS_CHANGED = "order.status.changed";
  public static final String PAYMENT_COMPLETED = "payment.completed";
  public static final String PAYMENT_FAILED = "payment.failed";
  public static final String NOTIFICATION_REQUESTED = "notification.requested";
  public static final String LOCKER_BOX_OPENED = "locker.box.opened";
  public static final String LOCKER_BOX_FAULT = "locker.box.fault";
  public static final String IOT_DEVICE_STATUS_CHANGED = "iot.device.status.changed";

  private DomainEventNames() {}
}
