package lk.gov.health.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lk.gov.health.ums.enums.AreaType;

/**
 * Geographic hierarchy (National -&gt; Province -&gt; District -&gt; RDHS
 * Division), used for dashboard rollups. Kept separate from
 * {@link Institution}'s own parent chain, which is the reporting line rather
 * than geography — see §3 of the architecture doc.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Entity
@Table(name = "area")
public class Area extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AreaType type;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "parent_area_id")
    private Area parentArea;

    public AreaType getType() {
        return type;
    }

    public void setType(AreaType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Area getParentArea() {
        return parentArea;
    }

    public void setParentArea(Area parentArea) {
        this.parentArea = parentArea;
    }

    @Override
    public String toString() {
        return name;
    }

}
