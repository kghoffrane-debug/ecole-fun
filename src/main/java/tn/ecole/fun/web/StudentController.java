package tn.ecole.fun.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.ecole.fun.model.*;
import tn.ecole.fun.repo.*;
import tn.ecole.fun.service.AuditService;
import tn.ecole.fun.service.CurrentUserService;
import tn.ecole.fun.service.GradeService;
import tn.ecole.fun.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/students")
public class StudentController {
    private static final List<String> AVATARS =
            List.of("🦊", "🐼", "🐯", "🦁", "🐸", "🐵", "🐰", "🐻", "🦄", "🐙", "🐨", "🐧");

    private final StudentRepository students;
    private final SchoolClassRepository classes;
    private final AppUserRepository users;
    private final PaymentRepository payments;
    private final GradeService gradeService;
    private final PaymentService paymentService;
    private final CurrentUserService cu;
    private final AuditService audit;

    @Value("${app.annee-scolaire}")
    private String annee;
    @Value("${app.default-tuition:1200}")
    private String defaultTuition;

    public StudentController(StudentRepository students, SchoolClassRepository classes, AppUserRepository users,
                             PaymentRepository payments, GradeService gradeService, PaymentService paymentService,
                             CurrentUserService cu, AuditService audit) {
        this.students = students;
        this.classes = classes;
        this.users = users;
        this.payments = payments;
        this.gradeService = gradeService;
        this.paymentService = paymentService;
        this.cu = cu;
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','ACCOUNTANT')")
    public String list(@RequestParam(required = false) String q, Model m) {
        List<Student> all = students.findByDeletedFalseOrderByLastNameAscFirstNameAsc();
        if (q != null && !q.isBlank()) {
            String needle = q.toLowerCase().trim();
            all = all.stream().filter(s -> s.getFullName().toLowerCase().contains(needle)).toList();
        }
        m.addAttribute("students", all);
        m.addAttribute("q", q);
        return "students/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public String create(Model m) {
        Student s = new Student();
        s.setTuitionDue(new BigDecimal(defaultTuition));
        s.setAnneeScolaire(annee);
        return form(m, s);
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public String edit(@PathVariable Long id, Model m) {
        return form(m, students.findById(id).orElseThrow());
    }

    private String form(Model m, Student s) {
        m.addAttribute("s", s);
        m.addAttribute("avatars", AVATARS);
        m.addAttribute("classes", classes.findByDeletedFalseOrderByLevelAscSectionAsc());
        m.addAttribute("parents", users.findByRoleAndDeletedFalseOrderByFullNameAsc(Role.PARENT));
        m.addAttribute("pupils", users.findByRoleAndDeletedFalseOrderByFullNameAsc(Role.STUDENT));
        return "students/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String firstName,
                       @RequestParam String lastName,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
                       @RequestParam(defaultValue = "🦊") String avatar,
                       @RequestParam(required = false) Long classId,
                       @RequestParam(required = false) Long parentId,
                       @RequestParam(required = false) Long userId,
                       @RequestParam(defaultValue = "0") BigDecimal tuitionDue,
                       @RequestParam(required = false) String medicalNotes,
                       RedirectAttributes ra) {
        Student s = id == null ? new Student() : students.findById(id).orElseThrow();
        SchoolClass c = classId == null ? null : classes.findById(classId).orElse(null);

        boolean changesClass = c != null
                && (s.getSchoolClass() == null || !s.getSchoolClass().getId().equals(c.getId()));
        if (changesClass && students.countBySchoolClassAndDeletedFalse(c) >= c.getCapacity()) {
            ra.addFlashAttribute("err", "La classe " + c.getLabel() + " est complète (" + c.getCapacity() + " élèves).");
            return id == null ? "redirect:/students/new" : "redirect:/students/" + id + "/edit";
        }

        s.setFirstName(firstName.trim());
        s.setLastName(lastName.trim());
        s.setBirthDate(birthDate);
        s.setAvatar(avatar);
        s.setSchoolClass(c);
        s.setParent(parentId == null ? null : users.findById(parentId).orElse(null));
        s.setUser(userId == null ? null : users.findById(userId).orElse(null));
        s.setTuitionDue(tuitionDue);
        s.setMedicalNotes(medicalNotes);
        if (s.getAnneeScolaire() == null) s.setAnneeScolaire(annee);
        s = students.save(s);
        audit.log(id == null ? "CREATE" : "UPDATE", "Student", s.getId(), s.getFullName());
        ra.addFlashAttribute("ok", "Dossier élève enregistré ! 🎉");
        return "redirect:/students";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Student s = students.findById(id).orElseThrow();
        s.setDeleted(true);
        students.save(s);
        audit.log("DELETE", "Student", s.getId(), s.getFullName());
        ra.addFlashAttribute("ok", "Dossier archivé (suppression logique, l'historique est conservé).");
        return "redirect:/students";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model m) {
        Student s = accessible(id);
        m.addAttribute("s", s);
        m.addAttribute("progress", gradeService.progress(s));
        m.addAttribute("balance", paymentService.balance(s));
        m.addAttribute("payments", payments.findByStudentAndDeletedFalseOrderByPaidAtDescIdDesc(s));
        return "students/detail";
    }

    @GetMapping("/{id}/bulletin")
    public String bulletin(@PathVariable Long id, Model m) {
        Student s = accessible(id);
        m.addAttribute("s", s);
        m.addAttribute("progress", gradeService.progress(s));
        return "students/bulletin";
    }

    private Student accessible(Long id) {
        Student s = students.findById(id).filter(x -> !x.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!cu.canSee(s)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return s;
    }
}
