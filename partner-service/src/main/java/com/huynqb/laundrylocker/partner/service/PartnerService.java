package com.huynqb.laundrylocker.partner.service;

import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.partner.client.LockerClient;
import com.huynqb.laundrylocker.partner.client.OrderClient;
import com.huynqb.laundrylocker.partner.client.StoreClient;
import com.huynqb.laundrylocker.partner.dto.AccessCodeRequest;
import com.huynqb.laundrylocker.partner.dto.AccessCodeResponse;
import com.huynqb.laundrylocker.partner.dto.PartnerRequest;
import com.huynqb.laundrylocker.partner.dto.PartnerResponse;
import com.huynqb.laundrylocker.partner.model.PartnerProfile;
import com.huynqb.laundrylocker.partner.model.StaffAccessCode;
import com.huynqb.laundrylocker.partner.repository.PartnerRepository;
import com.huynqb.laundrylocker.partner.repository.StaffAccessCodeRepository;
import java.security.SecureRandom;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
  private final OrderClient orderClient;
  private final StoreClient storeClient;
  private final LockerClient lockerClient;

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

  @Transactional
  public PartnerResponse updateByUser(Long userId, PartnerRequest request) {
    PartnerProfile partner =
        partnerRepository.findByUserId(userId).orElseThrow(() -> new NotFoundException("Partner", userId));
    partner.setBusinessName(request.businessName());
    partner.setContactPhone(request.contactPhone());
    partner.setContactEmail(request.contactEmail());
    if (StringUtils.hasText(request.status())) {
      partner.setStatus(request.status());
    }
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

  @Transactional(readOnly = true)
  public PartnerResponse getByUser(Long userId) {
    return partnerRepository.findByUserId(userId).map(this::toResponse).orElseThrow(() -> new NotFoundException("Partner", userId));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> dashboard(Long userId) {
    Long partnerId = getByUser(userId).id();
    return Map.of(
        "partnerId", partnerId,
        "pendingOrders", safeOrders("INITIALIZED").size(),
        "activeAccessCodes", accessCodes(partnerId, null).size());
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> orders(String status) {
    return safeOrders(status);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> order(Long orderId) {
    return orderClient.order(orderId).data();
  }

  @Transactional
  public Map<String, Object> acceptOrder(Long orderId, Long userId) {
    PartnerResponse partner = getByUser(userId);
    Map<String, Object> response =
        orderClient.updateStatus(orderId, Map.of("status", "WAITING", "staffId", userId)).data();
    generateAccessCode(new AccessCodeRequest(partner.id(), orderId, "COLLECT", 24));
    return response;
  }

  @Transactional
  public Map<String, Object> collectOrder(Long orderId, Long userId) {
    return orderClient.collect(orderId, userId).data();
  }

  @Transactional
  public Map<String, Object> processOrder(Long orderId, Long userId) {
    return orderClient.process(orderId, userId).data();
  }

  @Transactional
  public Map<String, Object> readyOrder(Long orderId, Long userId) {
    Map<String, Object> response = orderClient.ready(orderId, userId).data();
    generateAccessCode(new AccessCodeRequest(getByUser(userId).id(), orderId, "PICKUP", 24));
    return response;
  }

  @Transactional
  public Map<String, Object> updateOrderWeight(Long orderId, Map<String, Object> request, Long userId) {
    return orderClient.updateWeight(orderId, request, userId).data();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> stores() {
    try {
      return storeClient.stores().data();
    } catch (Exception ex) {
      return List.of();
    }
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> lockers() {
    try {
      return lockerClient.lockers().data();
    } catch (Exception ex) {
      return List.of();
    }
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> availableBoxes(Long lockerId) {
    try {
      return lockerClient.availableBoxes(lockerId).data();
    } catch (Exception ex) {
      return List.of();
    }
  }

  @Transactional(readOnly = true)
  public Map<String, Object> orderStatistics() {
    return Map.of("totalOrders", safeOrders(null).size(), "pendingOrders", safeOrders("INITIALIZED").size());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> revenue() {
    BigDecimal totalRevenue =
        safeOrders(null).stream()
            .filter(order -> "COMPLETED".equalsIgnoreCase(String.valueOf(order.get("status"))))
            .map(order -> decimal(order.get("totalPrice")))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return Map.of("totalRevenue", totalRevenue);
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

  private List<Map<String, Object>> safeOrders(String status) {
    try {
      return orderClient.orders(status).data();
    } catch (Exception ex) {
      return List.of();
    }
  }

  private BigDecimal decimal(Object value) {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return BigDecimal.ZERO;
    }
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
