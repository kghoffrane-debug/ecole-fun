package tn.ecole.fun.repo;

import org.springframework.data.repository.Repository;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Homework;
import tn.ecole.fun.model.School;
import tn.ecole.fun.model.SchoolClass;

import java.util.List;
import java.util.Optional;

public interface HomeworkRepository extends Repository<Homework, Long> {
    <S extends Homework> S save(S homework);

    List<Homework> findBySchoolAndDeletedFalseOrderByDueDateAsc(School school);

    List<Homework> findBySchoolAndSchoolClassAndDeletedFalseOrderByDueDateAsc(
            School school, SchoolClass schoolClass);

        List<Homework> findBySchoolAndSchoolClassInAndDeletedFalseOrderByDueDateAsc(
            School school, List<SchoolClass> schoolClasses);

        List<Homework> findBySchoolAndTeacherAndDeletedFalseOrderByDueDateAsc(School school, AppUser teacher);

    Optional<Homework> findByIdAndSchoolAndDeletedFalse(Long id, School school);
}