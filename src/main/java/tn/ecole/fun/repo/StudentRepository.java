package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.SchoolClass;
import tn.ecole.fun.model.Student;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByDeletedFalseOrderByLastNameAscFirstNameAsc();

    List<Student> findByParentAndDeletedFalse(AppUser parent);

    List<Student> findByUserAndDeletedFalse(AppUser user);

    long countBySchoolClassAndDeletedFalse(SchoolClass schoolClass);

    long countByParentAndDeletedFalse(AppUser parent);
}
