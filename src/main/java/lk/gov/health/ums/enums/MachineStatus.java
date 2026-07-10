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

    /** e.g. NON_FUNCTIONAL_BER -> "Non functional ber" -- human-readable label for admin screens. */
    public String label() {
        String[] words = name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /** CSS class for the pulse-dot status indicator, matching ums.css's .status-* selectors. */
    public String dotClass() {
        return "status-dot status-" + name().toLowerCase().replace('_', '-');
    }

    /** CSS modifier for the dashboard's stacked status bar, matching ums.css's .seg-* selectors. */
    public String segClass() {
        return "seg-" + name().toLowerCase().replace('_', '-');
    }
}
