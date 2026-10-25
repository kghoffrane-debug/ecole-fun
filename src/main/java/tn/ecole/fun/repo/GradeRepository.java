package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.Grade;
import tn.ecole.fun.model.Student;
import tn.ecole.fun.model.Subject;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByDeletedFalseOrderByIdDesc();

    List<Grade> findByStudentAndDeletedFalseOrderByIdDesc(Student student);

    boolean existsBySubjectAndDeletedFalse(Subject subject);
}
