package tn.ecole.fun.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.ecole.fun.model.Grade;
import tn.ecole.fun.repo.GradeRepository;
import tn.ecole.fun.repo.StudentRepository;
import tn.ecole.fun.repo.SubjectRepository;
import tn.ecole.fun.service.AuditService;

import java.time.LocalDate;

@Controller
@RequestMapping("/grades")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
public class GradeController {
    private final GradeRepository grades;
    private final StudentRepository students;
    private final SubjectRepository subjects;
    private final AuditService audit;

    @Value("${app.annee-scolaire}")
    private String annee;

    public GradeController(GradeRepository grades, StudentRepository students,
                           SubjectRepository subjects, AuditService audit) {
        this.grades = grades;
        this.students = students;
        this.subjects = subjects;
        this.audit = audit;
    }

    @GetMapping
    public String list(Model m) {
        m.addAttribute("grades", grades.findByDeletedFalseOrderByIdDesc());
        return "grades/list";
    }

    @GetMapping("/new")
    public String create(@RequestParam(required = false) Long studentId, Model m) {
        Grade g = new Grade();
        if (studentId != null) g.setStudent(students.findById(studentId).orElse(null));
        return form(m, g);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        return form(m, grades.findById(id).orElseThrow());
    }

    private String form(Model m, Grade g) {
        m.addAttribute("g", g);
        m.addAttribute("students", students.findByDeletedFalseOrderByLastNameAscFirstNameAsc());
        m.addAttribute("subjects", subjects.findByDeletedFalseOrderByNameAsc());
        return "grades/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long studentId,
                       @RequestParam Long subjectId,
                       @RequestParam String title,
                       @RequestParam double score,
                       @RequestParam(defaultValue = "1") int trimester,
                       RedirectAttributes ra) {
        if (score < 0 || score > 20) {
            ra.addFlashAttribute("err", "La note doit être comprise entre 0 et 20.");
            return id == null ? "redirect:/grades/new" : "redirect:/grades/" + id + "/edit";
        }
        Grade g = id == null ? new Grade() : grades.findById(id).orElseThrow();
        String before = id == null ? null : g.getScore() + "/20";
        g.setStudent(students.findById(studentId).orElseThrow());
        g.setSubject(subjects.findById(subjectId).orElseThrow());
        g.setTitle(title.trim());
        g.setScore(score);
        g.setTrimester(trimester);
        if (g.getAnneeScolaire() == null) g.setAnneeScolaire(annee);
        if (g.getGradedOn() == null) g.setGradedOn(LocalDate.now());
        g = grades.save(g);
        audit.log(id == null ? "CREATE" : "UPDATE", "Grade", g.getId(),
                (before == null ? "" : "ancienne valeur " + before + " → ") + score + "/20 - " + g.getStudent().getFullName());
        ra.addFlashAttribute("ok", "Note enregistrée ! ⭐");
        return "redirect:/grades";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Grade g = grades.findById(id).orElseThrow();
        g.setDeleted(true);
        grades.save(g);
        audit.log("DELETE", "Grade", g.getId(), g.getScore() + "/20 - " + g.getStudent().getFullName());
        ra.addFlashAttribute("ok", "Note supprimée.");
        return "redirect:/grades";
    }
}
