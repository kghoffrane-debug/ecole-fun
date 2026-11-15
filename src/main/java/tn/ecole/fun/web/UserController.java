package tn.ecole.fun.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.School;
import tn.ecole.fun.repo.AppUserRepository;
import tn.ecole.fun.repo.StudentRepository;
import tn.ecole.fun.service.AuditService;
import tn.ecole.fun.service.CurrentUserService;

import java.util.Arrays;
import java.util.List;

/** CRUD des comptes de SON école : enseignants, parents, élèves, comptables. */
@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final AppUserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder encoder;
    private final CurrentUserService cu;
    private final AuditService audit;

    public UserController(AppUserRepository users, StudentRepository students, PasswordEncoder encoder,
                          CurrentUserService cu, AuditService audit) {
        this.users = users;
        this.students = students;
        this.encoder = encoder;
        this.cu = cu;
        this.audit = audit;
    }

    /** Un directeur ne peut jamais créer un super-administrateur (élévation de privilège). */
    private List<Role> assignableRoles(AppUser user) {
        return Arrays.stream(Role.values())
                .filter(role -> role != Role.SUPER_ADMIN)
                .filter(role -> role != Role.ADMIN || user.getRole() == Role.ADMIN)
                .toList();
    }

    @GetMapping
    public String list(Model m) {
        m.addAttribute("users", users.findBySchoolAndDeletedFalseOrderByFullNameAsc(cu.school()));
        return "users/list";
    }

    @GetMapping("/new")
    public String create(Model m) {
        AppUser user = new AppUser();
        m.addAttribute("u", user);
        m.addAttribute("roles", assignableRoles(user));
        return "users/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        AppUser user = users.findByIdAndSchoolAndDeletedFalse(id, cu.school()).orElseThrow(Http::notFound);
        m.addAttribute("u", user);
        m.addAttribute("roles", assignableRoles(user));
        return "users/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String username,
                       @RequestParam String fullName,
                       @RequestParam(required = false) String email,
                       @RequestParam Role role,
                       @RequestParam(required = false) String password,
                       RedirectAttributes ra) {
        School school = cu.school();
        if (role == Role.SUPER_ADMIN || (id == null && role == Role.ADMIN)) {
            ra.addFlashAttribute("err", "Ce rôle n'est pas attribuable.");
            return id == null ? "redirect:/users/new" : "redirect:/users/" + id + "/edit";
        }
        AppUser u;
        if (id == null) {
            if (username.isBlank() || password == null || password.length() < 6) {
                ra.addFlashAttribute("err", "Identifiant et mot de passe (6 caractères min.) obligatoires.");
                return "redirect:/users/new";
            }
            if (users.existsByUsername(username.trim())) {
                ra.addFlashAttribute("err", "Cet identifiant existe déjà.");
                return "redirect:/users/new";
            }
            u = new AppUser();
            u.setUsername(username.trim());
            u.setSchool(school); // l'école vient de la session, jamais du formulaire
        } else {
            u = users.findByIdAndSchoolAndDeletedFalse(id, school).orElseThrow(Http::notFound);
            if (role == Role.ADMIN && u.getRole() != Role.ADMIN) {
                ra.addFlashAttribute("err", "Le rôle Direction ne peut pas être attribué à un autre compte.");
                return "redirect:/users/" + id + "/edit";
            }
        }
        u.setFullName(fullName.trim());
        u.setEmail(email);
        u.setRole(role);
        if (password != null && !password.isBlank()) {
            if (password.length() < 6) {
                ra.addFlashAttribute("err", "Mot de passe trop court (6 caractères min.).");
                return id == null ? "redirect:/users/new" : "redirect:/users/" + id + "/edit";
            }
            u.setPassword(encoder.encode(password));
        }
        u = users.save(u);
        audit.log(id == null ? "CREATE" : "UPDATE", "AppUser", u.getId(), u.getUsername() + " (" + role + ")");
        ra.addFlashAttribute("ok", "Compte enregistré ! 🎉");
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        AppUser u = users.findByIdAndSchoolAndDeletedFalse(id, cu.school()).orElseThrow(Http::notFound);
        if (u.getId().equals(cu.get().getId())) {
            ra.addFlashAttribute("err", "Vous ne pouvez pas supprimer votre propre compte.");
        } else if (students.countByParentAndDeletedFalse(u) > 0) {
            ra.addFlashAttribute("err", "Impossible : des élèves sont rattachés à ce parent. Réaffectez-les d'abord.");
        } else {
            u.setDeleted(true);
            users.save(u);
            audit.log("DELETE", "AppUser", u.getId(), u.getUsername());
            ra.addFlashAttribute("ok", "Compte supprimé.");
        }
        return "redirect:/users";
    }
}
