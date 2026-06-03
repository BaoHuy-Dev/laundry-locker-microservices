package com.huynqb.laundrylocker.user.repository;

import com.huynqb.laundrylocker.user.model.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}
