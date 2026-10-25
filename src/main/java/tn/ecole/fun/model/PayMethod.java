package tn.ecole.fun.model;

public enum PayMethod {
    CASH("Espèces"),
    TRANSFER("Virement"),
    MOBILE_MONEY("Mobile money");

    private final String label;

    PayMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
