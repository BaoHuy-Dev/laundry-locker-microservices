package com.huynqb.laundrylocker.locker.repository;

import com.huynqb.laundrylocker.locker.model.LockerUnit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockerUnitRepository extends JpaRepository<LockerUnit, Long> {

  List<LockerUnit> findByStoreId(Long storeId);
}
