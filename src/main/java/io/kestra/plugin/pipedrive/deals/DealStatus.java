package io.kestra.plugin.pipedrive.deals;

public enum DealStatus {
    OPEN,
    WON,
    LOST,
    DELETED;

    /**
     * Pipedrive's API expects lowercase status values (e.g. "open", "won").
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
