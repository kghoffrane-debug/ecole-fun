package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
