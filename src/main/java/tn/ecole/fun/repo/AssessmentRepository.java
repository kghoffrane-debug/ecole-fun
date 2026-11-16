package tn.ecole.fun.repo;

import org.springframework.data.repository.Repository;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Assessment;
import tn.ecole.fun.model.School;
import tn.ecole.fun.model.SchoolClass;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends Repository<Assessment, Long> {
    <S extends Assessment> S save(S assessment);

    List<Assessment> findBySchoolAndDeletedFalseOrderByDateAscStartTimeAsc(School school);

    List<Assessment> findBySchoolAndSchoolClassAndDeletedFalseOrderByDateAscStartTimeAsc(
            School school, SchoolClass schoolClass);

        List<Assessment> findBySchoolAndSchoolClassInAndDeletedFalseOrderByDateAscStartTimeAsc(
            School school, List<SchoolClass> schoolClasses);

        List<Assessment> findBySchoolAndTeacherAndDeletedFalseOrderByDateAscStartTimeAsc(School school, AppUser teacher);

    Optional<Assessment> findByIdAndSchoolAndDeletedFalse(Long id, School school);
}