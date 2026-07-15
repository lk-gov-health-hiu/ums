package lk.gov.health.ums.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lk.gov.health.ums.entity.EquipmentType;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.enums.MachineStatus;
import lk.gov.health.ums.facade.EquipmentFacade;
import lk.gov.health.ums.facade.EquipmentTypeFacade;
import lk.gov.health.ums.facade.InstitutionFacade;
import lk.gov.health.ums.facade.StatusLogFacade;

/**
 * National-level utilisation dashboard: a date/equipment-type/hospital filter
 * bar over a single context-sensitive summary — totals by equipment type
 * (default, or scoped to one hospital once picked) or totals by hospital
 * (once a modality is picked with no hospital chosen). Fleet status, the
 * reporting-trend sparkline and the needs-attention list moved to the
 * Reports page ({@link ReportController}) — this page stays a single,
 * filterable summary.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Named
@ViewScoped
public class DashboardController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private StatusLogFacade statusLogFacade;
    @Inject
    private EquipmentFacade equipmentFacade;
    @Inject
    private EquipmentTypeFacade equipmentTypeFacade;
    @Inject
    private InstitutionFacade institutionFacade;
    @Inject
    private SessionController sessionController;

    private LocalDate filterDate;
    private EquipmentType filterEquipmentType;
    private Institution filterHospital;

    private List<EquipmentType> equipmentTypes;
    private List<Institution> hospitals;
    private boolean hospitalFilterLocked;

    private String summaryLabelHeader;
    private List<SummaryRow> summaryRows;
    private long summaryTotal;
    private long equipmentTotal;
    private long scansTotal;

    private long equipmentTracked;
    private long reportedCount;
    private int reportedPercent;
    private long functioningCount;
    private int functioningPercent;
    private long needsAttentionCount;

    @PostConstruct
    public void init() {
        filterDate = LocalDate.now().minusDays(1);
        equipmentTypes = equipmentTypeFacade.findAll();

        if (sessionController.isSystemAdmin() || sessionController.isNationalUser()) {
            hospitals = institutionFacade.findAll();
            hospitalFilterLocked = false;
        } else {
            Institution own = sessionController.getScopeInstitution();
            hospitals = own != null ? List.of(own) : List.of();
            filterHospital = own;
            hospitalFilterLocked = true;
        }

        refreshSummary();
        refreshKpis();
    }

    /** Ajax listener for the date/equipment-type/hospital filter controls. */
    public void onFilterChange() {
        refreshSummary();
        refreshKpis();
    }

    private void refreshSummary() {
        if (filterHospital != null) {
            summaryLabelHeader = "Equipment Type";
            summaryRows = mergeRows(
                    equipmentFacade.countActiveByType(filterHospital),
                    statusLogFacade.countReportedByType(filterDate, filterHospital),
                    statusLogFacade.sumProcedureCountByType(filterDate, filterHospital),
                    row -> typeLabel((EquipmentType) row[0]));
        } else if (filterEquipmentType != null) {
            summaryLabelHeader = "Hospital";
            summaryRows = mergeRows(
                    equipmentFacade.countActiveByInstitution(filterEquipmentType),
                    statusLogFacade.countReportedByInstitution(filterDate, filterEquipmentType),
                    statusLogFacade.sumProcedureCountByInstitution(filterDate, filterEquipmentType),
                    row -> institutionLabel((Institution) row[0]));
        } else {
            summaryLabelHeader = "Equipment Type";
            summaryRows = mergeRows(
                    equipmentFacade.countActiveByType(null),
                    statusLogFacade.countReportedByType(filterDate, null),
                    statusLogFacade.sumProcedureCountByType(filterDate, null),
                    row -> typeLabel((EquipmentType) row[0]));
        }
        summaryRows.sort(Comparator.comparing(SummaryRow::getLabel));
        equipmentTotal = summaryRows.stream().mapToLong(SummaryRow::getEquipmentCount).sum();
        summaryTotal = summaryRows.stream().mapToLong(SummaryRow::getReportedCount).sum();
        scansTotal = summaryRows.stream().mapToLong(SummaryRow::getScanCount).sum();
    }

    /**
     * The four top-line KPI tiles, scoped to the current hospital/equipment-type filter.
     * "Reported" and "Functioning" are for the selected date; "Needs attention" reflects each
     * machine's latest report regardless of date, matching its Reports-page counterpart.
     */
    private void refreshKpis() {
        equipmentTracked = equipmentFacade.countActive(filterHospital, filterEquipmentType);
        reportedCount = summaryTotal;
        reportedPercent = percentOf(reportedCount, equipmentTracked);
        functioningCount = statusLogFacade.countReportedWithStatus(
                filterDate, MachineStatus.FUNCTIONING, filterHospital, filterEquipmentType);
        functioningPercent = percentOf(functioningCount, reportedCount);
        needsAttentionCount = equipmentFacade.countNeverReported(filterHospital, filterEquipmentType)
                + statusLogFacade.countLatestNonFunctioning(filterHospital, filterEquipmentType);
    }

    private int percentOf(long count, long total) {
        return total > 0 ? (int) Math.round(count * 100.0 / total) : 0;
    }

    /**
     * Merges the three independently-grouped queries (fleet size, reported count, scan sum) into
     * one row per category, keyed by label so a category present in one query but absent from
     * another (e.g. a type with equipment but no submissions today) still gets a zero-filled row.
     */
    private List<SummaryRow> mergeRows(List<Object[]> equipmentRows, List<Object[]> reportedRows,
            List<Object[]> scanRows, Function<Object[], String> labeler) {
        Map<String, long[]> byLabel = new LinkedHashMap<>();
        for (Object[] row : equipmentRows) {
            byLabel.computeIfAbsent(labeler.apply(row), k -> new long[3])[0] = (Long) row[1];
        }
        for (Object[] row : reportedRows) {
            byLabel.computeIfAbsent(labeler.apply(row), k -> new long[3])[1] = (Long) row[1];
        }
        for (Object[] row : scanRows) {
            byLabel.computeIfAbsent(labeler.apply(row), k -> new long[3])[2] = ((Number) row[1]).longValue();
        }
        List<SummaryRow> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : byLabel.entrySet()) {
            long[] counts = entry.getValue();
            result.add(new SummaryRow(entry.getKey(), counts[0], counts[1], counts[2]));
        }
        return result;
    }

    private String typeLabel(EquipmentType type) {
        return type != null ? type.getName() : "—";
    }

    private String institutionLabel(Institution institution) {
        return institution != null ? institution.getName() : "—";
    }

    /** JSON payload (`{categories:[...], equipment:[...], reported:[...]}`) for the grouped equipment/reported chart. */
    public String getEquipmentChartJson() {
        JsonArrayBuilder categories = Json.createArrayBuilder();
        JsonArrayBuilder equipment = Json.createArrayBuilder();
        JsonArrayBuilder reported = Json.createArrayBuilder();
        for (SummaryRow row : summaryRows) {
            categories.add(row.getLabel());
            equipment.add(row.getEquipmentCount());
            reported.add(row.getReportedCount());
        }
        String json = Json.createObjectBuilder()
                .add("categories", categories)
                .add("equipment", equipment)
                .add("reported", reported)
                .build()
                .toString();
        return json.replace("</", "<\\/");
    }

    /** JSON payload (`{categories:[...], data:[...]}`) for the scan-count chart. */
    public String getScanChartJson() {
        JsonArrayBuilder categories = Json.createArrayBuilder();
        JsonArrayBuilder data = Json.createArrayBuilder();
        for (SummaryRow row : summaryRows) {
            categories.add(row.getLabel());
            data.add(row.getScanCount());
        }
        String json = Json.createObjectBuilder()
                .add("categories", categories)
                .add("data", data)
                .build()
                .toString();
        return json.replace("</", "<\\/");
    }

    public LocalDate getFilterDate() {
        return filterDate;
    }

    public void setFilterDate(LocalDate filterDate) {
        this.filterDate = filterDate;
    }

    public EquipmentType getFilterEquipmentType() {
        return filterEquipmentType;
    }

    public void setFilterEquipmentType(EquipmentType filterEquipmentType) {
        this.filterEquipmentType = filterEquipmentType;
    }

    public Institution getFilterHospital() {
        return filterHospital;
    }

    public void setFilterHospital(Institution filterHospital) {
        this.filterHospital = filterHospital;
    }

    public List<EquipmentType> getEquipmentTypes() {
        return equipmentTypes;
    }

    public List<Institution> getHospitals() {
        return hospitals;
    }

    public boolean isHospitalFilterLocked() {
        return hospitalFilterLocked;
    }

    public String getSummaryLabelHeader() {
        return summaryLabelHeader;
    }

    public List<SummaryRow> getSummaryRows() {
        return summaryRows;
    }

    public long getSummaryTotal() {
        return summaryTotal;
    }

    public long getEquipmentTotal() {
        return equipmentTotal;
    }

    public long getScansTotal() {
        return scansTotal;
    }

    public long getEquipmentTracked() {
        return equipmentTracked;
    }

    public long getReportedCount() {
        return reportedCount;
    }

    public int getReportedPercent() {
        return reportedPercent;
    }

    public long getFunctioningCount() {
        return functioningCount;
    }

    public int getFunctioningPercent() {
        return functioningPercent;
    }

    public long getNeedsAttentionCount() {
        return needsAttentionCount;
    }

    /**
     * One row of the context-sensitive summary — a label (equipment type or hospital name) with
     * its fleet size, the count reported on the selected date, and that date's scan/study total.
     */
    public static class SummaryRow implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String label;
        private final long equipmentCount;
        private final long reportedCount;
        private final long scanCount;

        SummaryRow(String label, long equipmentCount, long reportedCount, long scanCount) {
            this.label = label;
            this.equipmentCount = equipmentCount;
            this.reportedCount = reportedCount;
            this.scanCount = scanCount;
        }

        public String getLabel() {
            return label;
        }

        public long getEquipmentCount() {
            return equipmentCount;
        }

        public long getReportedCount() {
            return reportedCount;
        }

        public long getScanCount() {
            return scanCount;
        }
    }

}
