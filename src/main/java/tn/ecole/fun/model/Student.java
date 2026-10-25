package tn.ecole.fun.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "student")
@Getter
@Setter
@NoArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String avatar = "🦊";

    @ManyToOne
    private AppUser parent;

    /** Compte de connexion de l'élève (optionnel). */
    @ManyToOne
    private AppUser user;

    @ManyToOne
    private SchoolClass schoolClass;

    private String anneeScolaire;

    @Column(precision = 12, scale = 2)
    private BigDecimal tuitionDue = BigDecimal.ZERO;

    @Column(length = 1000)
    private String medicalNotes;

    private boolean deleted;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
