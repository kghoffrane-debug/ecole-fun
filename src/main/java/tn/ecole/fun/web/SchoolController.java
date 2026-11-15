package tn.ecole.fun.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.School;
import tn.ecole.fun.repo.AppUserRepository;
import tn.ecole.fun.repo.SchoolRepository;
import tn.ecole.fun.repo.StudentRepository;
import tn.ecole.fun.service.AuditService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** CRUD des écoles clientes : réservé au propriétaire du SaaS (rôle SUPER_ADMIN). */
@Controller
@RequestMapping("/schools")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SchoolController {
    private static final List<String> STATUSES = List.of(School.TRIAL, School.ACTIVE, School.SUSPENDED);
    private static final List<String> PLANS = List.of("basic", "standard", "premium");

    private final SchoolRepository schools;
    private final AppUserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public SchoolController(SchoolRepository schools, AppUserRepository users, StudentRepository students,
                            PasswordEncoder encoder, AuditService audit) {
        this.schools = schools;
        this.users = users;
        this.students = students;
        this.encoder = encoder;
        this.audit = audit;
    }

    @GetMapping
    public String list(Model m) {
        List<School> all = schools.findByDeletedFalseOrderByNameAsc();
        Map<Long, Long> counts = new HashMap<>();
        all.forEach(s -> counts.put(s.getId(), students.countBySchoolAndDeletedFalse(s)));
        m.addAttribute("schools", all);
        m.addAttribute("counts", counts);
        return "schools/list";
    }

    @GetMapping("/new")
    public String create(Model m) {
        return form(m, new School());
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        return form(m, schools.findByIdAndDeletedFalse(id).orElseThrow(Http::notFound));
    }

    private String form(Model m, School s) {
        m.addAttribute("s", s);
        m.addAttribute("statuses", STATUSES);
        m.addAttribute("plans", PLANS);
        return "schools/form";
    }

    @PostMapping("/save")
    @Transactional
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(defaultValue = School.TRIAL) String status,
                       @RequestParam(defaultValue = "basic") String plan,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate subscriptionEnd,
                       @RequestParam(required = false) String directorName,
                       @RequestParam(required = false) String directorUsername,
                       @RequestParam(required = false) String directorPassword,
                       RedirectAttributes ra) {
        String back = id == null ? "redirect:/schools/new" : "redirect:/schools/" + id + "/edit";
        if (name.isBlank() || !STATUSES.contains(status) || !PLANS.contains(plan)) {
            ra.addFlashAttribute("err", "Nom, statut et offre valides obligatoires.");
            return back;
        }
        School s;
        if (id == null) {
            if (directorName == null || directorName.isBlank() || directorUsername == null || directorUsername.isBlank()
                    || directorPassword == null || directorPassword.length() < 6) {
                ra.addFlashAttribute("err", "Directeur : nom, identifiant et mot de passe (6 caractères min.) obligatoires.");
                return back;
            }
            if (users.existsByUsername(directorUsername.trim())) {
                ra.addFlashAttribute("err", "Cet identifiant existe déjà (il doit être unique dans toute la base).");
                return back;
            }
            s = new School();
        } else {
            s = schools.findByIdAndDeletedFalse(id).orElseThrow(Http::notFound);
        }
        s.setName(name.trim());
        s.setStatus(status);
        s.setPlan(plan);
        s.setSubscriptionEnd(subscriptionEnd);
        s = schools.save(s);

        if (id == null) {
            AppUser director = new AppUser();
            director.setSchool(s);
            director.setRole(Role.ADMIN);
            director.setUsername(directorUsername.trim());
            director.setFullName(directorName.trim());
            director.setPassword(encoder.encode(directorPassword));
            users.save(director);
        }
        audit.log(id == null ? "CREATE" : "UPDATE", "School", s.getId(), s.getName() + " (" + s.getStatus() + ")");
        ra.addFlashAttribute("ok", id == null ? "École créée avec son directeur ! 🎉" : "École modifiée.");
        return "redirect:/schools";
    }

    /** Suspendre / réactiver : les comptes de l'école ne peuvent plus se connecter. */
    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        School s = schools.findByIdAndDeletedFalse(id).orElseThrow(Http::notFound);
        s.setStatus(School.SUSPENDED.equals(s.getStatus()) ? School.ACTIVE : School.SUSPENDED);
        schools.save(s);
        audit.log("UPDATE", "School", s.getId(), s.getName() + " → " + s.getStatus());
        ra.addFlashAttribute("ok", School.SUSPENDED.equals(s.getStatus())
                ? "École suspendue : ses comptes sont bloqués." : "École réactivée.");
        return "redirect:/schools";
    }

    /** Suppression logique : les données sont conservées mais l'école n'est plus accessible. */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        School s = schools.findByIdAndDeletedFalse(id).orElseThrow(Http::notFound);
        s.setDeleted(true);
        s.setStatus(School.SUSPENDED);
        schools.save(s);
        audit.log("DELETE", "School", s.getId(), s.getName());
        ra.addFlashAttribute("ok", "École archivée (suppression logique).");
        return "redirect:/schools";
    }
}