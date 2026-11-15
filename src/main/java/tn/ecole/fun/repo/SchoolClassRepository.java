package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.School;
import tn.ecole.fun.model.SchoolClass;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    List<SchoolClass> findBySchoolAndDeletedFalseOrderByLevelAscSectionAsc(School school);

    Optional<SchoolClass> findByIdAndSchoolAndDeletedFalse(Long id, School school);
}
