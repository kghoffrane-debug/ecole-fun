package tn.ecole.fun.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.ecole.fun.model.AppUser;
import tn.ecole.fun.model.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipientAndDraftFalseAndHiddenByRecipientFalseOrderBySentAtDesc(AppUser recipient);

    List<Message> findBySenderAndDraftFalseOrderBySentAtDesc(AppUser sender);

    List<Message> findBySenderAndDraftTrueOrderByIdDesc(AppUser sender);

    long countByRecipientAndDraftFalseAndHiddenByRecipientFalseAndReadByRecipientFalse(AppUser recipient);
}
