package lk.gov.health.ums.enums;

/**
 * Four roles as specified, scoped by {@link lk.gov.health.ums.entity.Institution}
 * hierarchy rather than dmis/fmis's larger, domain-specific role lists — see §5
 * of the architecture doc.
 */
public enum UserRole {
    SYSTEM_ADMIN,
    NATIONAL_USER,
    INSTITUTION_ADMIN,
    INSTITUTION_USER;

    public boolean isNationalLevel() {
        return this == SYSTEM_ADMIN || this == NATIONAL_USER;
    }
}
