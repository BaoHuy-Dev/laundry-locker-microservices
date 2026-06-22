package com.huynqb.laundrylocker.locker.repository;

import com.huynqb.laundrylocker.locker.model.DroneUnit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DroneUnitRepository extends JpaRepository<DroneUnit, Long> {

  List<DroneUnit> findAllByOrderByLockerIdAscCodeAsc();

  boolean existsByCode(String code);
}
