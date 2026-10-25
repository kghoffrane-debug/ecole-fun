package tn.ecole.fun.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.repo.AppUserRepository;
import tn.ecole.fun.repo.MessageRepository;

/** Données communes à toutes les pages : utilisateur connecté, messages non lus, année scolaire. */
@ControllerAdvice
public class GlobalModel {
    private final AppUserRepository users;
    private final MessageRepository messages;

    @Value("${app.annee-scolaire}")
    private String annee;

    public GlobalModel(AppUserRepository users, MessageRepository messages) {
        this.users = users;
        this.messages = messages;
    }

    @ModelAttribute
    public void common(Model model, Authentication auth) {
        model.addAttribute("annee", annee);
        if (auth == null) return;
        users.findByUsernameAndDeletedFalse(auth.getName()).ifPresent((AppUser u) -> {
            model.addAttribute("me", u);
            model.addAttribute("unread", messages
                    .countByRecipientAndDraftFalseAndHiddenByRecipientFalseAndReadByRecipientFalse(u));
        });
    }
}
