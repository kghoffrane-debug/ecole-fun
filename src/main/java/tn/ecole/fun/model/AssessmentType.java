package tn.ecole.fun.model;

public enum AssessmentType {
    CONTROLE("Contrôle"),
    DEVOIR_SURVEILLE("Devoir surveillé"),
    EXAMEN("Examen"),
    ORAL_SYNTHESE("Oral de synthèse");

    private final String label;

    AssessmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}