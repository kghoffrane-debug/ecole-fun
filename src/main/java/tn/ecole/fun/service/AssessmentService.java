package tn.ecole.fun.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Assessment;
import tn.ecole.fun.model.AssessmentType;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.SchoolClass;
import tn.ecole.fun.model.Subject;
import tn.ecole.fun.repo.AssessmentRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class AssessmentService {
    private final AssessmentRepository assessments;
    private final SchoolWorkAccessService access;
    private final AuditService audit;

    public AssessmentService(AssessmentRepository assessments, SchoolWorkAccessService access, AuditService audit) {
        this.assessments = assessments;
        this.access = access;
        this.audit = audit;
    }

    public List<Assessment> listForCurrentUser() {
        SchoolWorkAccessService.AccessContext context = access.readContext();
        return switch (context.role()) {
            case ADMIN -> assessments.findBySchoolAndDeletedFalseOrderByDateAscStartTimeAsc(context.school());
            case TEACHER -> assessments.findBySchoolAndTeacherAndDeletedFalseOrderByDateAscStartTimeAsc(
                    context.school(), context.user());
            case PARENT, STUDENT -> {
                List<SchoolClass> visibleClasses = access.visibleClasses(context);
                yield visibleClasses.isEmpty() ? List.of()
                        : assessments.findBySchoolAndSchoolClassInAndDeletedFalseOrderByDateAscStartTimeAsc(
                                context.school(), visibleClasses);
            }
            default -> throw notFound();
        };
    }

    public List<Assessment> upcoming() {
        LocalDate today = LocalDate.now();
        return between(today, today.plusDays(14));
        }

        public List<Assessment> forMonth(YearMonth month) {
        if (month == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return between(month.atDay(1), month.atEndOfMonth());
        }

        private List<Assessment> between(LocalDate start, LocalDate end) {
        return listForCurrentUser().stream()
            .filter(assessment -> !assessment.getDate().isBefore(start)
                && !assessment.getDate().isAfter(end))
            .toList();
    }

    public Assessment findForCurrentUser(Long id) {
        SchoolWorkAccessService.AccessContext context = access.readContext();
        Assessment assessment = find(id, context);
        checkReadOwnership(context, assessment);
        return assessment;
    }

    @Transactional
    public Assessment create(Long schoolClassId, Long subjectId, AssessmentType type,
                             LocalDate date, LocalTime startTime, String room, String notes) {
        SchoolWorkAccessService.AccessContext context = access.writeContext();
        SchoolClass schoolClass = access.findClass(context, schoolClassId);
        Subject subject = access.findSubject(context, subjectId);
        access.requireTeacherOwnsSubject(context, subject);
        AppUser teacher = access.assignedTeacher(context, subject);
        validate(type, date);

        Assessment assessment = new Assessment();
        assessment.setSchool(context.school());
        assessment.setSchoolClass(schoolClass);
        assessment.setSubject(subject);
        assessment.setTeacher(teacher);
        assessment.setType(type);
        assessment.setDate(date);
        assessment.setStartTime(startTime);
        assessment.setRoom(room);
        assessment.setNotes(notes);
        assessment = assessments.save(assessment);
        audit.log("CREATE", "Assessment", assessment.getId(), type.getLabel() + " - " + date);
        return assessment;
    }

    @Transactional
    public Assessment update(Long id, Long schoolClassId, Long subjectId, AssessmentType type,
                             LocalDate date, LocalTime startTime, String room, String notes) {
        SchoolWorkAccessService.AccessContext context = access.writeContext();
        Assessment assessment = find(id, context);
        access.requireTeacherOwnsRecord(context, assessment.getTeacher());
        access.requireTeacherOwnsSubject(context, assessment.getSubject());

        SchoolClass schoolClass = access.findClass(context, schoolClassId);
        Subject subject = access.findSubject(context, subjectId);
        access.requireTeacherOwnsSubject(context, subject);
        AppUser teacher = access.assignedTeacher(context, subject);
        validate(type, date);

        assessment.setSchoolClass(schoolClass);
        assessment.setSubject(subject);
        assessment.setTeacher(teacher);
        assessment.setType(type);
        assessment.setDate(date);
        assessment.setStartTime(startTime);
        assessment.setRoom(room);
        assessment.setNotes(notes);
        assessment = assessments.save(assessment);
        audit.log("UPDATE", "Assessment", assessment.getId(), type.getLabel() + " - " + date);
        return assessment;
    }

    @Transactional
    public void delete(Long id) {
        SchoolWorkAccessService.AccessContext context = access.writeContext();
        Assessment assessment = find(id, context);
        access.requireTeacherOwnsRecord(context, assessment.getTeacher());
        access.requireTeacherOwnsSubject(context, assessment.getSubject());
        assessment.setDeleted(true);
        assessments.save(assessment);
        audit.log("DELETE", "Assessment", assessment.getId(), assessment.getType().getLabel());
    }

    private Assessment find(Long id, SchoolWorkAccessService.AccessContext context) {
        if (id == null) throw notFound();
        return assessments.findByIdAndSchoolAndDeletedFalse(id, context.school()).orElseThrow(this::notFound);
    }

    private void checkReadOwnership(SchoolWorkAccessService.AccessContext context, Assessment assessment) {
        if (context.role() == Role.TEACHER) {
            access.requireTeacherOwnsRecord(context, assessment.getTeacher());
        } else if (context.role() == Role.PARENT || context.role() == Role.STUDENT) {
            access.requireVisibleClass(context, assessment.getSchoolClass());
        }
    }

    private void validate(AssessmentType type, LocalDate date) {
        if (type == null || date == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Introuvable");
    }
}