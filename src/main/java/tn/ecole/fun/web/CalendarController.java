package tn.ecole.fun.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import tn.ecole.fun.model.Assessment;
import tn.ecole.fun.model.Homework;
import tn.ecole.fun.service.AssessmentService;
import tn.ecole.fun.service.HomeworkService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class CalendarController {
    private final HomeworkService homeworks;
    private final AssessmentService assessments;

    public CalendarController(HomeworkService homeworks, AssessmentService assessments) {
        this.homeworks = homeworks;
        this.assessments = assessments;
    }

    @GetMapping("/calendar")
    public String month(@RequestParam(required = false) String month,
                        @RequestParam(required = false) String day,
                        Model model) {
        YearMonth selectedMonth;
        try {
            selectedMonth = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mois invalide.");
        }

        LocalDate selectedDay = null;
        if (day != null && !day.isBlank()) {
            try {
                selectedDay = LocalDate.parse(day);
            } catch (DateTimeParseException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jour invalide.");
            }
            if (!YearMonth.from(selectedDay).equals(selectedMonth)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le jour doit appartenir au mois demandé.");
            }
        }

        List<CalendarEvent> events = new ArrayList<>();
        for (Homework homework : homeworks.forMonth(selectedMonth)) {
            events.add(new CalendarEvent(homework.getDueDate(), null, "Devoir", homework.getTitle(),
                    homework.getSchoolClass().getLabel(), homework.getSubject().getName(),
                    homework.getInstructions(), null, "event-homework"));
        }
        for (Assessment assessment : assessments.forMonth(selectedMonth)) {
            events.add(new CalendarEvent(assessment.getDate(), assessment.getStartTime(),
                    assessment.getType().getLabel(), assessment.getType().getLabel(),
                    assessment.getSchoolClass().getLabel(), assessment.getSubject().getName(),
                    assessment.getNotes(), assessment.getRoom(), cssClass(assessment)));
        }
        events.sort(Comparator.comparing(CalendarEvent::date)
                .thenComparing(CalendarEvent::startTime, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<LocalDate, List<CalendarEvent>> eventsByDate = new HashMap<>();
        events.forEach(event -> eventsByDate.computeIfAbsent(event.date(), ignored -> new ArrayList<>()).add(event));

        int leadingDays = selectedMonth.atDay(1).getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        int cellCount = ((leadingDays + selectedMonth.lengthOfMonth() + 6) / 7) * 7;
        List<CalendarCell> cells = new ArrayList<>(cellCount);
        for (int index = 0; index < cellCount; index++) {
            int dayOfMonth = index - leadingDays + 1;
            if (dayOfMonth < 1 || dayOfMonth > selectedMonth.lengthOfMonth()) {
                cells.add(new CalendarCell(null, 0, false, List.of()));
            } else {
                LocalDate date = selectedMonth.atDay(dayOfMonth);
                cells.add(new CalendarCell(date, dayOfMonth, date.equals(selectedDay),
                        eventsByDate.getOrDefault(date, List.of())));
            }
        }
        List<List<CalendarCell>> weeks = new ArrayList<>();
        for (int index = 0; index < cells.size(); index += 7) {
            weeks.add(List.copyOf(cells.subList(index, index + 7)));
        }

        model.addAttribute("month", selectedMonth);
        model.addAttribute("monthLabel", selectedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                + " " + selectedMonth.getYear());
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        model.addAttribute("weeks", weeks);
        model.addAttribute("selectedDay", selectedDay);
        model.addAttribute("selectedEvents", selectedDay == null ? List.of()
                : eventsByDate.getOrDefault(selectedDay, List.of()));
        return "calendar";
    }

    public record CalendarEvent(LocalDate date, LocalTime startTime, String type, String title,
                                String schoolClass, String subject, String details, String room, String cssClass) {
    }

    public record CalendarCell(LocalDate date, int day, boolean selected, List<CalendarEvent> events) {
    }

    private String cssClass(Assessment assessment) {
        return switch (assessment.getType()) {
            case CONTROLE -> "event-control";
            case DEVOIR_SURVEILLE -> "event-devoir-surveille";
            case EXAMEN -> "event-examen";
            case ORAL_SYNTHESE -> "event-oral";
        };
    }
}