package com.huynqb.laundrylocker.order.repository;

import com.huynqb.laundrylocker.order.model.Promotion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

  Optional<Promotion> findByCodeIgnoreCase(String code);

  List<Promotion> findByStatus(String status);
}
