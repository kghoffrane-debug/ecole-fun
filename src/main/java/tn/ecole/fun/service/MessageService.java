package tn.ecole.fun.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Message;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.repo.AppUserRepository;
import tn.ecole.fun.repo.MessageRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageService {
    private final MessageRepository repo;
    private final AppUserRepository users;

    public MessageService(MessageRepository repo, AppUserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    /** Contrôle des droits (RBAC) : à qui chaque rôle peut-il écrire ? */
    public List<AppUser> recipientsFor(AppUser sender) {
        List<AppUser> all = users.findByDeletedFalseOrderByFullNameAsc();
        return all.stream()
                .filter(u -> !u.getId().equals(sender.getId()))
                .filter(u -> allowed(sender.getRole(), u.getRole()))
                .toList();
    }

    private boolean allowed(Role from, Role to) {
        return switch (from) {
            case ADMIN -> true;
            case TEACHER, ACCOUNTANT -> to == Role.PARENT || to == Role.ADMIN;
            case PARENT -> to == Role.TEACHER || to == Role.ADMIN || to == Role.ACCOUNTANT;
            case STUDENT -> to == Role.ADMIN;
        };
    }

    @Transactional
    public int send(AppUser sender, String to, String subject, String body, boolean draft, Long draftId) {
        if (subject == null || subject.isBlank() || body == null || body.isBlank()) {
            throw new IllegalArgumentException("L'objet et le message sont obligatoires.");
        }
        List<AppUser> targets;
        if ("all-parents".equals(to)) {
            if (sender.getRole() != Role.ADMIN || draft || draftId != null) {
                throw new IllegalArgumentException("Envoi groupé réservé à la direction (sans brouillon).");
            }
            targets = users.findByRoleAndDeletedFalseOrderByFullNameAsc(Role.PARENT);
        } else {
            AppUser r = recipientsFor(sender).stream()
                    .filter(u -> String.valueOf(u.getId()).equals(to))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Destinataire non autorisé pour votre rôle."));
            targets = List.of(r);
        }

        if (draftId != null) {
            Message d = repo.findById(draftId).orElseThrow();
            if (!d.isDraft() || !d.getSender().getId().equals(sender.getId())) {
                throw new IllegalArgumentException("Ce message est scellé : il ne peut plus être modifié.");
            }
            d.setRecipient(targets.get(0));
            d.setSubject(subject);
            d.setBody(body);
            if (!draft) {
                d.setDraft(false);
                d.setSentAt(LocalDateTime.now());
            }
            repo.save(d);
            return 1;
        }

        List<Message> toSave = new ArrayList<>();
        for (AppUser r : targets) {
            Message m = new Message();
            m.setSender(sender);
            m.setRecipient(r);
            m.setSubject(subject);
            m.setBody(body);
            m.setDraft(draft);
            m.setSentAt(draft ? null : LocalDateTime.now());
            toSave.add(m);
        }
        repo.saveAll(toSave);
        return toSave.size();
    }

    /** Message automatique (reçu de paiement, relance...). */
    public void sendSystem(AppUser sender, AppUser recipient, String subject, String body,
                           String linkUrl, String linkLabel) {
        Message m = new Message();
        m.setSender(sender);
        m.setRecipient(recipient);
        m.setSubject(subject);
        m.setBody(body);
        m.setLinkUrl(linkUrl);
        m.setLinkLabel(linkLabel);
        m.setSentAt(LocalDateTime.now());
        repo.save(m);
    }
}
