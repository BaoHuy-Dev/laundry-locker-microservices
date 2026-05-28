package com.huynqb.laundrylocker.store.repository;

import com.huynqb.laundrylocker.store.model.StoreLocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<StoreLocation, Long> {

  List<StoreLocation> findByStatusAndActiveTrue(String status);
}
