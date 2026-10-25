package tn.ecole.fun.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "school_class")
@Getter
@Setter
@NoArgsConstructor
public class SchoolClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String level;
    private String section;
    private int capacity = 25;
    private String anneeScolaire;

    @ManyToOne
    private AppUser teacher;

    private boolean deleted;

    public String getLabel() {
        return level + (section == null || section.isBlank() ? "" : " " + section);
    }
}
