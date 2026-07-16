package lk.gov.health.ums.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import lk.gov.health.ums.entity.EquipmentType;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.facade.EquipmentTypeFacade;
import lk.gov.health.ums.facade.InstitutionFacade;

/**
 * Date/equipment-type/hospital filter selection shared between the dashboard and its
 * drill-down pages — session-scoped rather than per-view so a filter picked on one page
 * is still in effect when navigating to another.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Named
@SessionScoped
public class DashboardFilterState implements Serializable {

    private static final long serialVersionUID = 1L;

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
    }

    /** Restores the date/equipment/hospital filters to their {@link #init()} defaults. */
    public void resetFilters() {
        filterDate = LocalDate.now().minusDays(1);
        filterEquipmentType = null;
        if (!hospitalFilterLocked) {
            filterHospital = null;
        }
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

}
