package lk.gov.health.ums.enums;

/**
 * Static list, as opposed to {@link lk.gov.health.ums.entity.EquipmentType}
 * which is a dynamic, admin-managed list. Two reporting lines both terminate
 * at Ministry_of_Health — see §3 of the architecture doc.
 * <p>
 * Values below cover every {@code type_atomic} category found in the MoH
 * institution registry (see tools/import-institutions.js and
 * db/migration/V2__import_institutions.sql) plus a few not present in that
 * source but named in the original requirements (Specialized_Hospital,
 * Provincial_General_Hospital) — kept for manual data entry.
 */
public enum InstitutionType {
    Ministry_of_Health,
    Provincial_Ministry_of_Health,
    Provincial_Department_of_Health_Services,
    Regional_Department_of_Health_Services,
    MOH_Office,

    National_Hospital,
    Teaching_Hospital,
    Specialized_Teaching_Hospital,
    Specialized_Hospital,
    Other_Specialized_Hospital,
    Provincial_General_Hospital,
    District_General_Hospital,
    Base_Hospital,
    Base_Hospital_Type_A,
    Base_Hospital_Type_B,
    Divisional_Hospital,
    Divisional_Hospital_Type_A,
    Divisional_Hospital_Type_B,
    Divisional_Hospital_Type_C,
    Other_Hospital,
    Board_Managed_Hospital_Tertiary_Care,
    Board_Managed_Hospital_Secondary_Care,

    Primary_Medical_Care_Unit,
    Chest_Clinic,
    STD_Clinic,

    Air_Force_Hospital,
    Army_Hospital,
    Navy_Hospital,
    Police_Hospital,
    Prison_Hospital,

    ADC,
    CDC,
    NTS,

    Other
}
