package tn.ecole.fun.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.ecole.fun.model.AuditLog;
import tn.ecole.fun.repo.AuditLogRepository;

import java.time.LocalDateTime;

/** Journalisation des créations / modifications / suppressions. */
@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(String action, String entity, Long entityId, String details) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        AuditLog l = new AuditLog();
        l.setUsername(a == null ? "system" : a.getName());
        l.setAction(action);
        l.setEntity(entity);
        l.setEntityId(entityId);
        l.setDetails(details != null && details.length() > 990 ? details.substring(0, 990) : details);
        l.setHappenedAt(LocalDateTime.now());
        repo.save(l);
    }
}
