package tn.ecole.fun.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.Student;
import tn.ecole.fun.repo.SchoolClassRepository;
import tn.ecole.fun.repo.StudentRepository;
import tn.ecole.fun.service.CurrentUserService;
import tn.ecole.fun.service.GradeService;
import tn.ecole.fun.service.PaymentService;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class HomeController {

    @Getter
    @AllArgsConstructor
    public static class ChildCard {
        private Student student;
        private GradeService.Progress progress;
        private BigDecimal balance;
    }

    private final CurrentUserService cu;
    private final StudentRepository students;
    private final SchoolClassRepository classes;
    private final GradeService gradeService;
    private final PaymentService paymentService;

    @Value("${app.seed-demo:true}")
    private boolean demo;

    public HomeController(CurrentUserService cu, StudentRepository students, SchoolClassRepository classes,
                          GradeService gradeService, PaymentService paymentService) {
        this.cu = cu;
        this.students = students;
        this.classes = classes;
        this.gradeService = gradeService;
        this.paymentService = paymentService;
    }

    @GetMapping("/login")
    public String login(Model m) {
        m.addAttribute("demo", demo);
        return "login";
    }

    @GetMapping("/")
    public String home(Model m) {
        AppUser me = cu.get();
        if (me.getRole() == Role.PARENT || me.getRole() == Role.STUDENT) {
            List<ChildCard> cards = cu.visibleStudents().stream()
                    .map(s -> new ChildCard(s, gradeService.progress(s), paymentService.balance(s)))
                    .toList();
            m.addAttribute("cards", cards);
            return "dashboard-family";
        }
        m.addAttribute("studentCount", students.findByDeletedFalseOrderByLastNameAscFirstNameAsc().size());
        m.addAttribute("classCount", classes.findByDeletedFalseOrderByLevelAscSectionAsc().size());
        BigDecimal unpaid = paymentService.debts().stream()
                .map(PaymentService.Debt::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        m.addAttribute("unpaidTotal", unpaid);
        return "dashboard";
    }
}
