package com.smartdesk.ticket.entity;

public enum Priority {
    LOW, MEDIUM, HIGH, CRITICAL;

    /** A widely reported incident escalates itself: 5+ reports -> at least HIGH, 15+ -> CRITICAL. Never lowers. */
    public Priority escalateFor(int reports) {
        Priority byVolume = reports >= 15 ? CRITICAL : reports >= 5 ? HIGH : LOW;
        return byVolume.compareTo(this) > 0 ? byVolume : this;
    }
}
