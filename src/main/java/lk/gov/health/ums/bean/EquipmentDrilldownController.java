package lk.gov.health.ums.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lk.gov.health.ums.entity.EquipmentType;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.facade.EquipmentFacade;
import lk.gov.health.ums.util.Pluralizer;

/**
 * Drill-down behind the dashboard's "Equipment tracked" KPI: the same fleet count broken
 * down by equipment type (pie) and by hospital (line), under the filter selection shared
 * with the main dashboard via {@link DashboardFilterState}. The type breakdown is scoped
 * to the Hospital filter (if set); the hospital breakdown is scoped to the Equipment
 * filter (if set) — mirroring how {@link DashboardController} picks which axis to group
 * by, since scoping the hospital breakdown to a single selected hospital would trivialize
 * it to one bar.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Named
@ViewScoped
public class EquipmentDrilldownController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int MAX_HOSPITAL_SERIES = 20;

    @Inject
    private EquipmentFacade equipmentFacade;
    @Inject
    private DashboardFilterState filterState;

    private long equipmentTracked;
    private List<TypeCount> byType;
    private List<HospitalCount> byHospital;
    private boolean hospitalChartTruncated;

    @PostConstruct
    public void init() {
        refresh();
    }

    /** Ajax listener for any of the shared filters. */
    public void onFilterChange() {
        refresh();
    }

    /** Manual "Reload" button. */
    public void reload() {
        refresh();
    }

    /** "Reset filters" button — restores the shared filters to their defaults. */
    public void resetFilters() {
        filterState.resetFilters();
        refresh();
    }

    private void refresh() {
        Institution filterHospital = filterState.getFilterHospital();
        EquipmentType filterEquipmentType = filterState.getFilterEquipmentType();

        equipmentTracked = equipmentFacade.countActive(filterHospital, filterEquipmentType);

        byType = new ArrayList<>();
        for (Object[] row : equipmentFacade.countActiveByType(filterHospital)) {
            byType.add(new TypeCount(typeLabel((EquipmentType) row[0]), (Long) row[1]));
        }
        byType.sort(Comparator.comparing(TypeCount::getLabel));

        List<HospitalCount> allHospitals = new ArrayList<>();
        for (Object[] row : equipmentFacade.countActiveByInstitution(filterEquipmentType)) {
            allHospitals.add(new HospitalCount(institutionLabel((Institution) row[0]), (Long) row[1]));
        }
        allHospitals.sort(Comparator.comparing(HospitalCount::getCount).reversed());
        hospitalChartTruncated = allHospitals.size() > MAX_HOSPITAL_SERIES;
        byHospital = hospitalChartTruncated ? allHospitals.subList(0, MAX_HOSPITAL_SERIES) : allHospitals;
    }

    private String typeLabel(EquipmentType type) {
        return type != null ? type.getName() : "—";
    }

    private String institutionLabel(Institution institution) {
        return institution != null ? institution.getName() : "—";
    }

    /** JSON payload (`[{name, value}...]`) for the equipment-by-type pie chart — labels pluralized (e.g. "CT Scanners"), each slice being a count of that type. */
    public String getTypeChartJson() {
        JsonArrayBuilder array = Json.createArrayBuilder();
        for (TypeCount row : byType) {
            array.add(Json.createObjectBuilder().add("name", Pluralizer.plural(row.getLabel())).add("value", row.getCount()));
        }
        return array.build().toString().replace("</", "<\\/");
    }

    /** JSON payload (`{categories:[...], data:[...]}`) for the equipment-by-hospital line chart. */
    public String getHospitalChartJson() {
        JsonArrayBuilder categories = Json.createArrayBuilder();
        JsonArrayBuilder data = Json.createArrayBuilder();
        for (HospitalCount row : byHospital) {
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

    public long getEquipmentTracked() {
        return equipmentTracked;
    }

    public boolean isHasData() {
        return !byType.isEmpty() || !byHospital.isEmpty();
    }

    public boolean isHospitalChartTruncated() {
        return hospitalChartTruncated;
    }

    /** One slice of the equipment-by-type pie chart. */
    public static class TypeCount implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String label;
        private final long count;

        TypeCount(String label, long count) {
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

    /** One point of the equipment-by-hospital line chart. */
    public static class HospitalCount implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String label;
        private final long count;

        HospitalCount(String label, long count) {
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
