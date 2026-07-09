package lk.gov.health.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * The list of procedures a {@link PatientRecord} (phase 2) can select from,
 * scoped to one {@link EquipmentType} — e.g. "CT Angiogram" only appears
 * under CT, not under MRI. See §4 of the architecture doc.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Entity
@Table(name = "clinical_procedure") // "procedure" is a reserved word in MySQL
public class Procedure extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "equipment_type_id")
    private EquipmentType equipmentType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EquipmentType getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(EquipmentType equipmentType) {
        this.equipmentType = equipmentType;
    }

    @Override
    public String toString() {
        return name;
    }

}
