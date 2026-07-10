package lk.gov.health.ums.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lk.gov.health.ums.entity.Equipment;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.entity.StatusLog;
import lk.gov.health.ums.enums.MachineStatus;
import lk.gov.health.ums.facade.EquipmentFacade;
import lk.gov.health.ums.facade.StatusLogFacade;

/**
 * Fleet-wide (System Admin / National User) or institution-scoped utilisation
 * dashboard — the "not yet built" landing page from the original scaffold.
 * Panels chosen from precedent (India NHM's BEMMP facility-tier uptime/
 * functional-status views, DHIS2's reporting-completeness convention) and
 * scoped to what StatusLog already captures: latest functional status per
 * machine, today's reporting compliance, and an actionable "needs attention"
 * list (down machines + machines that have never once reported).
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Named
@ViewScoped
public class DashboardController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EquipmentFacade equipmentFacade;
    @Inject
    private StatusLogFacade statusLogFacade;
    @Inject
    private SessionController sessionController;

    private long totalEquipment;
    private long reportedToday;
    private long neverReportedCount;
    private Map<MachineStatus, Long> statusCounts;
    private Map<MachineStatus, Integer> statusPercents;
    private List<AttentionRow> needsAttention;

    @PostConstruct
    public void init() {
        statusCounts = new EnumMap<>(MachineStatus.class);
        for (MachineStatus status : MachineStatus.values()) {
            statusCounts.put(status, 0L);
        }
        needsAttention = new ArrayList<>();

        if (sessionController.isSystemAdmin() || sessionController.isNationalUser()) {
            loadNational();
        } else {
            loadInstitution();
        }

        statusPercents = new EnumMap<>(MachineStatus.class);
        for (MachineStatus status : MachineStatus.values()) {
            statusPercents.put(status, percentOf(statusCounts.get(status)));
        }
    }

    private void loadNational() {
        totalEquipment = equipmentFacade.countActive();
        reportedToday = statusLogFacade.countDistinctEquipmentReportedOn(LocalDate.now());

        for (Object[] row : statusLogFacade.countByLatestStatus()) {
            statusCounts.put((MachineStatus) row[0], (Long) row[1]);
        }
        for (StatusLog log : statusLogFacade.findLatestNonFunctioning()) {
            needsAttention.add(AttentionRow.from(log));
        }

        List<Equipment> neverReported = equipmentFacade.findNeverReported();
        neverReportedCount = neverReported.size();
        for (Equipment equipment : neverReported) {
            needsAttention.add(AttentionRow.neverReported(equipment));
        }
    }

    private void loadInstitution() {
        Institution institution = sessionController.getScopeInstitution();
        List<Equipment> equipment = institution != null
                ? equipmentFacade.findActiveByInstitution(institution) : List.of();
        totalEquipment = equipment.size();

        Set<Long> reportedEquipmentIds = new HashSet<>();
        LocalDate today = LocalDate.now();
        for (StatusLog log : statusLogFacade.findLatestForEquipmentIn(equipment)) {
            statusCounts.merge(log.getStatus(), 1L, Long::sum);
            reportedEquipmentIds.add(log.getEquipment().getId());
            if (log.getLogDate().equals(today)) {
                reportedToday++;
            }
            if (log.getStatus() != MachineStatus.FUNCTIONING) {
                needsAttention.add(AttentionRow.from(log));
            }
        }
        for (Equipment e : equipment) {
            if (!reportedEquipmentIds.contains(e.getId())) {
                neverReportedCount++;
                needsAttention.add(AttentionRow.neverReported(e));
            }
        }
    }

    private int percentOf(long count) {
        return totalEquipment > 0 ? (int) Math.round(count * 100.0 / totalEquipment) : 0;
    }

    public long getTotalEquipment() {
        return totalEquipment;
    }

    public long getReportedToday() {
        return reportedToday;
    }

    public int getReportingRatePercent() {
        return percentOf(reportedToday);
    }

    public long getNeverReportedCount() {
        return neverReportedCount;
    }

    public int getNeverReportedPercent() {
        return percentOf(neverReportedCount);
    }

    public long getNeedsAttentionCount() {
        return needsAttention.size();
    }

    public int getFunctioningPercent() {
        return statusPercents.getOrDefault(MachineStatus.FUNCTIONING, 0);
    }

    public Map<MachineStatus, Long> getStatusCounts() {
        return statusCounts;
    }

    public Map<MachineStatus, Integer> getStatusPercents() {
        return statusPercents;
    }

    /** Only the statuses with at least one machine in them — keeps the bar/legend free of empty slivers. */
    public List<MachineStatus> getPresentStatuses() {
        List<MachineStatus> present = new ArrayList<>();
        for (MachineStatus status : MachineStatus.values()) {
            if (statusCounts.get(status) > 0) {
                present.add(status);
            }
        }
        return present;
    }

    public List<AttentionRow> getNeedsAttention() {
        return needsAttention;
    }

    /** One row of the "needs attention" table — either a down machine or one that has never reported. */
    public static class AttentionRow implements Serializable {

        private static final long serialVersionUID = 1L;
        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        private String equipmentTypeName;
        private String institutionName;
        private String location;
        private String statusLabel;
        private String statusDotClass;
        private String lastReported;

        static AttentionRow from(StatusLog log) {
            AttentionRow row = new AttentionRow();
            row.fillEquipmentFields(log.getEquipment());
            row.statusLabel = log.getStatus().label();
            row.statusDotClass = log.getStatus().dotClass();
            row.lastReported = log.getLogDate().format(DATE_FORMAT);
            return row;
        }

        static AttentionRow neverReported(Equipment equipment) {
            AttentionRow row = new AttentionRow();
            row.fillEquipmentFields(equipment);
            row.statusLabel = "Never reported";
            row.statusDotClass = "status-dot status-unknown";
            row.lastReported = "—";
            return row;
        }

        private void fillEquipmentFields(Equipment equipment) {
            this.equipmentTypeName = equipment.getType() != null ? equipment.getType().getName() : "—";
            this.institutionName = equipment.getInstitution() != null ? equipment.getInstitution().getName() : "—";
            this.location = equipment.getLocation();
        }

        public String getEquipmentTypeName() {
            return equipmentTypeName;
        }

        public String getInstitutionName() {
            return institutionName;
        }

        public String getLocation() {
            return location;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public String getStatusDotClass() {
            return statusDotClass;
        }

        public String getLastReported() {
            return lastReported;
        }
    }

}
