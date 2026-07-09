package lk.gov.health.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lk.gov.health.ums.enums.MachineStatus;

/**
 * The mandatory daily update: one row per {@link Equipment} per day. Kept
 * separate from the optional patient-level detail (phase 2 —
 * {@code PatientRecord}, not yet built per the confirmed phasing decision) so
 * this quick, required entry never gets slower as optional detail is added —
 * see §4 of the architecture doc. {@code createdBy}/{@code createdAt}
 * (inherited from {@link BaseEntity}) double as "submitted by/at", since a
 * StatusLog is not expected to be edited after submission.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Entity
@Table(name = "status_log", uniqueConstraints = @UniqueConstraint(columnNames = {"equipment_id", "log_date"}))
public class StatusLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @Column(name = "log_date")
    private LocalDate logDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MachineStatus status;

    @Column(name = "procedure_count")
    private Integer procedureCount;

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDate logDate) {
        this.logDate = logDate;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public void setStatus(MachineStatus status) {
        this.status = status;
    }

    public Integer getProcedureCount() {
        return procedureCount;
    }

    public void setProcedureCount(Integer procedureCount) {
        this.procedureCount = procedureCount;
    }

}
