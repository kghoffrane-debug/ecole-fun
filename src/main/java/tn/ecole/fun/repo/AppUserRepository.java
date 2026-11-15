package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Role;
import tn.ecole.fun.model.School;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    /** Connexion : l'identifiant est unique dans toute la base (contrainte unique sur username). */
    Optional<AppUser> findByUsernameAndDeletedFalse(String username);

    boolean existsByUsername(String username);

    // Toutes les lectures métier passent par l'école : jamais de findById() nu.
    Optional<AppUser> findByIdAndSchoolAndDeletedFalse(Long id, School school);

    List<AppUser> findBySchoolAndDeletedFalseOrderByFullNameAsc(School school);

    List<AppUser> findBySchoolAndRoleAndDeletedFalseOrderByFullNameAsc(School school, Role role);
}