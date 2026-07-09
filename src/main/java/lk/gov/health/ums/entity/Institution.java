package lk.gov.health.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lk.gov.health.ums.enums.InstitutionType;

/**
 * Self-referencing reporting-line hierarchy, ported from dmis/fmis's
 * Institution entity. {@code parent} walks up to whichever body the
 * institution actually answers to — either straight to Ministry_of_Health, or
 * up through Provincial_Ministry_of_Health -&gt;
 * Provincial_Department_of_Health_Services -&gt;
 * Regional_Department_of_Health_Services. See §3 of the architecture doc.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Entity
@Table(name = "institution")
public class Institution extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private InstitutionType type;

    @Column(name = "name")
    private String name;
    @Column(name = "code")
    private String code;
    @Column(name = "address")
    private String address;
    @Column(name = "phone")
    private String phone;
    @Column(name = "email")
    private String email;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Institution parent;

    @ManyToOne
    @JoinColumn(name = "area_id")
    private Area area;

    public InstitutionType getType() {
        return type;
    }

    public void setType(InstitutionType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Institution getParent() {
        return parent;
    }

    public void setParent(Institution parent) {
        this.parent = parent;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    @Override
    public String toString() {
        return name;
    }

}
