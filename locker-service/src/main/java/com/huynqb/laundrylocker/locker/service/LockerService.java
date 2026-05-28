package com.huynqb.laundrylocker.locker.service;

import com.huynqb.laundrylocker.common.dto.LockerBoxSummary;
import com.huynqb.laundrylocker.common.event.DomainEvent;
import com.huynqb.laundrylocker.common.event.DomainEventNames;
import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.locker.dto.BoxRequest;
import com.huynqb.laundrylocker.locker.dto.LockerRequest;
import com.huynqb.laundrylocker.locker.dto.LockerReportRequest;
import com.huynqb.laundrylocker.locker.dto.LockerReportResponse;
import com.huynqb.laundrylocker.locker.dto.LockerResponse;
import com.huynqb.laundrylocker.locker.model.LockerBox;
import com.huynqb.laundrylocker.locker.model.LockerReport;
import com.huynqb.laundrylocker.locker.model.LockerUnit;
import com.huynqb.laundrylocker.locker.repository.LockerBoxRepository;
import com.huynqb.laundrylocker.locker.repository.LockerReportRepository;
import com.huynqb.laundrylocker.locker.repository.LockerUnitRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockerService {

  private final LockerUnitRepository lockerRepository;
  private final LockerBoxRepository boxRepository;
  private final LockerReportRepository reportRepository;
  private final RabbitTemplate rabbitTemplate;

  @Transactional
  public LockerResponse createLocker(LockerRequest request) {
    LockerUnit locker = new LockerUnit();
    locker.setStoreId(request.storeId());
    locker.setCode(request.code());
    locker.setName(request.name());
    locker.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ACTIVE");
    locker.setAddress(request.address());
    locker.setLatitude(request.latitude());
    locker.setLongitude(request.longitude());
    return toResponse(lockerRepository.save(locker));
  }

  @Transactional
  public LockerBoxSummary createBox(BoxRequest request) {
    LockerBox box = new LockerBox();
    box.setLockerId(request.lockerId());
    box.setBoxNumber(request.boxNumber());
    box.setSize(StringUtils.hasText(request.size()) ? request.size() : "MEDIUM");
    box.setStatus(StringUtils.hasText(request.status()) ? request.status() : "AVAILABLE");
    return toSummary(boxRepository.save(box));
  }

  @Transactional
  public LockerBoxSummary openBox(Long boxId) {
    LockerBox box = findBox(boxId);
    publishBoxOpened(box);
    return toSummary(box);
  }

  @Transactional
  public LockerBoxSummary reserveBox(Long boxId) {
    LockerBox box = findBox(boxId);
    if (!"AVAILABLE".equalsIgnoreCase(box.getStatus())) {
      throw new com.huynqb.laundrylocker.common.exception.BusinessException("BOX_NOT_AVAILABLE", "Box is not available");
    }
    box.setStatus("OCCUPIED");
    return toSummary(boxRepository.save(box));
  }

  @Transactional
  public LockerBoxSummary releaseBox(Long boxId) {
    LockerBox box = findBox(boxId);
    box.setStatus("AVAILABLE");
    return toSummary(boxRepository.save(box));
  }

  @Transactional(readOnly = true)
  public LockerResponse getLocker(Long id) {
    return toResponse(lockerRepository.findById(id).orElseThrow(() -> new NotFoundException("Locker", id)));
  }

  @Transactional(readOnly = true)
  public LockerBoxSummary getBox(Long id) {
    return toSummary(findBox(id));
  }

  @Transactional(readOnly = true)
  public List<LockerResponse> listLockers() {
    return lockerRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<LockerResponse> listLockers(Long storeId) {
    return (storeId == null ? lockerRepository.findAll() : lockerRepository.findByStoreId(storeId)).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<LockerBoxSummary> listBoxes(Long lockerId) {
    return boxRepository.findByLockerId(lockerId).stream().map(this::toSummary).toList();
  }

  @Transactional(readOnly = true)
  public List<LockerBoxSummary> listAvailableBoxes(Long lockerId) {
    return boxRepository.findByLockerIdAndStatusAndActiveTrue(lockerId, "AVAILABLE").stream().map(this::toSummary).toList();
  }

  @Transactional
  public LockerReportResponse report(Long lockerId, LockerReportRequest request) {
    LockerReport report = new LockerReport();
    report.setLockerId(lockerId);
    report.setUserId(request.userId());
    report.setTitle(request.title());
    report.setDescription(request.description());
    return toReport(reportRepository.save(report));
  }

  @Transactional
  public LockerReportResponse resolveReport(Long reportId, Long userId) {
    LockerReport report = reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException("LockerReport", reportId));
    report.setStatus("RESOLVED");
    report.setResolvedByUserId(userId);
    report.setResolvedAt(java.time.LocalDateTime.now());
    return toReport(reportRepository.save(report));
  }

  @Transactional(readOnly = true)
  public List<LockerReportResponse> myReports(Long userId) {
    return reportRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toReport).toList();
  }

  private LockerBox findBox(Long id) {
    return boxRepository.findById(id).orElseThrow(() -> new NotFoundException("Box", id));
  }

  private LockerResponse toResponse(LockerUnit locker) {
    return new LockerResponse(
        locker.getId(), locker.getStoreId(), locker.getCode(), locker.getName(), locker.getStatus(),
        locker.getAddress(), locker.getLatitude(), locker.getLongitude());
  }

  private LockerBoxSummary toSummary(LockerBox box) {
    return new LockerBoxSummary(box.getLockerId(), box.getId(), null, box.getBoxNumber(), box.getStatus());
  }

  private LockerReportResponse toReport(LockerReport report) {
    return new LockerReportResponse(
        report.getId(),
        report.getLockerId(),
        report.getUserId(),
        report.getTitle(),
        report.getDescription(),
        report.getStatus(),
        report.getResolvedByUserId(),
        report.getResolvedAt(),
        report.getCreatedAt());
  }

  private void publishBoxOpened(LockerBox box) {
    try {
      rabbitTemplate.convertAndSend(
          DomainEventNames.EXCHANGE,
          DomainEventNames.LOCKER_BOX_OPENED,
          DomainEvent.of(
              DomainEventNames.LOCKER_BOX_OPENED,
              "locker-service",
              Map.of("lockerId", box.getLockerId(), "boxId", box.getId(), "boxNumber", box.getBoxNumber())));
    } catch (AmqpException ex) {
      log.warn("Could not publish locker.box.opened for box {}: {}", box.getId(), ex.getMessage());
    }
  }
}
