package tn.ecole.fun.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Message interne. Une fois envoyé (draft = false), il est immuable :
 * seul le destinataire peut le masquer de sa propre boîte.
 */
@Entity
@Table(name = "app_message")
@Getter
@Setter
@NoArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser sender;

    @ManyToOne(optional = false)
    private AppUser recipient;

    private String subject;

    @Column(length = 4000)
    private String body;

    private String linkUrl;
    private String linkLabel;

    private LocalDateTime sentAt;
    private boolean draft;
    private boolean readByRecipient;
    private boolean hiddenByRecipient;
}
