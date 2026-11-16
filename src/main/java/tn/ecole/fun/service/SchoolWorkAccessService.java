package tn.ecole.fun.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.ecole.fun.model.*;
import tn.ecole.fun.repo.SchoolClassRepository;
import tn.ecole.fun.repo.StudentRepository;
import tn.ecole.fun.repo.SubjectRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SchoolWorkAccessService {
    private final CurrentUserService currentUsers;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final StudentRepository students;

    public SchoolWorkAccessService(CurrentUserService currentUsers, SchoolClassRepository classes,
                                   SubjectRepository subjects, StudentRepository students) {
        this.currentUsers = currentUsers;
        this.classes = classes;
        this.subjects = subjects;
        this.students = students;
    }

    public AccessContext readContext() {
        return context(false);
    }

    public AccessContext writeContext() {
        return context(true);
    }

    private AccessContext context(boolean write) {
        AppUser user = currentUsers.get();
        Role role = user.getRole();
        if (role == null || role == Role.ACCOUNTANT || role == Role.SUPER_ADMIN
                || (write && role != Role.ADMIN && role != Role.TEACHER)) {
            throw notFound();
        }
        School school = user.getSchool();
        if (school == null) throw notFound();
        return new AccessContext(user, school, role);
    }

    public SchoolClass findClass(AccessContext context, Long id) {
        if (id == null) throw notFound();
        return classes.findByIdAndSchoolAndDeletedFalse(id, context.school()).orElseThrow(this::notFound);
    }

    public Subject findSubject(AccessContext context, Long id) {
        if (id == null) throw notFound();
        return subjects.findByIdAndSchoolAndDeletedFalse(id, context.school()).orElseThrow(this::notFound);
    }

    public AppUser assignedTeacher(AccessContext context, Subject subject) {
        if (context.role() == Role.TEACHER) return context.user();
        AppUser teacher = subject.getTeacher();
        if (teacher == null) return context.user();
        if (teacher.getRole() != Role.TEACHER || teacher.getSchool() == null
                || !Objects.equals(teacher.getSchool().getId(), context.school().getId())) {
            throw notFound();
        }
        return teacher;
    }

    public void requireTeacherOwnsSubject(AccessContext context, Subject subject) {
        if (context.role() == Role.TEACHER
                && (subject.getTeacher() == null
                || !Objects.equals(subject.getTeacher().getId(), context.user().getId()))) {
            throw notFound();
        }
    }

    public void requireTeacherOwnsRecord(AccessContext context, AppUser recordTeacher) {
        if (context.role() == Role.TEACHER
                && (recordTeacher == null
                || !Objects.equals(recordTeacher.getId(), context.user().getId()))) {
            throw notFound();
        }
    }

    public List<SchoolClass> visibleClasses(AccessContext context) {
        if (context.role() == Role.ADMIN || context.role() == Role.TEACHER) {
            return classes.findBySchoolAndDeletedFalseOrderByLevelAscSectionAsc(context.school());
        }

        Map<Long, SchoolClass> visible = new LinkedHashMap<>();
        for (Student student : currentUsers.visibleStudents()) {
            SchoolClass schoolClass = student.getSchoolClass();
            if (student.getSchool() != null && schoolClass != null
                    && !schoolClass.isDeleted()
                    && Objects.equals(student.getSchool().getId(), context.school().getId())
                    && schoolClass.getSchool() != null
                    && Objects.equals(schoolClass.getSchool().getId(), context.school().getId())) {
                visible.putIfAbsent(schoolClass.getId(), schoolClass);
            }
        }
        return new ArrayList<>(visible.values());
    }

    public void requireVisibleClass(AccessContext context, SchoolClass schoolClass) {
        if (context.role() == Role.ADMIN || context.role() == Role.TEACHER) return;
        boolean visible = visibleClasses(context).stream()
                .anyMatch(candidate -> Objects.equals(candidate.getId(), schoolClass.getId()));
        if (!visible) throw notFound();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Introuvable");
    }

    public record AccessContext(AppUser user, School school, Role role) {
    }
}