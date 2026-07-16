package lk.gov.health.ums.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lk.gov.health.ums.entity.EquipmentType;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.enums.MachineStatus;
import lk.gov.health.ums.facade.EquipmentFacade;
import lk.gov.health.ums.facade.StatusLogFacade;
import lk.gov.health.ums.util.Pluralizer;

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
    private DashboardFilterState filterState;

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
    private long nonFunctioningCount;

    private static final int TREND_MONTHS = 12;
    private static final int MAX_TREND_SERIES = 7;

    private List<String> trendMonths;
    private List<TrendSeries> trendSeries;
    private boolean trendHasData;

    @PostConstruct
    public void init() {
        refreshSummary();
        refreshKpis();
        refreshTrend();
    }

    /** Ajax listener for the date filter — the trend chart spans a fixed 12-month window, so it's unaffected. */
    public void onDateChange() {
        refreshSummary();
        refreshKpis();
    }

    /** Ajax listener for the equipment-type/hospital filters — these also reshape the trend chart's breakdown. */
    public void onCategoryFilterChange() {
        refreshSummary();
        refreshKpis();
        refreshTrend();
    }

    /** Manual "Reload" button — re-runs every query under the current filters, e.g. to pick up submissions made since page load. */
    public void reload() {
        refreshSummary();
        refreshKpis();
        refreshTrend();
    }

    /** "Reset filters" button — restores the date/equipment/hospital filters to their {@link DashboardFilterState#init()} defaults. */
    public void resetFilters() {
        filterState.resetFilters();
        refreshSummary();
        refreshKpis();
        refreshTrend();
    }

    private void refreshSummary() {
        LocalDate filterDate = filterState.getFilterDate();
        EquipmentType filterEquipmentType = filterState.getFilterEquipmentType();
        Institution filterHospital = filterState.getFilterHospital();
        if (filterHospital != null) {
            summaryLabelHeader = "Equipment Type";
            summaryRows = mergeRows(
                    equipmentFacade.countActiveByType(filterHospital),
                    statusLogFacade.countReportedByType(filterDate, filterHospital),
                    statusLogFacade.sumProcedureCountByType(filterDate, filterHospital),
                    row -> typeLabel((EquipmentType) row[0]),
                    row -> procedureLabel((EquipmentType) row[0]));
        } else if (filterEquipmentType != null) {
            summaryLabelHeader = "Hospital";
            summaryRows = mergeRows(
                    equipmentFacade.countActiveByInstitution(filterEquipmentType),
                    statusLogFacade.countReportedByInstitution(filterDate, filterEquipmentType),
                    statusLogFacade.sumProcedureCountByInstitution(filterDate, filterEquipmentType),
                    row -> institutionLabel((Institution) row[0]),
                    row -> institutionLabel((Institution) row[0]));
        } else {
            summaryLabelHeader = "Equipment Type";
            summaryRows = mergeRows(
                    equipmentFacade.countActiveByType(null),
                    statusLogFacade.countReportedByType(filterDate, null),
                    statusLogFacade.sumProcedureCountByType(filterDate, null),
                    row -> typeLabel((EquipmentType) row[0]),
                    row -> procedureLabel((EquipmentType) row[0]));
        }
        summaryRows.sort(Comparator.comparing(SummaryRow::getLabel));
        equipmentTotal = summaryRows.stream().mapToLong(SummaryRow::getEquipmentCount).sum();
        summaryTotal = summaryRows.stream().mapToLong(SummaryRow::getReportedCount).sum();
        scansTotal = summaryRows.stream().mapToLong(SummaryRow::getScanCount).sum();
    }

    /**
     * The four top-line KPI tiles, scoped to the current hospital/equipment-type filter.
     * "Reported" and "Functioning" are for the selected date; "Non-functioning" reflects each
     * machine's latest report regardless of date — equipment that has never reported at all is
     * excluded, since that's a missing-data problem rather than a non-functioning one.
     */
    private void refreshKpis() {
        LocalDate filterDate = filterState.getFilterDate();
        EquipmentType filterEquipmentType = filterState.getFilterEquipmentType();
        Institution filterHospital = filterState.getFilterHospital();
        equipmentTracked = equipmentFacade.countActive(filterHospital, filterEquipmentType);
        reportedCount = summaryTotal;
        reportedPercent = percentOf(reportedCount, equipmentTracked);
        functioningCount = statusLogFacade.countReportedWithStatus(
                filterDate, MachineStatus.FUNCTIONING, filterHospital, filterEquipmentType);
        functioningPercent = percentOf(functioningCount, reportedCount);
        nonFunctioningCount = statusLogFacade.countLatestNonFunctioning(filterHospital, filterEquipmentType);
    }

    private int percentOf(long count, long total) {
        return total > 0 ? (int) Math.round(count * 100.0 / total) : 0;
    }

    /**
     * Scan-volume trend for the trailing {@link #TREND_MONTHS} months, broken down the same way as
     * the summary charts (by equipment type, or by hospital once a type is picked with no hospital).
     * Categories are bucketed into calendar months in Java (matching {@link ReportController}'s
     * day-bucketing idiom rather than a DB-specific date-truncation function); the top
     * {@link #MAX_TREND_SERIES} by trailing-year volume keep their own line, the rest are folded
     * into a single "Other" line so the legend never grows unbounded (e.g. when broken down by
     * hospital, of which there are hundreds).
     */
    private void refreshTrend() {
        EquipmentType filterEquipmentType = filterState.getFilterEquipmentType();
        Institution filterHospital = filterState.getFilterHospital();
        LocalDate since = YearMonth.now().minusMonths(TREND_MONTHS - 1).atDay(1);
        List<Object[]> rows;
        Function<Object[], String> labeler;
        if (filterHospital != null) {
            rows = statusLogFacade.sumProcedureCountByDateAndType(since, filterHospital);
            labeler = row -> typeLabel((EquipmentType) row[1]);
        } else if (filterEquipmentType != null) {
            rows = statusLogFacade.sumProcedureCountByDateAndInstitution(since, filterEquipmentType);
            labeler = row -> institutionLabel((Institution) row[1]);
        } else {
            rows = statusLogFacade.sumProcedureCountByDateAndType(since, null);
            labeler = row -> typeLabel((EquipmentType) row[1]);
        }

        Map<String, Map<YearMonth, Long>> byLabel = new LinkedHashMap<>();
        Map<String, Long> totalByLabel = new HashMap<>();
        for (Object[] row : rows) {
            YearMonth month = YearMonth.from((LocalDate) row[0]);
            String label = labeler.apply(row);
            long value = ((Number) row[2]).longValue();
            byLabel.computeIfAbsent(label, k -> new HashMap<>()).merge(month, value, Long::sum);
            totalByLabel.merge(label, value, Long::sum);
        }

        List<YearMonth> months = new ArrayList<>();
        for (YearMonth m = YearMonth.from(since); !m.isAfter(YearMonth.now()); m = m.plusMonths(1)) {
            months.add(m);
        }
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMM yyyy");
        trendMonths = months.stream().map(m -> m.format(monthFormat)).collect(Collectors.toList());

        List<String> orderedLabels = totalByLabel.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<String> topLabels = orderedLabels.size() > MAX_TREND_SERIES
                ? orderedLabels.subList(0, MAX_TREND_SERIES) : orderedLabels;

        trendSeries = new ArrayList<>();
        for (String label : topLabels) {
            trendSeries.add(new TrendSeries(label, monthlyValues(byLabel.get(label), months)));
        }
        if (orderedLabels.size() > MAX_TREND_SERIES) {
            List<String> otherLabels = orderedLabels.subList(MAX_TREND_SERIES, orderedLabels.size());
            List<Long> otherValues = new ArrayList<>();
            for (YearMonth m : months) {
                long sum = 0;
                for (String label : otherLabels) {
                    sum += byLabel.get(label).getOrDefault(m, 0L);
                }
                otherValues.add(sum);
            }
            trendSeries.add(new TrendSeries("Other", otherValues));
        }
        trendHasData = totalByLabel.values().stream().mapToLong(Long::longValue).sum() > 0;
    }

    private List<Long> monthlyValues(Map<YearMonth, Long> monthly, List<YearMonth> months) {
        List<Long> values = new ArrayList<>();
        for (YearMonth m : months) {
            values.add(monthly.getOrDefault(m, 0L));
        }
        return values;
    }

    /**
     * Merges the three independently-grouped queries (fleet size, reported count, scan sum)
     * into one row per category, keyed by label so a category present in one query but absent
     * from another (e.g. a type with equipment but no submissions today) still gets a
     * zero-filled row. {@code scanLabeler} feeds a second, scan-context label (e.g. "PET Scans"
     * rather than the machine name "PET Scanner") shown only on the scan chart.
     */
    private List<SummaryRow> mergeRows(List<Object[]> equipmentRows, List<Object[]> reportedRows,
            List<Object[]> scanRows, Function<Object[], String> labeler, Function<Object[], String> scanLabeler) {
        Map<String, long[]> byLabel = new LinkedHashMap<>();
        Map<String, String> scanLabelByLabel = new LinkedHashMap<>();
        for (Object[] row : equipmentRows) {
            String label = labeler.apply(row);
            byLabel.computeIfAbsent(label, k -> new long[3])[0] = (Long) row[1];
            scanLabelByLabel.putIfAbsent(label, scanLabeler.apply(row));
        }
        for (Object[] row : reportedRows) {
            String label = labeler.apply(row);
            byLabel.computeIfAbsent(label, k -> new long[3])[1] = (Long) row[1];
            scanLabelByLabel.putIfAbsent(label, scanLabeler.apply(row));
        }
        for (Object[] row : scanRows) {
            String label = labeler.apply(row);
            byLabel.computeIfAbsent(label, k -> new long[3])[2] = ((Number) row[1]).longValue();
            scanLabelByLabel.putIfAbsent(label, scanLabeler.apply(row));
        }
        List<SummaryRow> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : byLabel.entrySet()) {
            long[] counts = entry.getValue();
            result.add(new SummaryRow(entry.getKey(), scanLabelByLabel.get(entry.getKey()), counts[0], counts[1], counts[2]));
        }
        return result;
    }

    private String typeLabel(EquipmentType type) {
        return type != null ? type.getName() : "—";
    }

    private String institutionLabel(Institution institution) {
        return institution != null ? institution.getName() : "—";
    }

    /** The scan/procedure-context label for a type (e.g. "PET Scans"), falling back to the machine name. */
    private String procedureLabel(EquipmentType type) {
        if (type == null) {
            return "—";
        }
        String procedureName = type.getProcedureName();
        return (procedureName != null && !procedureName.isBlank()) ? procedureName : type.getName();
    }

    /**
     * JSON payload (`{categories:[...], equipment:[...]}`) for the equipment-by-category chart.
     * Categories are pluralized (e.g. "CT Scanners") when grouped by equipment type — each bar is
     * a count of that type — but left as-is when grouped by hospital, since hospital names aren't
     * nouns to pluralize.
     */
    public String getEquipmentChartJson() {
        boolean byType = "Equipment Type".equals(summaryLabelHeader);
        JsonArrayBuilder categories = Json.createArrayBuilder();
        JsonArrayBuilder equipment = Json.createArrayBuilder();
        for (SummaryRow row : summaryRows) {
            categories.add(byType ? Pluralizer.plural(row.getLabel()) : row.getLabel());
            equipment.add(row.getEquipmentCount());
        }
        String json = Json.createObjectBuilder()
                .add("categories", categories)
                .add("equipment", equipment)
                .build()
                .toString();
        return json.replace("</", "<\\/");
    }

    /**
     * JSON payload (`{categories:[...], data:[...]}`) for the scan-count chart. Categories use
     * each row's scan/procedure-context label (e.g. "PET Scans"), not the machine name shown
     * on the equipment chart.
     */
    public String getScanChartJson() {
        JsonArrayBuilder categories = Json.createArrayBuilder();
        JsonArrayBuilder data = Json.createArrayBuilder();
        for (SummaryRow row : summaryRows) {
            categories.add(row.getScanLabel());
            data.add(row.getScanCount());
        }
        String json = Json.createObjectBuilder()
                .add("categories", categories)
                .add("data", data)
                .build()
                .toString();
        return json.replace("</", "<\\/");
    }

    /** JSON payload (`{months:[...], series:[{name, data:[...]}...]}`) for the scan-volume trend chart. */
    public String getTrendChartJson() {
        JsonArrayBuilder months = Json.createArrayBuilder();
        for (String month : trendMonths) {
            months.add(month);
        }
        JsonArrayBuilder seriesArray = Json.createArrayBuilder();
        for (TrendSeries series : trendSeries) {
            JsonArrayBuilder data = Json.createArrayBuilder();
            for (Long value : series.getValues()) {
                data.add(value);
            }
            seriesArray.add(Json.createObjectBuilder().add("name", series.getLabel()).add("data", data));
        }
        String json = Json.createObjectBuilder()
                .add("months", months)
                .add("series", seriesArray)
                .build()
                .toString();
        return json.replace("</", "<\\/");
    }

    public boolean isTrendHasData() {
        return trendHasData;
    }

    public LocalDate getFilterDate() {
        return filterState.getFilterDate();
    }

    public void setFilterDate(LocalDate filterDate) {
        filterState.setFilterDate(filterDate);
    }

    public EquipmentType getFilterEquipmentType() {
        return filterState.getFilterEquipmentType();
    }

    public void setFilterEquipmentType(EquipmentType filterEquipmentType) {
        filterState.setFilterEquipmentType(filterEquipmentType);
    }

    public Institution getFilterHospital() {
        return filterState.getFilterHospital();
    }

    public void setFilterHospital(Institution filterHospital) {
        filterState.setFilterHospital(filterHospital);
    }

    public List<EquipmentType> getEquipmentTypes() {
        return filterState.getEquipmentTypes();
    }

    public List<Institution> getHospitals() {
        return filterState.getHospitals();
    }

    public boolean isHospitalFilterLocked() {
        return filterState.isHospitalFilterLocked();
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

    public long getNonFunctioningCount() {
        return nonFunctioningCount;
    }

    /**
     * One row of the context-sensitive summary — a label (equipment type or hospital name) with
     * its fleet size, the count reported on the selected date, and that date's scan/study total.
     * {@code scanLabel} is the same as {@code label} when grouped by hospital, but differs when
     * grouped by equipment type (machine name vs. procedure name, e.g. "PET Scanner" vs. "PET Scans").
     */
    public static class SummaryRow implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String label;
        private final String scanLabel;
        private final long equipmentCount;
        private final long reportedCount;
        private final long scanCount;

        SummaryRow(String label, String scanLabel, long equipmentCount, long reportedCount, long scanCount) {
            this.label = label;
            this.scanLabel = scanLabel;
            this.equipmentCount = equipmentCount;
            this.reportedCount = reportedCount;
            this.scanCount = scanCount;
        }

        public String getLabel() {
            return label;
        }

        public String getScanLabel() {
            return scanLabel;
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

    /** One line of the scan-volume trend chart — a category label and its monthly values, oldest first. */
    public static class TrendSeries implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String label;
        private final List<Long> values;

        TrendSeries(String label, List<Long> values) {
            this.label = label;
            this.values = values;
        }

        public String getLabel() {
            return label;
        }

        public List<Long> getValues() {
            return values;
        }
    }

}
