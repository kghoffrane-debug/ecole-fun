package tn.ecole.fun.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.SchoolClass;
import tn.ecole.fun.repo.AppUserRepository;
import tn.ecole.fun.repo.SchoolClassRepository;
import tn.ecole.fun.repo.StudentRepository;
import tn.ecole.fun.service.AuditService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/classes")
public class ClassController {
    private static final List<String> LEVELS = List.of("CP", "CE1", "CE2", "CE3", "CE4", "CE5", "CE6");

    private final SchoolClassRepository classes;
    private final StudentRepository students;
    private final AppUserRepository users;
    private final AuditService audit;

    @Value("${app.annee-scolaire}")
    private String annee;

    public ClassController(SchoolClassRepository classes, StudentRepository students,
                           AppUserRepository users, AuditService audit) {
        this.classes = classes;
        this.students = students;
        this.users = users;
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String list(Model m) {
        List<SchoolClass> all = classes.findByDeletedFalseOrderByLevelAscSectionAsc();
        Map<Long, Long> counts = new HashMap<>();
        all.forEach(c -> counts.put(c.getId(), students.countBySchoolClassAndDeletedFalse(c)));
        m.addAttribute("classes", all);
        m.addAttribute("counts", counts);
        return "classes/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String create(Model m) {
        SchoolClass c = new SchoolClass();
        c.setAnneeScolaire(annee);
        return form(m, c);
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String edit(@PathVariable Long id, Model m) {
        return form(m, classes.findById(id).orElseThrow());
    }

    private String form(Model m, SchoolClass c) {
        m.addAttribute("c", c);
        m.addAttribute("levels", LEVELS);
        m.addAttribute("teachers", users.findByRoleAndDeletedFalseOrderByFullNameAsc(Role.TEACHER));
        return "classes/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String level,
                       @RequestParam(required = false) String section,
                       @RequestParam(defaultValue = "25") int capacity,
                       @RequestParam(required = false) String anneeScolaire,
                       @RequestParam(required = false) Long teacherId,
                       RedirectAttributes ra) {
        if (capacity < 1) {
            ra.addFlashAttribute("err", "La capacité doit être d'au moins 1 élève.");
            return "redirect:/classes";
        }
        SchoolClass c = id == null ? new SchoolClass() : classes.findById(id).orElseThrow();
        long current = id == null ? 0 : students.countBySchoolClassAndDeletedFalse(c);
        if (capacity < current) {
            ra.addFlashAttribute("err", "La capacité ne peut pas être inférieure à l'effectif actuel (" + current + ").");
            return "redirect:/classes/" + id + "/edit";
        }
        c.setLevel(level);
        c.setSection(section);
        c.setCapacity(capacity);
        c.setAnneeScolaire(anneeScolaire == null || anneeScolaire.isBlank() ? annee : anneeScolaire);
        AppUser teacher = teacherId == null ? null : users.findById(teacherId).orElse(null);
        c.setTeacher(teacher);
        c = classes.save(c);
        audit.log(id == null ? "CREATE" : "UPDATE", "SchoolClass", c.getId(), c.getLabel());
        ra.addFlashAttribute("ok", "Classe enregistrée ! 🎉");
        return "redirect:/classes";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        SchoolClass c = classes.findById(id).orElseThrow();
        if (students.countBySchoolClassAndDeletedFalse(c) > 0) {
            ra.addFlashAttribute("err", "Impossible : des élèves sont encore affectés à " + c.getLabel()
                    + ". Réaffectez-les d'abord.");
        } else {
            c.setDeleted(true);
            classes.save(c);
            audit.log("DELETE", "SchoolClass", c.getId(), c.getLabel());
            ra.addFlashAttribute("ok", "Classe supprimée.");
        }
        return "redirect:/classes";
    }
}
