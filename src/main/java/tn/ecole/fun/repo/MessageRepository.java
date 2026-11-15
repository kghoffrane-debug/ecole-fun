package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Message;
import tn.ecole.fun.model.School;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    /** Jamais de findById() nu : un message se lit toujours avec son école. */
    Optional<Message> findByIdAndSchool(Long id, School school);

    List<Message> findByRecipientAndDraftFalseAndHiddenByRecipientFalseOrderBySentAtDesc(AppUser recipient);

    List<Message> findBySenderAndDraftFalseOrderBySentAtDesc(AppUser sender);

    List<Message> findBySenderAndDraftTrueOrderByIdDesc(AppUser sender);

    List<Message> findByRecipient(AppUser recipient);

    long countByRecipientAndDraftFalseAndHiddenByRecipientFalseAndReadByRecipientFalse(AppUser recipient);
}
