package lk.gov.health.ums.enums;

/**
 * Grounded in WHO's Medical Equipment Maintenance Programme Overview and
 * India NHM's Biomedical Equipment Management and Maintenance Program (the
 * closest real government precedent in the region) — see §4/§9 (D4) of the
 * architecture doc for the reasoning behind each state.
 */
public enum MachineStatus {
    FUNCTIONING,
    UNDER_REPAIR,
    AWAITING_PARTS,
    NON_FUNCTIONAL_BER,
    IDLE_NO_OPERATOR;

    /** Whether this status still counts as "the machine is fine" for uptime %. */
    public boolean isOperational() {
        return this == FUNCTIONING;
    }
}
