package com.huynqb.laundrylocker.iot.repository;

import com.huynqb.laundrylocker.iot.model.DeviceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceStatusRepository extends JpaRepository<DeviceStatus, Long> {

  Optional<DeviceStatus> findByDeviceId(String deviceId);

  List<DeviceStatus> findByLockerId(Long lockerId);
}
