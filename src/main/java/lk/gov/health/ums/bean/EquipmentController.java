package lk.gov.health.ums.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lk.gov.health.ums.entity.Equipment;
import lk.gov.health.ums.entity.EquipmentType;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.entity.StatusLog;
import lk.gov.health.ums.enums.MachineStatus;
import lk.gov.health.ums.facade.EquipmentFacade;
import lk.gov.health.ums.facade.EquipmentTypeFacade;
import lk.gov.health.ums.facade.InstitutionFacade;
import lk.gov.health.ums.facade.StatusLogFacade;

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
    private StatusLogFacade statusLogFacade;
    @Inject
    private SessionController sessionController;

    private List<Equipment> equipmentList;
    private List<EquipmentType> equipmentTypes;
    private List<Institution> institutions;
    private Map<Long, StatusLog> latestStatusByEquipmentId;
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
        loadLatestStatuses();
    }

    /** Most recent {@link StatusLog} per equipment, for the "Last Status" pulse-dot column. */
    private void loadLatestStatuses() {
        latestStatusByEquipmentId = new HashMap<>();
        if (equipmentList.isEmpty()) {
            return;
        }
        List<StatusLog> logs = statusLogFacade.findByJpql(
                "SELECT s FROM StatusLog s WHERE s.equipment IN :equipment ORDER BY s.logDate DESC",
                Map.of("equipment", equipmentList));
        for (StatusLog log : logs) {
            latestStatusByEquipmentId.putIfAbsent(log.getEquipment().getId(), log);
        }
    }

    public MachineStatus getLatestStatus(Equipment equipment) {
        StatusLog log = latestStatusByEquipmentId.get(equipment.getId());
        return log != null ? log.getStatus() : null;
    }

    public String statusDotClass(Equipment equipment) {
        MachineStatus status = getLatestStatus(equipment);
        return status != null ? status.dotClass() : "status-dot status-unknown";
    }

    public String statusLabel(Equipment equipment) {
        MachineStatus status = getLatestStatus(equipment);
        return status != null ? status.label() : "No data yet";
    }

    public void prepareNew() {
        current = new Equipment();
        if (!sessionController.isSystemAdmin()) {
            current.setInstitution(sessionController.getScopeInstitution());
        }
    }

    public void save() {
        try {
            if (current.getCreatedAt() == null) {
                current.setCreatedBy(sessionController.getWebUser());
                current.setCreatedAt(LocalDateTime.now());
                equipmentFacade.create(current);
            } else {
                current.setLastEditBy(sessionController.getWebUser());
                current.setLastEditAt(LocalDateTime.now());
                equipmentFacade.edit(current);
            }
        } catch (RuntimeException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Could not save equipment",
                    "Check that a type and institution are selected."));
            return;
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
