package com.huynqb.laundrylocker.partner.repository;

import com.huynqb.laundrylocker.partner.model.PartnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerRepository extends JpaRepository<PartnerProfile, Long> {}
