package tn.ecole.fun.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.AuditLog;
import tn.ecole.fun.repo.AppUserRepository;
import tn.ecole.fun.repo.AuditLogRepository;

import java.time.LocalDateTime;

/** Journalisation des actions. Chaque ligne porte l'école de l'utilisateur (null pour le propriétaire du SaaS). */
@Service
public class AuditService {
    private final AuditLogRepository repo;
    private final AppUserRepository users;

    public AuditService(AuditLogRepository repo, AppUserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    public void log(String action, String entity, Long entityId, String details) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        AppUser u = a == null ? null : users.findByUsernameAndDeletedFalse(a.getName()).orElse(null);
        AuditLog l = new AuditLog();
        l.setUsername(a == null ? "system" : a.getName());
        l.setSchool(u == null ? null : u.getSchool());
        l.setAction(action);
        l.setEntity(entity);
        l.setEntityId(entityId);
        l.setDetails(details != null && details.length() > 990 ? details.substring(0, 990) : details);
        l.setHappenedAt(LocalDateTime.now());
        repo.save(l);
    }
}
