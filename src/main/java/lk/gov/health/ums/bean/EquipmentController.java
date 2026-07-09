package lk.gov.health.ums.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lk.gov.health.ums.entity.Equipment;
import lk.gov.health.ums.entity.EquipmentType;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.facade.EquipmentFacade;
import lk.gov.health.ums.facade.EquipmentTypeFacade;
import lk.gov.health.ums.facade.InstitutionFacade;

/**
 * Registers the physical machines (§4 of the architecture doc) that the
 * daily-entry PWA lets institution users report status/counts against.
 * Scoped per §5: System Admin sees/edits equipment anywhere; Institution
 * Admin is locked to their own institution (no institution picker shown).
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Named
@ViewScoped
public class EquipmentController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EquipmentFacade equipmentFacade;
    @Inject
    private EquipmentTypeFacade equipmentTypeFacade;
    @Inject
    private InstitutionFacade institutionFacade;
    @Inject
    private SessionController sessionController;

    private List<Equipment> equipmentList;
    private List<EquipmentType> equipmentTypes;
    private List<Institution> institutions;
    private Equipment current;

    @PostConstruct
    public void init() {
        equipmentTypes = equipmentTypeFacade.findAll();
        if (sessionController.isSystemAdmin()) {
            institutions = institutionFacade.findAll();
            equipmentList = equipmentFacade.findAll();
        } else {
            Institution own = sessionController.getScopeInstitution();
            institutions = own != null ? List.of(own) : List.of();
            equipmentList = own != null ? equipmentFacade.findActiveByInstitution(own) : List.of();
        }
    }

    public void prepareNew() {
        current = new Equipment();
        if (!sessionController.isSystemAdmin()) {
            current.setInstitution(sessionController.getScopeInstitution());
        }
    }

    public void save() {
        if (current.getCreatedAt() == null) {
            current.setCreatedBy(sessionController.getWebUser());
            current.setCreatedAt(LocalDateTime.now());
            equipmentFacade.create(current);
        } else {
            current.setLastEditBy(sessionController.getWebUser());
            current.setLastEditAt(LocalDateTime.now());
            equipmentFacade.edit(current);
        }
        init();
    }

    public void retire() {
        current.setRetired(true);
        current.setRetiredBy(sessionController.getWebUser());
        current.setRetiredAt(LocalDateTime.now());
        equipmentFacade.edit(current);
        init();
    }

    public List<Equipment> getEquipmentList() {
        return equipmentList;
    }

    public List<EquipmentType> getEquipmentTypes() {
        return equipmentTypes;
    }

    public List<Institution> getInstitutions() {
        return institutions;
    }

    public Equipment getCurrent() {
        return current;
    }

    public void setCurrent(Equipment current) {
        this.current = current;
    }

}
