package tn.ecole.fun.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.ecole.fun.model.Homework;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.Subject;
import tn.ecole.fun.repo.SchoolClassRepository;
import tn.ecole.fun.repo.SubjectRepository;
import tn.ecole.fun.service.HomeworkService;
import tn.ecole.fun.service.SchoolWorkAccessService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/homework")
public class HomeworkController {
    private final HomeworkService homeworks;
    private final SchoolWorkAccessService access;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;

    public HomeworkController(HomeworkService homeworks, SchoolWorkAccessService access,
                              SchoolClassRepository classes, SubjectRepository subjects) {
        this.homeworks = homeworks;
        this.access = access;
        this.classes = classes;
        this.subjects = subjects;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("homeworks", homeworks.listForCurrentUser());
        return "homework/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("h", homeworks.findForCurrentUser(id));
        return "homework/detail";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return form(model, new Homework());
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        access.writeContext();
        return form(model, homeworks.findForCurrentUser(id));
    }

    private String form(Model model, Homework homework) {
        SchoolWorkAccessService.AccessContext context = access.writeContext();
        List<Subject> availableSubjects = subjects.findBySchoolAndDeletedFalseOrderByNameAsc(context.school());
        if (context.role() == Role.TEACHER) {
            availableSubjects = availableSubjects.stream()
                    .filter(subject -> subject.getTeacher() != null
                            && Objects.equals(subject.getTeacher().getId(), context.user().getId()))
                    .toList();
        }
        model.addAttribute("h", homework);
        model.addAttribute("classes", classes.findBySchoolAndDeletedFalseOrderByLevelAscSectionAsc(context.school()));
        model.addAttribute("subjects", availableSubjects);
        return "homework/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam Long schoolClassId,
                       @RequestParam Long subjectId,
                       @RequestParam String title,
                       @RequestParam(required = false) String instructions,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                       RedirectAttributes redirect) {
        if (id == null) {
            homeworks.create(schoolClassId, subjectId, title, instructions, dueDate);
        } else {
            homeworks.update(id, schoolClassId, subjectId, title, instructions, dueDate);
        }
        redirect.addFlashAttribute("ok", "Devoir enregistré !");
        return "redirect:/homework";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        homeworks.delete(id);
        redirect.addFlashAttribute("ok", "Devoir supprimé.");
        return "redirect:/homework";
    }
}