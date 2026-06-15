package com.huynqb.laundrylocker.locker.service;

import com.huynqb.laundrylocker.common.dto.LockerBoxSummary;
import com.huynqb.laundrylocker.common.event.DomainEvent;
import com.huynqb.laundrylocker.common.event.DomainEventNames;
import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.locker.dto.BoxRequest;
import com.huynqb.laundrylocker.locker.dto.CellResponse;
import com.huynqb.laundrylocker.locker.dto.FaultCellResponse;
import com.huynqb.laundrylocker.locker.dto.LockerLayoutResponse;
import com.huynqb.laundrylocker.locker.dto.LockerStatsResponse;
import com.huynqb.laundrylocker.locker.dto.LockerRequest;
import com.huynqb.laundrylocker.locker.dto.LockerReportRequest;
import com.huynqb.laundrylocker.locker.dto.LockerReportResponse;
import com.huynqb.laundrylocker.locker.dto.RepairLogResponse;
import com.huynqb.laundrylocker.locker.dto.LockerResponse;
import com.huynqb.laundrylocker.locker.model.LockerBox;
import com.huynqb.laundrylocker.locker.model.LockerReport;
import com.huynqb.laundrylocker.locker.model.RepairLog;
import com.huynqb.laundrylocker.locker.model.LockerUnit;
import com.huynqb.laundrylocker.locker.repository.LockerBoxRepository;
import com.huynqb.laundrylocker.locker.repository.LockerReportRepository;
import com.huynqb.laundrylocker.locker.repository.LockerUnitRepository;
import com.huynqb.laundrylocker.locker.repository.RepairLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockerService {

  private static final List<String> OPEN_REPORT_STATUSES = List.of("OPEN", "IN_PROGRESS");

  private final LockerUnitRepository lockerRepository;
  private final LockerBoxRepository boxRepository;
  private final LockerReportRepository reportRepository;
  private final RepairLogRepository repairLogRepository;

  /// SLA: số giờ tối đa để xử lý một phiếu bảo trì trước khi bị coi là quá hạn.
  @Value("${app.maintenance.sla-hours:4}")
  private int slaHours;
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
    box.setCellType(StringUtils.hasText(request.cellType()) ? request.cellType().toUpperCase() : "STANDARD");
    box.setRowIndex(request.rowIndex());
    box.setColIndex(request.colIndex());
    return toSummary(boxRepository.save(box));
  }

  @Transactional
  public LockerResponse updateLocker(Long id, LockerRequest request) {
    LockerUnit locker =
        lockerRepository.findById(id).orElseThrow(() -> new NotFoundException("Locker", id));
    locker.setStoreId(request.storeId());
    locker.setCode(request.code());
    locker.setName(request.name());
    locker.setStatus(StringUtils.hasText(request.status()) ? request.status() : locker.getStatus());
    locker.setAddress(request.address());
    locker.setLatitude(request.latitude());
    locker.setLongitude(request.longitude());
    return toResponse(lockerRepository.save(locker));
  }

  @Transactional
  public void deleteLocker(Long id) {
    lockerRepository.delete(
        lockerRepository.findById(id).orElseThrow(() -> new NotFoundException("Locker", id)));
  }

  @Transactional
  public LockerResponse setMaintenance(Long id, boolean maintenance) {
    LockerUnit locker =
        lockerRepository.findById(id).orElseThrow(() -> new NotFoundException("Locker", id));
    locker.setStatus(maintenance ? "MAINTENANCE" : "ACTIVE");
    return toResponse(lockerRepository.save(locker));
  }

  @Transactional
  public LockerBoxSummary updateBoxStatus(Long boxId, String status) {
    LockerBox box = findBox(boxId);
    box.setStatus(status);
    return toSummary(boxRepository.save(box));
  }

  @Transactional
  public LockerBoxSummary openBox(Long boxId) {
    LockerBox box = findBox(boxId);
    publishBoxOpened(box);
    return toSummary(box);
  }

  @Transactional
  public LockerBoxSummary reserveBox(Long boxId, String channel) {
    LockerBox box = findBox(boxId);
    if (!"AVAILABLE".equalsIgnoreCase(box.getStatus())) {
      throw new com.huynqb.laundrylocker.common.exception.BusinessException("BOX_NOT_AVAILABLE", "Box is not available");
    }
    // Ô hàng 1 (DRONE) chỉ dành cho luồng drone thả hàng; mọi kênh khác bị chặn
    if ("DRONE".equalsIgnoreCase(box.getCellType()) && !"DRONE".equalsIgnoreCase(channel)) {
      throw new com.huynqb.laundrylocker.common.exception.BusinessException(
          "DRONE_CELL_RESTRICTED", "This cell is reserved for drone deliveries only");
    }
    box.setStatus("RESERVED");
    return toSummary(boxRepository.save(box));
  }

  @Transactional
  public LockerBoxSummary occupyBox(Long boxId) {
    LockerBox box = findBox(boxId);
    if (!"RESERVED".equalsIgnoreCase(box.getStatus()) && !"AVAILABLE".equalsIgnoreCase(box.getStatus())) {
      throw new com.huynqb.laundrylocker.common.exception.BusinessException(
          "BOX_NOT_RESERVED", "Box must be reserved before deposit");
    }
    box.setStatus("OCCUPIED");
    return toSummary(boxRepository.save(box));
  }

  @Transactional
  public LockerBoxSummary releaseBox(Long boxId) {
    LockerBox box = findBox(boxId);
    String status = box.getStatus();
    if ("FAULT".equalsIgnoreCase(status)
        || "OUT_OF_SERVICE".equalsIgnoreCase(status)
        || "CLEANING".equalsIgnoreCase(status)) {
      // Ô hỏng/ngưng dùng/đang vệ sinh phải được kỹ thuật khôi phục chủ động;
      // release thường (từ luồng đơn) không được tự đưa về AVAILABLE.
      return toSummary(box);
    }
    box.setStatus("AVAILABLE");
    return toSummary(boxRepository.save(box));
  }

  @Transactional
  public CellResponse markFault(Long boxId, String reason, Long userId) {
    LockerBox box = findBox(boxId);
    box.setStatus("FAULT");
    box.setFaultReason(reason);
    boxRepository.save(box);
    LockerReport report = new LockerReport();
    report.setLockerId(box.getLockerId());
    report.setBoxId(box.getId());
    report.setUserId(userId == null ? 0L : userId);
    report.setTitle("Box " + box.getBoxNumber() + " fault");
    report.setDescription(StringUtils.hasText(reason) ? reason : "Reported faulty");
    reportRepository.save(report);
    publishBoxFault(box, reason);
    return toCell(box);
  }

  @Transactional
  public CellResponse clearFault(Long boxId) {
    LockerBox box = findBox(boxId);
    box.setStatus("AVAILABLE");
    box.setFaultReason(null);
    return toCell(boxRepository.save(box));
  }

  /// Ngưng dùng ô có chủ đích (bảo trì/đóng). Ô bị loại khỏi mọi reserve vì
  /// reserveBox chỉ nhận từ AVAILABLE. Dùng faultReason làm ghi chú lý do.
  @Transactional
  public CellResponse setOutOfService(Long boxId, String reason) {
    return changeServiceState(boxId, "OUT_OF_SERVICE", reason, "ngưng dùng");
  }

  /// Đưa ô vào trạng thái đang vệ sinh/khử khuẩn (cũng bị loại khỏi reserve).
  @Transactional
  public CellResponse setCleaning(Long boxId) {
    return changeServiceState(boxId, "CLEANING", null, "vệ sinh");
  }

  /// Khôi phục ô từ OUT_OF_SERVICE/CLEANING về AVAILABLE. Ô đang FAULT phải
  /// dùng clear-fault, không dùng đường này.
  @Transactional
  public CellResponse returnToService(Long boxId) {
    LockerBox box = findBox(boxId);
    String status = box.getStatus();
    if ("FAULT".equalsIgnoreCase(status)) {
      throw new com.huynqb.laundrylocker.common.exception.BusinessException(
          "BOX_IN_FAULT", "Ô đang hỏng — dùng clear-fault để khôi phục");
    }
    if (!"OUT_OF_SERVICE".equalsIgnoreCase(status) && !"CLEANING".equalsIgnoreCase(status)) {
      throw new com.huynqb.laundrylocker.common.exception.BusinessException(
          "BOX_NOT_OUT_OF_SERVICE", "Ô không ở trạng thái ngưng dùng/vệ sinh");
    }
    box.setStatus("AVAILABLE");
    box.setFaultReason(null);
    return toCell(boxRepository.save(box));
  }

  private CellResponse changeServiceState(Long boxId, String target, String reason, String action) {
    LockerBox box = findBox(boxId);
    String status = box.getStatus();
    if ("OCCUPIED".equalsIgnoreCase(status) || "RESERVED".equalsIgnoreCase(status)) {
      throw new com.huynqb.laundrylocker.common.exception.BusinessException(
          "BOX_IN_USE", "Ô đang có đơn — không thể " + action);
    }
    box.setStatus(target);
    box.setFaultReason(reason);
    return toCell(boxRepository.save(box));
  }

  @Transactional(readOnly = true)
  public LockerLayoutResponse layout(Long lockerId) {
    LockerUnit locker =
        lockerRepository.findById(lockerId).orElseThrow(() -> new NotFoundException("Locker", lockerId));
    List<LockerBox> boxes = boxRepository.findByLockerIdOrderByRowIndexAscColIndexAsc(lockerId);
    List<CellResponse> cells = boxes.stream().map(this::toCell).toList();
    long available = boxes.stream().filter(b -> "AVAILABLE".equalsIgnoreCase(b.getStatus())).count();
    long fault = boxes.stream().filter(b -> "FAULT".equalsIgnoreCase(b.getStatus())).count();
    return new LockerLayoutResponse(
        locker.getId(), locker.getCode(), locker.getName(), locker.getStatus(),
        locker.getLandingPad(), locker.getLandingMarkerId(),
        cells.size(), available, fault, cells);
  }

  @Transactional(readOnly = true)
  public CellResponse findAvailableBox(Long lockerId, String size, String cellType) {
    String type = StringUtils.hasText(cellType) ? cellType.toUpperCase() : "STANDARD";
    java.util.Optional<LockerBox> found =
        StringUtils.hasText(size)
            ? boxRepository.findFirstByLockerIdAndStatusAndCellTypeAndSizeAndActiveTrueOrderByBoxNumberAsc(
                lockerId, "AVAILABLE", type, size.toUpperCase())
            : boxRepository.findFirstByLockerIdAndStatusAndCellTypeAndActiveTrueOrderByBoxNumberAsc(
                lockerId, "AVAILABLE", type);
    return found
        .map(this::toCell)
        .orElseThrow(
            () ->
                new com.huynqb.laundrylocker.common.exception.BusinessException(
                    "NO_AVAILABLE_BOX", "No available box matching criteria"));
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

  @Transactional(readOnly = true)
  public List<LockerReportResponse> allReports() {
    return reportRepository.findAll().stream().map(this::toReport).toList();
  }

  @Transactional(readOnly = true)
  public List<LockerStatsResponse> stats(Long storeId) {
    return lockerRepository.findAll().stream()
        .filter(locker -> storeId == null || storeId.equals(locker.getStoreId()))
        .map(this::toStats)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<FaultCellResponse> openFaults() {
    return boxRepository.findByStatusAndActiveTrueOrderByLockerIdAscBoxNumberAsc("FAULT").stream()
        .map(box -> {
          LockerUnit locker = lockerRepository.findById(box.getLockerId()).orElse(null);
          Long reportId =
              reportRepository
                  .findFirstByBoxIdAndStatusInOrderByCreatedAtDesc(box.getId(), OPEN_REPORT_STATUSES)
                  .map(LockerReport::getId)
                  .orElse(null);
          return new FaultCellResponse(
              box.getLockerId(),
              locker == null ? null : locker.getCode(),
              locker == null ? null : locker.getName(),
              locker == null ? null : locker.getAddress(),
              locker == null ? null : locker.getLatitude(),
              locker == null ? null : locker.getLongitude(),
              box.getId(),
              box.getBoxNumber(),
              box.getCellType(),
              box.getRowIndex(),
              box.getColIndex(),
              box.getFaultReason(),
              reportId);
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public List<LockerReportResponse> openReports() {
    return reportRepository.findByStatusInOrderByCreatedAtDesc(OPEN_REPORT_STATUSES).stream()
        .map(this::toReport)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<LockerReportResponse> assignedReports(Long userId) {
    return reportRepository.findByAssignedToUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toReport)
        .toList();
  }

  @Transactional
  public LockerReportResponse claimReport(Long reportId, Long userId) {
    LockerReport report =
        reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException("LockerReport", reportId));
    if (!"OPEN".equals(report.getStatus())) {
      throw new com.huynqb.laundrylocker.common.exception.BusinessException(
          "REPORT_NOT_CLAIMABLE", "Report is not open for claiming");
    }
    report.setStatus("IN_PROGRESS");
    report.setAssignedToUserId(userId);
    report.setAssignedAt(java.time.LocalDateTime.now());
    return toReport(reportRepository.save(report));
  }

  // Resolving a report tied to a faulty cell also returns the cell to service —
  // the technician confirms the physical repair in one step.
  @Transactional
  public LockerReportResponse resolveReportAndClearFault(Long reportId, Long userId) {
    LockerReport report =
        reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException("LockerReport", reportId));
    report.setStatus("RESOLVED");
    report.setResolvedByUserId(userId);
    report.setResolvedAt(java.time.LocalDateTime.now());
    if (report.getBoxId() != null) {
      boxRepository.findById(report.getBoxId())
          .filter(box -> "FAULT".equals(box.getStatus()))
          .ifPresent(box -> {
            box.setStatus("AVAILABLE");
            box.setFaultReason(null);
            boxRepository.save(box);
          });
    }
    return toReport(reportRepository.save(report));
  }

  /// L5: kỹ thuật viên thêm 1 dòng nhật ký xử lý vào phiếu bảo trì.
  @Transactional
  public RepairLogResponse addRepairLog(Long reportId, String note, Long actorUserId) {
    LockerReport report =
        reportRepository.findById(reportId).orElseThrow(() -> new NotFoundException("Report", reportId));
    RepairLog log = new RepairLog();
    log.setReportId(report.getId());
    log.setActorUserId(actorUserId);
    log.setNote(note);
    return toRepairLog(repairLogRepository.save(log));
  }

  @Transactional(readOnly = true)
  public List<RepairLogResponse> repairLogs(Long reportId) {
    return repairLogRepository.findByReportIdOrderByCreatedAtAsc(reportId).stream()
        .map(this::toRepairLog)
        .toList();
  }

  private RepairLogResponse toRepairLog(RepairLog log) {
    return new RepairLogResponse(
        log.getId(), log.getReportId(), log.getActorUserId(), log.getNote(), log.getCreatedAt());
  }

  private LockerStatsResponse toStats(LockerUnit locker) {
    List<LockerBox> boxes = boxRepository.findByLockerId(locker.getId());
    int total = boxes.size();
    int available = (int) boxes.stream().filter(b -> "AVAILABLE".equals(b.getStatus())).count();
    int reserved = (int) boxes.stream().filter(b -> "RESERVED".equals(b.getStatus())).count();
    int occupied = (int) boxes.stream().filter(b -> "OCCUPIED".equals(b.getStatus())).count();
    int fault = (int) boxes.stream().filter(b -> "FAULT".equals(b.getStatus())).count();
    double utilization = total == 0 ? 0.0 : Math.round((reserved + occupied) * 1000.0 / total) / 10.0;
    long openReports = reportRepository.countByLockerIdAndStatusIn(locker.getId(), OPEN_REPORT_STATUSES);
    return new LockerStatsResponse(
        locker.getId(), locker.getCode(), locker.getName(), locker.getStatus(), locker.getLandingPad(),
        total, available, reserved, occupied, fault, utilization, openReports);
  }

  @Transactional(readOnly = true)
  public CellResponse getCell(Long boxId) {
    return toCell(findBox(boxId));
  }

  private LockerBox findBox(Long id) {
    return boxRepository.findById(id).orElseThrow(() -> new NotFoundException("Box", id));
  }

  private LockerResponse toResponse(LockerUnit locker) {
    return new LockerResponse(
        locker.getId(), locker.getStoreId(), locker.getCode(), locker.getName(), locker.getStatus(),
        locker.getAddress(), locker.getLatitude(), locker.getLongitude(),
        locker.getLandingPad(), locker.getLandingMarkerId());
  }

  private LockerBoxSummary toSummary(LockerBox box) {
    return new LockerBoxSummary(box.getLockerId(), box.getId(), null, box.getBoxNumber(), box.getStatus());
  }

  private CellResponse toCell(LockerBox box) {
    return new CellResponse(
        box.getId(),
        box.getBoxNumber(),
        box.getSize(),
        box.getCellType(),
        box.getRowIndex(),
        box.getColIndex(),
        box.getStatus(),
        box.getFaultReason());
  }

  private void publishBoxFault(LockerBox box, String reason) {
    try {
      rabbitTemplate.convertAndSend(
          DomainEventNames.EXCHANGE,
          DomainEventNames.LOCKER_BOX_FAULT,
          DomainEvent.of(
              DomainEventNames.LOCKER_BOX_FAULT,
              "locker-service",
              Map.of(
                  "lockerId", box.getLockerId(),
                  "boxId", box.getId(),
                  "boxNumber", box.getBoxNumber(),
                  "reason", reason == null ? "" : reason)));
    } catch (AmqpException ex) {
      log.warn("Could not publish locker.box.fault for box {}: {}", box.getId(), ex.getMessage());
    }
  }

  private LockerReportResponse toReport(LockerReport report) {
    LockerUnit locker = lockerRepository.findById(report.getLockerId()).orElse(null);
    LockerBox box = report.getBoxId() == null ? null : boxRepository.findById(report.getBoxId()).orElse(null);
    LocalDateTime slaDueAt =
        report.getCreatedAt() == null ? null : report.getCreatedAt().plusHours(slaHours);
    boolean overdue =
        slaDueAt != null
            && !"RESOLVED".equalsIgnoreCase(report.getStatus())
            && LocalDateTime.now().isAfter(slaDueAt);
    return new LockerReportResponse(
        report.getId(),
        report.getLockerId(),
        report.getBoxId(),
        report.getUserId(),
        report.getTitle(),
        report.getDescription(),
        report.getStatus(),
        report.getAssignedToUserId(),
        report.getAssignedAt(),
        report.getResolvedByUserId(),
        report.getResolvedAt(),
        report.getCreatedAt(),
        locker == null ? null : locker.getCode(),
        locker == null ? null : locker.getName(),
        locker == null ? null : locker.getAddress(),
        locker == null ? null : locker.getLatitude(),
        locker == null ? null : locker.getLongitude(),
        box == null ? null : box.getBoxNumber(),
        box == null ? null : box.getCellType(),
        slaHours,
        slaDueAt,
        overdue);
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
