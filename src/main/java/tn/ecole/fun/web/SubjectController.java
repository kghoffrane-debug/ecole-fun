package tn.ecole.fun.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.Subject;
import tn.ecole.fun.repo.AppUserRepository;
import tn.ecole.fun.repo.GradeRepository;
import tn.ecole.fun.repo.SubjectRepository;
import tn.ecole.fun.service.AuditService;

import java.util.List;

@Controller
@RequestMapping("/subjects")
public class SubjectController {
    private static final List<String> LEVELS = List.of("Tous", "CP", "CE1", "CE2", "CE3", "CE4", "CE5", "CE6");

    private final SubjectRepository subjects;
    private final GradeRepository grades;
    private final AppUserRepository users;
    private final AuditService audit;

    public SubjectController(SubjectRepository subjects, GradeRepository grades,
                             AppUserRepository users, AuditService audit) {
        this.subjects = subjects;
        this.grades = grades;
        this.users = users;
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String list(Model m) {
        m.addAttribute("subjects", subjects.findByDeletedFalseOrderByNameAsc());
        return "subjects/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String create(Model m) {
        return form(m, new Subject());
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String edit(@PathVariable Long id, Model m) {
        return form(m, subjects.findById(id).orElseThrow());
    }

    private String form(Model m, Subject s) {
        m.addAttribute("s", s);
        m.addAttribute("levels", LEVELS);
        m.addAttribute("teachers", users.findByRoleAndDeletedFalseOrderByFullNameAsc(Role.TEACHER));
        return "subjects/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(defaultValue = "1") double coefficient,
                       @RequestParam(defaultValue = "Tous") String level,
                       @RequestParam(required = false) Long teacherId,
                       RedirectAttributes ra) {
        if (name.isBlank() || coefficient <= 0) {
            ra.addFlashAttribute("err", "Nom obligatoire et coefficient supérieur à 0.");
            return "redirect:/subjects";
        }
        Subject s = id == null ? new Subject() : subjects.findById(id).orElseThrow();
        s.setName(name.trim());
        s.setCoefficient(coefficient);
        s.setLevel(level);
        s.setTeacher(teacherId == null ? null : users.findById(teacherId).orElse(null));
        s = subjects.save(s);
        audit.log(id == null ? "CREATE" : "UPDATE", "Subject", s.getId(), s.getName() + " (coef " + coefficient + ")");
        ra.addFlashAttribute("ok", "Matière enregistrée ! 🎉");
        return "redirect:/subjects";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Subject s = subjects.findById(id).orElseThrow();
        if (grades.existsBySubjectAndDeletedFalse(s)) {
            ra.addFlashAttribute("err", "Impossible : des notes sont rattachées à « " + s.getName() + " ».");
        } else {
            s.setDeleted(true);
            subjects.save(s);
            audit.log("DELETE", "Subject", s.getId(), s.getName());
            ra.addFlashAttribute("ok", "Matière supprimée.");
        }
        return "redirect:/subjects";
    }
}
