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
import java.util.HashMap;
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
 * Single-page Reports section (left-nav switches which report is shown in the
 * content pane, no page navigation) — houses the panels that used to live on
 * the dashboard (fleet status, reporting-trend sparkline, needs-attention
 * list) now that the dashboard itself is a filtered summary. Fleet-wide
 * (System Admin / National User) or institution-scoped, same as before.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Named
@ViewScoped
public class ReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EquipmentFacade equipmentFacade;
    @Inject
    private StatusLogFacade statusLogFacade;
    @Inject
    private SessionController sessionController;

    private static final int TREND_WINDOW_DAYS = 14;

    private String selectedReport = "FLEET_STATUS";

    private long totalEquipment;
    private long neverReportedCount;
    private Map<MachineStatus, Long> statusCounts;
    private Map<MachineStatus, Integer> statusPercents;
    private List<AttentionRow> needsAttention;
    private List<Equipment> scopedEquipment;
    private List<TrendDay> trendDays;
    private LocalDate selectedTrendDate;
    private List<AttentionRow> drillInRows;

    @PostConstruct
    public void init() {
        statusCounts = new EnumMap<>(MachineStatus.class);
        for (MachineStatus status : MachineStatus.values()) {
            statusCounts.put(status, 0L);
        }
        needsAttention = new ArrayList<>();

        boolean fleetWide = sessionController.isSystemAdmin() || sessionController.isNationalUser();
        if (fleetWide) {
            loadNational();
        } else {
            loadInstitution();
        }

        statusPercents = new EnumMap<>(MachineStatus.class);
        for (MachineStatus status : MachineStatus.values()) {
            statusPercents.put(status, percentOf(statusCounts.get(status)));
        }

        loadTrend(fleetWide);
        onSelectDay(LocalDate.now());
    }

    private void loadNational() {
        totalEquipment = equipmentFacade.countActive();

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
        scopedEquipment = equipment;
        totalEquipment = equipment.size();

        Set<Long> reportedEquipmentIds = new HashSet<>();
        for (StatusLog log : statusLogFacade.findLatestForEquipmentIn(equipment)) {
            statusCounts.merge(log.getStatus(), 1L, Long::sum);
            reportedEquipmentIds.add(log.getEquipment().getId());
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

    private void loadTrend(boolean fleetWide) {
        LocalDate since = LocalDate.now().minusDays(TREND_WINDOW_DAYS - 1);
        List<Object[]> rows = fleetWide
                ? statusLogFacade.countByDateAndStatusSince(since)
                : statusLogFacade.countByDateAndStatusSince(since, scopedEquipment);

        Map<LocalDate, Long> functioningByDate = new HashMap<>();
        Map<LocalDate, Long> reportedByDate = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            MachineStatus status = (MachineStatus) row[1];
            long count = (Long) row[2];
            reportedByDate.merge(date, count, Long::sum);
            if (status == MachineStatus.FUNCTIONING) {
                functioningByDate.merge(date, count, Long::sum);
            }
        }

        trendDays = new ArrayList<>();
        for (LocalDate date = since; !date.isAfter(LocalDate.now()); date = date.plusDays(1)) {
            long reported = reportedByDate.getOrDefault(date, 0L);
            long functioning = functioningByDate.getOrDefault(date, 0L);
            trendDays.add(new TrendDay(date, reported, functioning, totalEquipment));
        }
    }

    /** Switches which report renders in the content pane — no page navigation, just an ajax swap. */
    public void selectReport(String report) {
        this.selectedReport = report;
    }

    /** Selects a day on the trend sparkline and loads its full submission list below it. */
    public void onSelectDay(LocalDate date) {
        selectedTrendDate = date;
        List<StatusLog> logs = scopedEquipment == null
                ? statusLogFacade.findByDate(date)
                : statusLogFacade.findByDate(date, scopedEquipment);
        drillInRows = new ArrayList<>();
        for (StatusLog log : logs) {
            drillInRows.add(AttentionRow.from(log));
        }
    }

    private int percentOf(long count) {
        return totalEquipment > 0 ? (int) Math.round(count * 100.0 / totalEquipment) : 0;
    }

    public String getSelectedReport() {
        return selectedReport;
    }

    public long getTotalEquipment() {
        return totalEquipment;
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

    public List<TrendDay> getTrendDays() {
        return trendDays;
    }

    public LocalDate getSelectedTrendDate() {
        return selectedTrendDate;
    }

    public List<AttentionRow> getDrillInRows() {
        return drillInRows;
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

    /** One column of the reporting-trend sparkline — a single day's submission volume. */
    public static class TrendDay implements Serializable {

        private static final long serialVersionUID = 1L;
        private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("dd MMM");

        private final LocalDate date;
        private final long reportedCount;
        private final int functioningBarPercent;
        private final int notFunctioningBarPercent;

        TrendDay(LocalDate date, long reportedCount, long functioningCount, long totalEquipment) {
            this.date = date;
            this.reportedCount = reportedCount;
            this.functioningBarPercent = totalEquipment > 0
                    ? (int) Math.round(functioningCount * 100.0 / totalEquipment) : 0;
            long notFunctioning = reportedCount - functioningCount;
            this.notFunctioningBarPercent = totalEquipment > 0
                    ? (int) Math.round(notFunctioning * 100.0 / totalEquipment) : 0;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getDayLabel() {
            return date.format(LABEL_FORMAT);
        }

        public long getReportedCount() {
            return reportedCount;
        }

        public int getFunctioningBarPercent() {
            return functioningBarPercent;
        }

        public int getNotFunctioningBarPercent() {
            return notFunctioningBarPercent;
        }
    }

}
