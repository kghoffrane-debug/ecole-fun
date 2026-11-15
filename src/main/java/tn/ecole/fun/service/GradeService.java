package tn.ecole.fun.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import tn.ecole.fun.model.Grade;
import tn.ecole.fun.model.Student;
import tn.ecole.fun.model.Subject;
import tn.ecole.fun.repo.GradeRepository;

import java.util.*;

/** Moyennes par matière, moyenne générale pondérée, étoiles et badges. */
@Service
public class GradeService {

    @Getter
    @AllArgsConstructor
    public static class SubjectRow {
        private Subject subject;
        private double average;
        private int count;
    }

    @Getter
    @AllArgsConstructor
    public static class Progress {
        private List<SubjectRow> rows;
        private Double average;
        private int stars;
        private List<String> badges;
        private List<Grade> grades;
    }

    private final GradeRepository grades;

    public GradeService(GradeRepository grades) {
        this.grades = grades;
    }

    public Progress progress(Student s) {
        List<Grade> list = grades.findByStudentAndDeletedFalseOrderByIdDesc(s);
        Map<Long, List<Grade>> bySubject = new LinkedHashMap<>();
        for (Grade g : list) {
            bySubject.computeIfAbsent(g.getSubject().getId(), k -> new ArrayList<>()).add(g);
        }
        List<SubjectRow> rows = new ArrayList<>();
        double num = 0;
        double den = 0;
        for (List<Grade> gl : bySubject.values()) {
            Subject sub = gl.get(0).getSubject();
            double avg = gl.stream().mapToDouble(Grade::getScore).average().orElse(0);
            rows.add(new SubjectRow(sub, avg, gl.size()));
            num += avg * sub.getCoefficient();
            den += sub.getCoefficient();
        }
        rows.sort(Comparator.comparing(r -> r.getSubject().getName()));
        Double general = den == 0 ? null : num / den;
        return new Progress(rows, general, stars(general), badges(list, general), list);
    }

    private int stars(Double avg) {
        if (avg == null) return 0;
        if (avg >= 16) return 3;
        if (avg >= 13) return 2;
        if (avg >= 10) return 1;
        return 0;
    }

    private List<String> badges(List<Grade> list, Double avg) {
        List<String> b = new ArrayList<>();
        if (list.isEmpty()) {
            b.add("🐣 Bienvenue !");
            return b;
        }
        if (list.stream().anyMatch(g -> g.getScore() >= 20)) b.add("🏆 Champion du 20");
        if (avg != null && avg >= 16) b.add("🚀 Fusée de la classe");
        else if (avg != null && avg >= 14) b.add("🌟 Étoile brillante");
        if (list.size() >= 5) b.add("🎯 Toujours au top");
        if (b.isEmpty()) b.add("🌱 En progression");
        return b;
    }
}
