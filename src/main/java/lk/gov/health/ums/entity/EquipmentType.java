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

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private EquipmentType parent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
