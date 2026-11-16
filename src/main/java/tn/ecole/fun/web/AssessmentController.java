package tn.ecole.fun.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.ecole.fun.model.Assessment;
import tn.ecole.fun.model.AssessmentType;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.Subject;
import tn.ecole.fun.repo.SchoolClassRepository;
import tn.ecole.fun.repo.SubjectRepository;
import tn.ecole.fun.service.AssessmentService;
import tn.ecole.fun.service.SchoolWorkAccessService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/assessments")
public class AssessmentController {
    private final AssessmentService assessments;
    private final SchoolWorkAccessService access;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;

    public AssessmentController(AssessmentService assessments, SchoolWorkAccessService access,
                                SchoolClassRepository classes, SubjectRepository subjects) {
        this.assessments = assessments;
        this.access = access;
        this.classes = classes;
        this.subjects = subjects;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("assessments", assessments.listForCurrentUser());
        return "assessments/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return form(model, new Assessment());
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        access.writeContext();
        return form(model, assessments.findForCurrentUser(id));
    }

    private String form(Model model, Assessment assessment) {
        SchoolWorkAccessService.AccessContext context = access.writeContext();
        List<Subject> availableSubjects = subjects.findBySchoolAndDeletedFalseOrderByNameAsc(context.school());
        if (context.role() == Role.TEACHER) {
            availableSubjects = availableSubjects.stream()
                    .filter(subject -> subject.getTeacher() != null
                            && Objects.equals(subject.getTeacher().getId(), context.user().getId()))
                    .toList();
        }
        model.addAttribute("a", assessment);
        model.addAttribute("classes", classes.findBySchoolAndDeletedFalseOrderByLevelAscSectionAsc(context.school()));
        model.addAttribute("subjects", availableSubjects);
        model.addAttribute("types", AssessmentType.values());
        return "assessments/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long schoolClassId,
                       @RequestParam Long subjectId,
                       @RequestParam AssessmentType type,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                       @RequestParam(required = false) String room,
                       @RequestParam(required = false) String notes,
                       RedirectAttributes redirect) {
        if (id == null) {
            assessments.create(schoolClassId, subjectId, type, date, startTime, room, notes);
        } else {
            assessments.update(id, schoolClassId, subjectId, type, date, startTime, room, notes);
        }
        redirect.addFlashAttribute("ok", "Évaluation enregistrée !");
        return "redirect:/assessments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        assessments.delete(id);
        redirect.addFlashAttribute("ok", "Évaluation supprimée.");
        return "redirect:/assessments";
    }
}