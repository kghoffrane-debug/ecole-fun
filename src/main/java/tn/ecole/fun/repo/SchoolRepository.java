package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.School;

import java.util.List;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {
    List<School> findByDeletedFalseOrderByNameAsc();

    Optional<School> findByIdAndDeletedFalse(Long id);

    Optional<School> findFirstByName(String name);
}