package com.huynqb.laundrylocker.locker.repository;

import com.huynqb.laundrylocker.locker.model.LockerBox;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockerBoxRepository extends JpaRepository<LockerBox, Long> {

  List<LockerBox> findByLockerId(Long lockerId);

  List<LockerBox> findByLockerIdAndStatusAndActiveTrue(Long lockerId, String status);

  Optional<LockerBox> findFirstByLockerIdAndStatusAndActiveTrue(Long lockerId, String status);
}
