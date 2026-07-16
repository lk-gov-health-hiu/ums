package lk.gov.health.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * The "dynamic list" the Ministry needs (CT / MRI / PET / ...), specializing
 * dmis's generic self-referencing {@code Item} master-data pattern — see §2
 * and §4 of the architecture doc. Managed by System Admin / National User
 * only (§5); {@link BaseEntity#isRetired()} is used to deactivate a type
 * without deleting equipment already recorded against it.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Entity
@Table(name = "equipment_type")
public class EquipmentType extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name")
    private String name;

    /**
     * The name used when talking about the test/procedure this equipment performs, distinct
     * from {@link #name} (the machine itself) — e.g. machine name "PET Scanner" vs. procedure
     * name "PET Scans". Falls back to {@link #name} wherever blank.
     */
    @Column(name = "procedure_name")
    private String procedureName;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private EquipmentType parent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }

    public EquipmentType getParent() {
        return parent;
    }

    public void setParent(EquipmentType parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return name;
    }

}
