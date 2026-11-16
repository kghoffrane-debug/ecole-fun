package tn.ecole.fun.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Assessment;
import tn.ecole.fun.model.Homework;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.School;
import tn.ecole.fun.model.Student;
import tn.ecole.fun.repo.SchoolClassRepository;
import tn.ecole.fun.repo.StudentRepository;
import tn.ecole.fun.service.AssessmentService;
import tn.ecole.fun.service.CurrentUserService;
import tn.ecole.fun.service.GradeService;
import tn.ecole.fun.service.HomeworkService;
import tn.ecole.fun.service.PaymentService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
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

    public record UpcomingItem(LocalDate date, LocalTime time, String type, String title,
                               String schoolClass, String subject, String cssClass) {
    }

    private final CurrentUserService cu;
    private final StudentRepository students;
    private final SchoolClassRepository classes;
    private final GradeService gradeService;
    private final PaymentService paymentService;
    private final HomeworkService homeworkService;
    private final AssessmentService assessmentService;

    @Value("${app.seed-demo:true}")
    private boolean demo;

    public HomeController(CurrentUserService cu, StudentRepository students, SchoolClassRepository classes,
                          GradeService gradeService, PaymentService paymentService,
                          HomeworkService homeworkService, AssessmentService assessmentService) {
        this.cu = cu;
        this.students = students;
        this.classes = classes;
        this.gradeService = gradeService;
        this.paymentService = paymentService;
        this.homeworkService = homeworkService;
        this.assessmentService = assessmentService;
    }

    @GetMapping("/login")
    public String login(Model m) {
        m.addAttribute("demo", demo);
        return "login";
    }

    @GetMapping("/")
    public String home(Model m) {
        AppUser me = cu.get();
        if (me.getRole() == Role.SUPER_ADMIN) {
            return "redirect:/schools";
        }
        if (me.getRole() == Role.PARENT || me.getRole() == Role.STUDENT) {
            List<ChildCard> cards = cu.visibleStudents().stream()
                    .map(s -> new ChildCard(s, gradeService.progress(s), paymentService.balance(s)))
                    .toList();
            m.addAttribute("cards", cards);
                m.addAttribute("upcoming", upcomingThisWeek());
            return "dashboard-family";
        }
        School school = cu.school();
        m.addAttribute("studentCount", students.countBySchoolAndDeletedFalse(school));
        m.addAttribute("classCount", classes.findBySchoolAndDeletedFalseOrderByLevelAscSectionAsc(school).size());
        BigDecimal unpaid = paymentService.debts(school).stream()
                .map(PaymentService.Debt::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        m.addAttribute("unpaidTotal", unpaid);
        return "dashboard";
    }

    private List<UpcomingItem> upcomingThisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<UpcomingItem> items = new ArrayList<>();
        for (Homework homework : homeworkService.upcoming()) {
            if (!homework.getDueDate().isAfter(weekEnd)) {
                items.add(new UpcomingItem(homework.getDueDate(), null, "Devoir", homework.getTitle(),
                        homework.getSchoolClass().getLabel(), homework.getSubject().getName(), "event-homework"));
            }
        }
        for (Assessment assessment : assessmentService.upcoming()) {
            if (!assessment.getDate().isAfter(weekEnd)) {
                items.add(new UpcomingItem(assessment.getDate(), assessment.getStartTime(),
                        assessment.getType().getLabel(), assessment.getType().getLabel(),
                        assessment.getSchoolClass().getLabel(), assessment.getSubject().getName(),
                        switch (assessment.getType()) {
                            case CONTROLE -> "event-control";
                            case DEVOIR_SURVEILLE -> "event-devoir-surveille";
                            case EXAMEN -> "event-examen";
                            case ORAL_SYNTHESE -> "event-oral";
                        }));
            }
        }
        items.sort(Comparator.comparing(UpcomingItem::date)
                .thenComparing(UpcomingItem::time, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }
}
