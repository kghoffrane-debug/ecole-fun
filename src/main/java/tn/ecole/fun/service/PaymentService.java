package tn.ecole.fun.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Payment;
import tn.ecole.fun.model.Student;
import tn.ecole.fun.repo.PaymentRepository;
import tn.ecole.fun.repo.StudentRepository;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PaymentService {

    @Getter
    @AllArgsConstructor
    public static class Debt {
        private Student student;
        private BigDecimal due;
        private BigDecimal paid;
        private BigDecimal balance;
    }

    private final PaymentRepository payments;
    private final StudentRepository students;
    private final MessageService messages;
    private final AuditService audit;

    @Value("${app.annee-scolaire}")
    private String annee;

    public PaymentService(PaymentRepository payments, StudentRepository students,
                          MessageService messages, AuditService audit) {
        this.payments = payments;
        this.students = students;
        this.messages = messages;
        this.audit = audit;
    }

    public BigDecimal paid(Student s) {
        return payments.findByStudentAndDeletedFalseOrderByPaidAtDescIdDesc(s).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal balance(Student s) {
        BigDecimal due = s.getTuitionDue() == null ? BigDecimal.ZERO : s.getTuitionDue();
        return due.subtract(paid(s));
    }

    /** Élèves ayant un solde restant à payer. */
    public List<Debt> debts() {
        return students.findByDeletedFalseOrderByLastNameAscFirstNameAsc().stream()
                .map(s -> {
                    BigDecimal paid = paid(s);
                    BigDecimal due = s.getTuitionDue() == null ? BigDecimal.ZERO : s.getTuitionDue();
                    return new Debt(s, due, paid, due.subtract(paid));
                })
                .filter(d -> d.getBalance().signum() > 0)
                .toList();
    }

    /**
     * Enregistre un paiement, numérote le reçu, puis envoie automatiquement
     * un message au parent (montant, date, mode, nouveau solde, lien du reçu).
     */
    @Transactional
    public Payment register(Payment p, AppUser sender) {
        p.setAnneeScolaire(annee);
        p = payments.save(p);
        p.setReceiptNumber(String.format("R-%d-%05d", p.getPaidAt().getYear(), p.getId()));
        p = payments.save(p);
        audit.log("CREATE", "Payment", p.getId(), "Paiement " + p.getAmount() + " DT - " + p.getStudent().getFullName());

        Student s = p.getStudent();
        if (s.getParent() != null) {
            BigDecimal newBalance = balance(s);
            String body = "Bonjour " + s.getParent().getFullName() + ",\n\n"
                    + "Nous avons bien reçu un paiement pour " + s.getFullName() + " :\n"
                    + "• Montant : " + p.getAmount() + " DT\n"
                    + "• Date : " + p.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n"
                    + "• Mode : " + p.getMethod().getLabel() + "\n"
                    + "• Nouveau solde restant : " + newBalance + " DT\n\n"
                    + "Merci pour votre confiance ! 🌟";
            messages.sendSystem(sender, s.getParent(),
                    "💰 Paiement reçu pour " + s.getFirstName(), body,
                    "/payments/" + p.getId() + "/receipt", "🧾 Ouvrir le reçu " + p.getReceiptNumber());
            audit.log("NOTIFY", "Message", null, "Message de paiement envoyé à " + s.getParent().getUsername());
        }
        return p;
    }
}
