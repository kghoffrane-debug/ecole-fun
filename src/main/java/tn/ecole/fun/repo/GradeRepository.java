package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.Grade;
import tn.ecole.fun.model.School;
import tn.ecole.fun.model.Student;
import tn.ecole.fun.model.Subject;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findBySchoolAndDeletedFalseOrderByIdDesc(School school);

    Optional<Grade> findByIdAndSchoolAndDeletedFalse(Long id, School school);

    List<Grade> findByStudentAndDeletedFalseOrderByIdDesc(Student student);

    boolean existsBySubjectAndDeletedFalse(Subject subject);
}
