package lk.gov.health.ums.enums;

/**
 * Geographic rollup used for dashboards (§6 of the architecture doc) —
 * independent of an institution's reporting line, which lives on
 * {@link lk.gov.health.ums.entity.Institution#getParent()} instead.
 */
public enum AreaType {
    NATIONAL,
    PROVINCE,
    DISTRICT,
    RDHS_DIVISION
}
