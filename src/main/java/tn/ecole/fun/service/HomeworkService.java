package tn.ecole.fun.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Homework;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.SchoolClass;
import tn.ecole.fun.model.Subject;
import tn.ecole.fun.repo.HomeworkRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class HomeworkService {
    private final HomeworkRepository homeworks;
    private final SchoolWorkAccessService access;
    private final AuditService audit;

    public HomeworkService(HomeworkRepository homeworks, SchoolWorkAccessService access, AuditService audit) {
        this.homeworks = homeworks;
        this.access = access;
        this.audit = audit;
    }

    public List<Homework> listForCurrentUser() {
        SchoolWorkAccessService.AccessContext context = access.readContext();
        return switch (context.role()) {
            case ADMIN -> homeworks.findBySchoolAndDeletedFalseOrderByDueDateAsc(context.school());
            case TEACHER -> homeworks.findBySchoolAndTeacherAndDeletedFalseOrderByDueDateAsc(
                    context.school(), context.user());
            case PARENT, STUDENT -> {
                List<SchoolClass> visibleClasses = access.visibleClasses(context);
                yield visibleClasses.isEmpty() ? List.of()
                        : homeworks.findBySchoolAndSchoolClassInAndDeletedFalseOrderByDueDateAsc(
                                context.school(), visibleClasses);
            }
            default -> throw notFound();
        };
    }

    public List<Homework> upcoming() {
        LocalDate today = LocalDate.now();
        return between(today, today.plusDays(14));
        }

        public List<Homework> forMonth(YearMonth month) {
        if (month == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return between(month.atDay(1), month.atEndOfMonth());
        }

        private List<Homework> between(LocalDate start, LocalDate end) {
        return listForCurrentUser().stream()
            .filter(homework -> homework.getDueDate() != null
                && !homework.getDueDate().isBefore(start)
                && !homework.getDueDate().isAfter(end))
            .toList();
    }

    public Homework findForCurrentUser(Long id) {
        SchoolWorkAccessService.AccessContext context = access.readContext();
        Homework homework = find(id, context);
        checkReadOwnership(context, homework);
        return homework;
    }

    @Transactional
    public Homework create(Long schoolClassId, Long subjectId, String title, String instructions, LocalDate dueDate) {
        SchoolWorkAccessService.AccessContext context = access.writeContext();
        SchoolClass schoolClass = access.findClass(context, schoolClassId);
        Subject subject = access.findSubject(context, subjectId);
        access.requireTeacherOwnsSubject(context, subject);
        AppUser teacher = access.assignedTeacher(context, subject);

        Homework homework = new Homework();
        homework.setSchool(context.school());
        homework.setSchoolClass(schoolClass);
        homework.setSubject(subject);
        homework.setTeacher(teacher);
        homework.setTitle(title);
        homework.setInstructions(instructions);
        homework.setDueDate(dueDate);
        homework.setCreatedAt(LocalDateTime.now());
        homework = homeworks.save(homework);
        audit.log("CREATE", "Homework", homework.getId(), homework.getTitle());
        return homework;
    }

    @Transactional
    public Homework update(Long id, Long schoolClassId, Long subjectId, String title,
                           String instructions, LocalDate dueDate) {
        SchoolWorkAccessService.AccessContext context = access.writeContext();
        Homework homework = find(id, context);
        access.requireTeacherOwnsRecord(context, homework.getTeacher());
        access.requireTeacherOwnsSubject(context, homework.getSubject());

        SchoolClass schoolClass = access.findClass(context, schoolClassId);
        Subject subject = access.findSubject(context, subjectId);
        access.requireTeacherOwnsSubject(context, subject);
        AppUser teacher = access.assignedTeacher(context, subject);

        homework.setSchoolClass(schoolClass);
        homework.setSubject(subject);
        homework.setTeacher(teacher);
        homework.setTitle(title);
        homework.setInstructions(instructions);
        homework.setDueDate(dueDate);
        homework = homeworks.save(homework);
        audit.log("UPDATE", "Homework", homework.getId(), homework.getTitle());
        return homework;
    }

    @Transactional
    public void delete(Long id) {
        SchoolWorkAccessService.AccessContext context = access.writeContext();
        Homework homework = find(id, context);
        access.requireTeacherOwnsRecord(context, homework.getTeacher());
        access.requireTeacherOwnsSubject(context, homework.getSubject());
        homework.setDeleted(true);
        homeworks.save(homework);
        audit.log("DELETE", "Homework", homework.getId(), homework.getTitle());
    }

    private Homework find(Long id, SchoolWorkAccessService.AccessContext context) {
        if (id == null) throw notFound();
        return homeworks.findByIdAndSchoolAndDeletedFalse(id, context.school()).orElseThrow(this::notFound);
    }

    private void checkReadOwnership(SchoolWorkAccessService.AccessContext context, Homework homework) {
        if (context.role() == Role.TEACHER) {
            access.requireTeacherOwnsRecord(context, homework.getTeacher());
        } else if (context.role() == Role.PARENT || context.role() == Role.STUDENT) {
            access.requireVisibleClass(context, homework.getSchoolClass());
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Introuvable");
    }
}