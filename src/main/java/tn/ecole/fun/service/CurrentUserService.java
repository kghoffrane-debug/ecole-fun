package tn.ecole.fun.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.Student;
import tn.ecole.fun.repo.AppUserRepository;
import tn.ecole.fun.repo.StudentRepository;

import java.util.List;

@Service
public class CurrentUserService {
    private final AppUserRepository users;
    private final StudentRepository students;

    public CurrentUserService(AppUserRepository users, StudentRepository students) {
        this.users = users;
        this.students = students;
    }

    public AppUser get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return users.findByUsernameAndDeletedFalse(a.getName()).orElseThrow();
    }

    /** Élèves visibles : tous pour le personnel, uniquement les siens pour parent / élève. */
    public List<Student> visibleStudents() {
        AppUser u = get();
        return switch (u.getRole()) {
            case PARENT -> students.findByParentAndDeletedFalse(u);
            case STUDENT -> students.findByUserAndDeletedFalse(u);
            default -> students.findByDeletedFalseOrderByLastNameAscFirstNameAsc();
        };
    }

    public boolean canSee(Student s) {
        AppUser u = get();
        if (u.getRole() == Role.PARENT) {
            return s.getParent() != null && s.getParent().getId().equals(u.getId());
        }
        if (u.getRole() == Role.STUDENT) {
            return s.getUser() != null && s.getUser().getId().equals(u.getId());
        }
        return true;
    }
}
