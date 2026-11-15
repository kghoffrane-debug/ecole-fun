package tn.ecole.fun.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Une école cliente du SaaS. Chaque donnée métier appartient à exactement une école
 * (colonne school_id).
 */
@Entity
@Table(name = "schools")
@Getter
@Setter
@NoArgsConstructor
public class School {
    public static final String TRIAL = "TRIAL";
    public static final String ACTIVE = "ACTIVE";
    public static final String SUSPENDED = "SUSPENDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** TRIAL (essai), ACTIVE ou SUSPENDED. */
    private String status = TRIAL;

    private String plan = "basic";

    private LocalDate subscriptionEnd;

    private boolean deleted;

    /** Une école suspendue, supprimée ou dont l'abonnement est terminé ne peut plus se connecter. */
    public boolean isBlocked() {
        return deleted
                || SUSPENDED.equals(status)
                || (subscriptionEnd != null && subscriptionEnd.isBefore(LocalDate.now()));
    }
}