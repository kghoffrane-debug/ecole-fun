package tn.ecole.fun.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Message;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.repo.MessageRepository;
import tn.ecole.fun.service.AuditService;
import tn.ecole.fun.service.CurrentUserService;
import tn.ecole.fun.service.MessageService;

/**
 * Messagerie : les messages envoyés sont immuables (ni modification ni suppression côté expéditeur).
 * Les brouillons sont libres. Le destinataire peut seulement masquer un message de sa boîte.
 */
@Controller
@RequestMapping("/messages")
public class MessageController {
    private final MessageRepository repo;
    private final MessageService service;
    private final CurrentUserService cu;
    private final AuditService audit;

    public MessageController(MessageRepository repo, MessageService service,
                             CurrentUserService cu, AuditService audit) {
        this.repo = repo;
        this.service = service;
        this.cu = cu;
        this.audit = audit;
    }

    @GetMapping
    public String inbox(Model m) {
        AppUser me = cu.get();
        m.addAttribute("tab", "inbox");
        m.addAttribute("messages", repo.findByRecipientAndDraftFalseAndHiddenByRecipientFalseOrderBySentAtDesc(me));
        return "messages/box";
    }

    @GetMapping("/sent")
    public String sent(Model m) {
        m.addAttribute("tab", "sent");
        m.addAttribute("messages", repo.findBySenderAndDraftFalseOrderBySentAtDesc(cu.get()));
        return "messages/box";
    }

    @GetMapping("/drafts")
    public String drafts(Model m) {
        m.addAttribute("tab", "drafts");
        m.addAttribute("messages", repo.findBySenderAndDraftTrueOrderByIdDesc(cu.get()));
        return "messages/box";
    }

    @GetMapping("/compose")
    public String compose(@RequestParam(required = false) Long to,
                          @RequestParam(required = false) String subject, Model m) {
        AppUser me = cu.get();
        Message draft = new Message();
        if (subject != null) draft.setSubject(subject);
        m.addAttribute("draft", draft);
        m.addAttribute("preselect", to == null ? "" : String.valueOf(to));
        m.addAttribute("recipients", service.recipientsFor(me));
        m.addAttribute("canBroadcast", me.getRole() == Role.ADMIN);
        return "messages/compose";
    }

    @GetMapping("/{id}/edit")
    public String editDraft(@PathVariable Long id, Model m) {
        AppUser me = cu.get();
        Message d = repo.findById(id).orElseThrow();
        if (!d.isDraft() || !d.getSender().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        m.addAttribute("draft", d);
        m.addAttribute("preselect", String.valueOf(d.getRecipient().getId()));
        m.addAttribute("recipients", service.recipientsFor(me));
        m.addAttribute("canBroadcast", false);
        return "messages/compose";
    }

    @PostMapping("/send")
    public String send(@RequestParam(required = false) Long draftId,
                       @RequestParam String to,
                       @RequestParam String subject,
                       @RequestParam String body,
                       @RequestParam(defaultValue = "send") String action,
                       RedirectAttributes ra) {
        boolean draft = "draft".equals(action);
        try {
            int n = service.send(cu.get(), to, subject.trim(), body.trim(), draft, draftId);
            audit.log(draft ? "DRAFT" : "SEND", "Message", draftId, n + " destinataire(s) - " + subject);
            ra.addFlashAttribute("ok", draft ? "Brouillon enregistré 📝" : "Message envoyé à " + n + " personne(s) ! 🚀");
            return draft ? "redirect:/messages/drafts" : "redirect:/messages/sent";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("err", e.getMessage());
            return draftId == null ? "redirect:/messages/compose" : "redirect:/messages/" + draftId + "/edit";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model m) {
        AppUser me = cu.get();
        Message msg = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean isSender = msg.getSender().getId().equals(me.getId());
        boolean isRecipient = msg.getRecipient().getId().equals(me.getId());
        if (msg.isDraft() && isSender) return "redirect:/messages/" + id + "/edit";
        if (msg.isDraft() || !(isSender || (isRecipient && !msg.isHiddenByRecipient()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (isRecipient && !msg.isReadByRecipient()) {
            msg.setReadByRecipient(true);
            repo.save(msg);
        }
        // Journalisation des accès à l'historique (qui a consulté quoi, quand)
        audit.log("READ", "Message", msg.getId(), "Consulté par " + me.getUsername());
        m.addAttribute("msg", msg);
        m.addAttribute("isRecipient", isRecipient);
        return "messages/view";
    }

    @PostMapping("/{id}/hide")
    public String hide(@PathVariable Long id, RedirectAttributes ra) {
        AppUser me = cu.get();
        Message msg = repo.findById(id).orElseThrow();
        if (!msg.getRecipient().getId().equals(me.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        msg.setHiddenByRecipient(true);
        repo.save(msg);
        ra.addFlashAttribute("ok", "Message masqué de votre boîte (l'expéditeur garde sa copie).");
        return "redirect:/messages";
    }

    @PostMapping("/{id}/delete-draft")
    public String deleteDraft(@PathVariable Long id, RedirectAttributes ra) {
        AppUser me = cu.get();
        Message d = repo.findById(id).orElseThrow();
        if (!d.isDraft() || !d.getSender().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        repo.delete(d);
        ra.addFlashAttribute("ok", "Brouillon supprimé.");
        return "redirect:/messages/drafts";
    }
}
