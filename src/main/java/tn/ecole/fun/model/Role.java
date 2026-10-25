package tn.ecole.fun.model;

public enum Role {
    ADMIN("Direction"),
    TEACHER("Enseignant"),
    PARENT("Parent"),
    STUDENT("Élève"),
    ACCOUNTANT("Comptable / Secrétariat");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
