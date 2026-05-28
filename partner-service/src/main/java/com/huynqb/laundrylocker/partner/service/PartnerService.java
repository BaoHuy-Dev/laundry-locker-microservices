package com.huynqb.laundrylocker.partner.service;

import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.partner.dto.AccessCodeRequest;
import com.huynqb.laundrylocker.partner.dto.AccessCodeResponse;
import com.huynqb.laundrylocker.partner.dto.PartnerRequest;
import com.huynqb.laundrylocker.partner.dto.PartnerResponse;
import com.huynqb.laundrylocker.partner.model.PartnerProfile;
import com.huynqb.laundrylocker.partner.model.StaffAccessCode;
import com.huynqb.laundrylocker.partner.repository.PartnerRepository;
import com.huynqb.laundrylocker.partner.repository.StaffAccessCodeRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PartnerService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final PartnerRepository partnerRepository;
  private final StaffAccessCodeRepository codeRepository;

  @Transactional
  public PartnerResponse create(PartnerRequest request) {
    PartnerProfile partner = new PartnerProfile();
    partner.setUserId(request.userId());
    partner.setBusinessName(request.businessName());
    partner.setContactPhone(request.contactPhone());
    partner.setContactEmail(request.contactEmail());
    partner.setStatus(StringUtils.hasText(request.status()) ? request.status() : "PENDING");
    return toResponse(partnerRepository.save(partner));
  }

  @Transactional
  public AccessCodeResponse generateAccessCode(AccessCodeRequest request) {
    StaffAccessCode code = new StaffAccessCode();
    code.setPartnerId(request.partnerId());
    code.setOrderId(request.orderId());
    code.setAction(StringUtils.hasText(request.action()) ? request.action() : "COLLECT");
    code.setCode(String.format("%06d", RANDOM.nextInt(1_000_000)));
    code.setExpiresAt(LocalDateTime.now().plusHours(request.expiresInHours() == null ? 24 : request.expiresInHours()));
    return toResponse(codeRepository.save(code));
  }

  @Transactional(readOnly = true)
  public PartnerResponse get(Long id) {
    return partnerRepository.findById(id).map(this::toResponse).orElseThrow(() -> new NotFoundException("Partner", id));
  }

  @Transactional
  public PartnerResponse updateStatus(Long id, String status) {
    PartnerProfile partner = partnerRepository.findById(id).orElseThrow(() -> new NotFoundException("Partner", id));
    partner.setStatus(status.toUpperCase());
    return toResponse(partnerRepository.save(partner));
  }

  @Transactional(readOnly = true)
  public List<PartnerResponse> list() {
    return partnerRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<AccessCodeResponse> accessCodes(Long partnerId, Long orderId) {
    List<StaffAccessCode> codes;
    if (partnerId != null) {
      codes = codeRepository.findByPartnerId(partnerId);
    } else if (orderId != null) {
      codes = codeRepository.findByOrderId(orderId);
    } else {
      codes = codeRepository.findAll();
    }
    return codes.stream().map(this::toResponse).toList();
  }

  @Transactional
  public AccessCodeResponse cancelCode(Long id) {
    StaffAccessCode code = codeRepository.findById(id).orElseThrow(() -> new NotFoundException("StaffAccessCode", id));
    code.setStatus("CANCELLED");
    return toResponse(codeRepository.save(code));
  }

  @Transactional(readOnly = true)
  public AccessCodeResponse verifyCode(String codeValue) {
    StaffAccessCode code =
        codeRepository
            .findByCodeAndStatus(codeValue, "ACTIVE")
            .orElseThrow(() -> new NotFoundException("StaffAccessCode", -1L));
    if (code.getExpiresAt() != null && code.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new com.huynqb.laundrylocker.common.exception.BusinessException("ACCESS_CODE_EXPIRED", "Access code expired");
    }
    return toResponse(code);
  }

  private PartnerResponse toResponse(PartnerProfile partner) {
    return new PartnerResponse(
        partner.getId(), partner.getUserId(), partner.getBusinessName(), partner.getContactPhone(), partner.getContactEmail(), partner.getStatus());
  }

  private AccessCodeResponse toResponse(StaffAccessCode code) {
    return new AccessCodeResponse(
        code.getId(), code.getPartnerId(), code.getOrderId(), code.getCode(), code.getAction(), code.getStatus(), code.getExpiresAt());
  }
}
