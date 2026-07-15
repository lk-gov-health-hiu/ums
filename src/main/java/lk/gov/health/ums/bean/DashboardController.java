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
import java.util.List;
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
        List<Object[]> rows;
        if (filterHospital != null) {
            summaryLabelHeader = "Equipment Type";
            rows = statusLogFacade.countReportedByType(filterDate, filterHospital);
            summaryRows = toRows(rows, row -> typeLabel((EquipmentType) row[0]));
        } else if (filterEquipmentType != null) {
            summaryLabelHeader = "Hospital";
            rows = statusLogFacade.countReportedByInstitution(filterDate, filterEquipmentType);
            summaryRows = toRows(rows, row -> institutionLabel((Institution) row[0]));
        } else {
            summaryLabelHeader = "Equipment Type";
            rows = statusLogFacade.countReportedByType(filterDate, null);
            summaryRows = toRows(rows, row -> typeLabel((EquipmentType) row[0]));
        }
        summaryRows.sort(Comparator.comparing(SummaryRow::getLabel));
        summaryTotal = summaryRows.stream().mapToLong(SummaryRow::getCount).sum();
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

    private List<SummaryRow> toRows(List<Object[]> rows, java.util.function.Function<Object[], String> labeler) {
        List<SummaryRow> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new SummaryRow(labeler.apply(row), (Long) row[1]));
        }
        return result;
    }

    private String typeLabel(EquipmentType type) {
        return type != null ? type.getName() : "—";
    }

    private String institutionLabel(Institution institution) {
        return institution != null ? institution.getName() : "—";
    }

    /** JSON payload (`{categories:[...], data:[...]}`) for the ECharts bar chart. */
    public String getSummaryChartJson() {
        JsonArrayBuilder categories = Json.createArrayBuilder();
        JsonArrayBuilder data = Json.createArrayBuilder();
        for (SummaryRow row : summaryRows) {
            categories.add(row.getLabel());
            data.add(row.getCount());
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

    /** One row of the context-sensitive summary — a label (equipment type or hospital name) and its count. */
    public static class SummaryRow implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String label;
        private final long count;

        SummaryRow(String label, long count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() {
            return label;
        }

        public long getCount() {
            return count;
        }
    }

}
