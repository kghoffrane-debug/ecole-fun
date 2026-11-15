package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.School;
import tn.ecole.fun.model.SchoolClass;
import tn.ecole.fun.model.Student;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findBySchoolAndDeletedFalseOrderByLastNameAscFirstNameAsc(School school);

    Optional<Student> findByIdAndSchoolAndDeletedFalse(Long id, School school);

    long countBySchoolAndDeletedFalse(School school);

    List<Student> findByParentAndDeletedFalse(AppUser parent);

    List<Student> findByUserAndDeletedFalse(AppUser user);

    long countBySchoolClassAndDeletedFalse(SchoolClass schoolClass);

    long countByParentAndDeletedFalse(AppUser parent);
}
