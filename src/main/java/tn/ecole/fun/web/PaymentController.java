package tn.ecole.fun.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.ecole.fun.model.PayMethod;
import tn.ecole.fun.model.Payment;
import tn.ecole.fun.model.Student;
import tn.ecole.fun.repo.PaymentRepository;
import tn.ecole.fun.repo.StudentRepository;
import tn.ecole.fun.service.AuditService;
import tn.ecole.fun.service.CurrentUserService;
import tn.ecole.fun.service.MessageService;
import tn.ecole.fun.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentRepository payments;
    private final StudentRepository students;
    private final PaymentService paymentService;
    private final MessageService messages;
    private final CurrentUserService cu;
    private final AuditService audit;

    public PaymentController(PaymentRepository payments, StudentRepository students, PaymentService paymentService,
                             MessageService messages, CurrentUserService cu, AuditService audit) {
        this.payments = payments;
        this.students = students;
        this.paymentService = paymentService;
        this.messages = messages;
        this.cu = cu;
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public String list(Model m) {
        m.addAttribute("payments", payments.findByDeletedFalseOrderByPaidAtDescIdDesc());
        m.addAttribute("debts", paymentService.debts());
        return "payments/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public String create(@RequestParam(required = false) Long studentId, Model m) {
        Payment p = new Payment();
        p.setPaidAt(LocalDate.now());
        p.setMethod(PayMethod.CASH);
        if (studentId != null) p.setStudent(students.findById(studentId).orElse(null));
        return form(m, p);
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public String edit(@PathVariable Long id, Model m) {
        return form(m, payments.findById(id).orElseThrow());
    }

    private String form(Model m, Payment p) {
        m.addAttribute("p", p);
        m.addAttribute("students", students.findByDeletedFalseOrderByLastNameAscFirstNameAsc());
        m.addAttribute("methods", PayMethod.values());
        return "payments/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long studentId,
                       @RequestParam BigDecimal amount,
                       @RequestParam PayMethod method,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidAt,
                       RedirectAttributes ra) {
        if (amount.signum() <= 0) {
            ra.addFlashAttribute("err", "Le montant doit être supérieur à 0.");
            return id == null ? "redirect:/payments/new" : "redirect:/payments/" + id + "/edit";
        }
        Student s = students.findById(studentId).orElseThrow();
        if (id == null) {
            Payment p = new Payment();
            p.setStudent(s);
            p.setAmount(amount);
            p.setMethod(method);
            p.setPaidAt(paidAt);
            p = paymentService.register(p, cu.get());
            ra.addFlashAttribute("ok", "Paiement enregistré, reçu " + p.getReceiptNumber()
                    + " créé et parent prévenu ! 💰🎉");
        } else {
            Payment p = payments.findById(id).orElseThrow();
            String before = p.getAmount() + " DT / " + p.getMethod() + " / " + p.getPaidAt();
            p.setStudent(s);
            p.setAmount(amount);
            p.setMethod(method);
            p.setPaidAt(paidAt);
            payments.save(p);
            audit.log("UPDATE", "Payment", p.getId(), before + " → " + amount + " DT / " + method + " / " + paidAt);
            ra.addFlashAttribute("ok", "Paiement modifié.");
        }
        return "redirect:/payments";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Payment p = payments.findById(id).orElseThrow();
        p.setDeleted(true);
        payments.save(p);
        audit.log("DELETE", "Payment", p.getId(), p.getAmount() + " DT - " + p.getStudent().getFullName());
        ra.addFlashAttribute("ok", "Paiement supprimé (annulé) : le solde est recalculé.");
        return "redirect:/payments";
    }

    @GetMapping("/{id}/receipt")
    public String receipt(@PathVariable Long id, Model m) {
        Payment p = payments.findById(id).filter(x -> !x.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!cu.canSee(p.getStudent())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        m.addAttribute("p", p);
        m.addAttribute("balance", paymentService.balance(p.getStudent()));
        return "payments/receipt";
    }

    @PostMapping("/remind/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public String remind(@PathVariable Long studentId, RedirectAttributes ra) {
        Student s = students.findById(studentId).orElseThrow();
        if (s.getParent() == null) {
            ra.addFlashAttribute("err", "Aucun parent rattaché à " + s.getFirstName() + ".");
            return "redirect:/payments";
        }
        BigDecimal balance = paymentService.balance(s);
        messages.sendSystem(cu.get(), s.getParent(), "⏰ Rappel de paiement pour " + s.getFirstName(),
                "Bonjour " + s.getParent().getFullName() + ",\n\nUn solde de " + balance
                        + " DT reste à régler pour " + s.getFullName() + ".\n"
                        + "Merci de passer au secrétariat. À très bientôt ! 🌈", null, null);
        audit.log("NOTIFY", "Student", s.getId(), "Relance de paiement envoyée");
        ra.addFlashAttribute("ok", "Relance envoyée à " + s.getParent().getFullName() + " 💌");
        return "redirect:/payments";
    }
}
