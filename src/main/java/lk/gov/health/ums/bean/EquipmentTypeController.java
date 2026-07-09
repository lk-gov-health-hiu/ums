package lk.gov.health.ums.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lk.gov.health.ums.entity.EquipmentType;
import lk.gov.health.ums.facade.EquipmentTypeFacade;

/**
 * Admin screen for the dynamic equipment-type list (CT / MRI / PET / ...) —
 * the specialization of dmis's generic Item pattern, see §4 of the
 * architecture doc.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Named
@ViewScoped
public class EquipmentTypeController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EquipmentTypeFacade equipmentTypeFacade;
    @Inject
    private SessionController sessionController;

    private List<EquipmentType> equipmentTypes;
    private EquipmentType current;

    @PostConstruct
    public void init() {
        equipmentTypes = equipmentTypeFacade.findAll();
    }

    public void prepareNew() {
        current = new EquipmentType();
    }

    public void save() {
        if (current.getCreatedAt() == null) {
            current.setCreatedBy(sessionController.getWebUser());
            current.setCreatedAt(LocalDateTime.now());
            equipmentTypeFacade.create(current);
        } else {
            current.setLastEditBy(sessionController.getWebUser());
            current.setLastEditAt(LocalDateTime.now());
            equipmentTypeFacade.edit(current);
        }
        equipmentTypes = equipmentTypeFacade.findAll();
    }

    public void retire() {
        current.setRetired(true);
        current.setRetiredBy(sessionController.getWebUser());
        current.setRetiredAt(LocalDateTime.now());
        equipmentTypeFacade.edit(current);
        equipmentTypes = equipmentTypeFacade.findAll();
    }

    public List<EquipmentType> getEquipmentTypes() {
        return equipmentTypes;
    }

    public EquipmentType getCurrent() {
        return current;
    }

    public void setCurrent(EquipmentType current) {
        this.current = current;
    }

}
