package me.swudam.jangbo.entity;

public enum DayOff {
    MONDAY("월"),
    TUESDAY("화"),
    WEDNESDAY("수"),
    THURSDAY("목"),
    FRIDAY("금"),
    ALWAYS_OPEN("연중무휴");

    private final String label;

    DayOff(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}