package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.School;
import tn.ecole.fun.model.Subject;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    // NOUVELLES : limitées à une école
    List<Subject> findBySchoolAndDeletedFalseOrderByNameAsc(School school);

    Optional<Subject> findByIdAndSchoolAndDeletedFalse(Long id, School school);

    // ANCIENNE : à supprimer à la fin de B.4
    List<Subject> findByDeletedFalseOrderByNameAsc();
}
