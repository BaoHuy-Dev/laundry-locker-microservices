package com.huynqb.laundrylocker.iot.service;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LockerMqttService {

  @Value("${mqtt.broker-url:tcp://broker.hivemq.com:1883}")
  private String brokerUrl;

  @Value("${mqtt.client-id:laundry-iot-service}")
  private String clientId;

  @Value("${mqtt.topic-prefix:locker}")
  private String topicPrefix;

  public void sendUnlockCommand(Long lockerId, Long boxId) {
    publish(lockerId, "{\"box_id\":" + boxId + ",\"action\":\"OPEN\"}");
  }

  public void sendLockCommand(Long lockerId, Long boxId) {
    publish(lockerId, "{\"box_id\":" + boxId + ",\"action\":\"LOCK\"}");
  }

  private void publish(Long lockerId, String payload) {
    MqttClient client = null;
    try {
      client = new MqttClient(brokerUrl, clientId + "-" + System.nanoTime(), null);
      client.connect();
      MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
      message.setQos(1);
      client.publish(topicPrefix + "/commands/" + lockerId, message);
      client.disconnect();
    } catch (Exception ex) {
      log.warn("MQTT publish failed for locker {}: {}", lockerId, ex.getMessage());
    } finally {
      if (client != null && client.isConnected()) {
        try {
          client.disconnect();
        } catch (Exception ex) {
          log.debug("MQTT disconnect failed: {}", ex.getMessage());
        }
      }
    }
  }
}
