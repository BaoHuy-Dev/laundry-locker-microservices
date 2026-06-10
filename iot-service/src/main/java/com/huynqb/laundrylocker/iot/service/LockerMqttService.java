package com.huynqb.laundrylocker.iot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockerMqttService {

  @Value("${mqtt.broker-url:tcp://broker.hivemq.com:1883}")
  private String brokerUrl;

  @Value("${mqtt.client-id:laundry-iot-service}")
  private String clientId;

  @Value("${mqtt.topic-prefix:locker}")
  private String topicPrefix;

  private final ObjectMapper objectMapper;
  private final ApplicationContext applicationContext;
  private MqttClient client;
  private final ConcurrentHashMap<String, CompletableFuture<JsonNode>> pendingCommands = new ConcurrentHashMap<>();

  @PostConstruct
  public void init() {
    try {
      client = new MqttClient(brokerUrl, clientId + "-" + System.nanoTime(), new MemoryPersistence());
      MqttConnectionOptions options = new MqttConnectionOptions();
      options.setAutomaticReconnect(true);
      options.setCleanStart(true);
      
      client.connect(options);
      log.info("Connected to MQTT Broker: {}", brokerUrl);

      String resultTopic = "cabinet/+/command/+/result";
      client.subscribe(new String[]{resultTopic, "cabinet/+/heartbeat", "cabinet/+/locker/+/status"}, new int[]{1, 1, 1}, new IMqttMessageListener() {
        @Override
        public void messageArrived(String topic, MqttMessage message) {
          try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            log.debug("Received message on {}: {}", topic, payload);
            JsonNode data = objectMapper.readTree(payload);
            
            if (topic.endsWith("/result")) {
              if (data.has("commandId")) {
                String commandId = data.get("commandId").asText();
                CompletableFuture<JsonNode> future = pendingCommands.remove(commandId);
                if (future != null) {
                  future.complete(data);
                }
              }
            } else if (topic.endsWith("/heartbeat")) {
              // Extract cabinetName from "cabinet/{cabinetName}/heartbeat"
              String cabinetName = topic.split("/")[1];
              String status = data.has("status") ? data.get("status").asText() : "ONLINE";
              IotService iotService = applicationContext.getBean(IotService.class);
              iotService.updateStatus(new com.huynqb.laundrylocker.iot.dto.DeviceStatusRequest(cabinetName, null, status));
            } else if (topic.endsWith("/status")) {
              // Extract from "cabinet/{cabinetName}/locker/{lockerId}/status"
              if (data.has("lockerId") || data.has("slotIndex")) {
                Long boxId = data.has("slotIndex") ? data.get("slotIndex").asLong() : data.get("lockerId").asLong();
                String hwState = data.has("hwState") ? data.get("hwState").asText() : "UNKNOWN";
                IotService iotService = applicationContext.getBean(IotService.class);
                iotService.updateBoxStatus(new com.huynqb.laundrylocker.iot.dto.BoxStatusUpdateRequest(boxId, hwState));
              }
            }
          } catch (Exception e) {
            log.error("Error parsing MQTT message on topic {}: {}", topic, e.getMessage());
          }
        }
      });
      log.info("Subscribed to MQTT topics: {}, cabinet/+/heartbeat, cabinet/+/locker/+/status", resultTopic);

    } catch (MqttException e) {
      log.error("Failed to connect to MQTT broker", e);
    }
  }

  @PreDestroy
  public void cleanup() {
    if (client != null && client.isConnected()) {
      try {
        client.disconnect();
      } catch (MqttException e) {
        log.warn("Error disconnecting MQTT client", e);
      }
    }
  }

  public CompletableFuture<JsonNode> sendUnlockCommandAsync(Long lockerId, Long boxId) {
    String commandId = UUID.randomUUID().toString();
    CompletableFuture<JsonNode> future = new CompletableFuture<>();
    pendingCommands.put(commandId, future);

    try {
      String payload = String.format("{\"commandId\":\"%s\",\"box_id\":%d,\"action\":\"OPEN\",\"timeout\":15}", commandId, boxId);
      MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
      message.setQos(1);
      
      String topic = "cabinet/" + lockerId + "/command/open";
      client.publish(topic, message);
      log.info("Published OPEN command {} to {}", commandId, topic);
      
      // Auto cleanup future if not resolved in time
      future.orTimeout(20, TimeUnit.SECONDS).whenComplete((res, ex) -> {
        if (ex != null) {
          pendingCommands.remove(commandId);
          log.warn("Command {} timed out", commandId);
        }
      });

    } catch (Exception ex) {
      log.warn("MQTT publish failed for locker {}: {}", lockerId, ex.getMessage());
      future.completeExceptionally(ex);
      pendingCommands.remove(commandId);
    }
    
    return future;
  }
}
