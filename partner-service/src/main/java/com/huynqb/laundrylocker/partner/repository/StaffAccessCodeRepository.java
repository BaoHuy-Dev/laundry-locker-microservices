package com.huynqb.laundrylocker.partner.repository;

import com.huynqb.laundrylocker.partner.model.StaffAccessCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAccessCodeRepository extends JpaRepository<StaffAccessCode, Long> {

  List<StaffAccessCode> findByPartnerId(Long partnerId);

  List<StaffAccessCode> findByOrderId(Long orderId);

  Optional<StaffAccessCode> findByCodeAndStatus(String code, String status);
}
