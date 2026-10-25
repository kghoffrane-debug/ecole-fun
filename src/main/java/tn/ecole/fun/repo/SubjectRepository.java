package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.Subject;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByDeletedFalseOrderByNameAsc();
}
