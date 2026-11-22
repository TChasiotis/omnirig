package com.omnirig.models;

public enum Category {
    CPU("Επεξεργαστές (CPU)"),
    MOTHERBOARD("Μητρικές (Motherboard)"),
    RAM("Μνήμες RAM"),
    GPU("Κάρτες Γραφικών (GPU)"),
    COOLER("Ψύξη (Cooler)"),
    STORAGE("Αποθήκευση (SSD)"),
    CASE("Κουτιά (Case)"),
    PSU("Τροφοδοτικά (PSU)");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}