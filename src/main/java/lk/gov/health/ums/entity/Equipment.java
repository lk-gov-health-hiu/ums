package lk.gov.health.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * One physical machine at one institution. {@code location} is deliberately
 * free text (e.g. "Radiology Department") rather than a lookup list, per the
 * original requirement.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Entity
@Table(name = "equipment")
public class Equipment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private EquipmentType type;

    @ManyToOne
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(name = "location")
    private String location;
    @Column(name = "asset_tag")
    private String assetTag;
    @Column(name = "installed_on")
    private LocalDate installedOn;

    public EquipmentType getType() {
        return type;
    }

    public void setType(EquipmentType type) {
        this.type = type;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public LocalDate getInstalledOn() {
        return installedOn;
    }

    public void setInstalledOn(LocalDate installedOn) {
        this.installedOn = installedOn;
    }

    @Override
    public String toString() {
        return (type != null ? type.getName() : "Equipment") + " — " + (institution != null ? institution.getName() : "");
    }

}
