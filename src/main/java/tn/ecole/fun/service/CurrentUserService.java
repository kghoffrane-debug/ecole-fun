package tn.ecole.fun.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.School;
import tn.ecole.fun.model.Student;
import tn.ecole.fun.repo.AppUserRepository;
import tn.ecole.fun.repo.StudentRepository;

import java.util.List;

/**
 * Point d'entrée UNIQUE de l'identité et de l'école de la personne connectée.
 * L'école vient toujours de la session côté serveur, jamais d'un paramètre du navigateur.
 */
@Service
public class CurrentUserService {
    private final AppUserRepository users;
    private final StudentRepository students;

    public CurrentUserService(AppUserRepository users, StudentRepository students) {
        this.users = users;
        this.students = students;
    }

    /** La personne connectée. */
    public AppUser get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return users.findByUsernameAndDeletedFalse(a.getName()).orElseThrow();
    }

    /** L'école de la personne connectée. 403 pour un compte sans école (propriétaire du SaaS). */
    public School school() {
        School s = get().getSchool();
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aucune école associée à ce compte.");
        }
        return s;
    }

    /** Élèves visibles : toute l'école pour le personnel, uniquement les siens pour parent / élève. */
    public List<Student> visibleStudents() {
        AppUser u = get();
        return switch (u.getRole()) {
            case PARENT -> students.findByParentAndDeletedFalse(u);
            case STUDENT -> students.findByUserAndDeletedFalse(u);
            case SUPER_ADMIN -> List.of();
            default -> students.findBySchoolAndDeletedFalseOrderByLastNameAscFirstNameAsc(school());
        };
    }

    public boolean canSee(Student s) {
        AppUser u = get();
        // 1) un élève d'une autre école n'est jamais visible
        if (u.getSchool() == null || s.getSchool() == null
                || !s.getSchool().getId().equals(u.getSchool().getId())) {
            return false;
        }
        // 2) puis le contrôle parent / élève
        if (u.getRole() == Role.PARENT) {
            return s.getParent() != null && s.getParent().getId().equals(u.getId());
        }
        if (u.getRole() == Role.STUDENT) {
            return s.getUser() != null && s.getUser().getId().equals(u.getId());
        }
        return true;
    }
}
