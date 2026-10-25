package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Role;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameAndDeletedFalse(String username);

    boolean existsByUsername(String username);

    List<AppUser> findByDeletedFalseOrderByFullNameAsc();

    List<AppUser> findByRoleAndDeletedFalseOrderByFullNameAsc(Role role);
}
