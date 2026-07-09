package lk.gov.health.ums.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lk.gov.health.ums.entity.Area;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.enums.InstitutionType;
import lk.gov.health.ums.facade.AreaFacade;
import lk.gov.health.ums.facade.InstitutionFacade;

/**
 * Master-data admin screen for the Institution hierarchy — System Admin
 * only. Same list/edit-dialog shape as dmis/fmis's institution management
 * pages, PrimeFaces-rendered.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Named
@ViewScoped
public class InstitutionController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private InstitutionFacade institutionFacade;
    @Inject
    private AreaFacade areaFacade;
    @Inject
    private SessionController sessionController;

    private List<Institution> institutions;
    private List<Area> areas;
    private Institution current;

    @PostConstruct
    public void init() {
        institutions = institutionFacade.findAll();
        areas = areaFacade.findAll();
    }

    public void prepareNew() {
        current = new Institution();
    }

    public void save() {
        if (current.getCreatedAt() == null) {
            current.setCreatedBy(sessionController.getWebUser());
            current.setCreatedAt(LocalDateTime.now());
            institutionFacade.create(current);
        } else {
            current.setLastEditBy(sessionController.getWebUser());
            current.setLastEditAt(LocalDateTime.now());
            institutionFacade.edit(current);
        }
        institutions = institutionFacade.findAll();
    }

    public void retire() {
        current.setRetired(true);
        current.setRetiredBy(sessionController.getWebUser());
        current.setRetiredAt(LocalDateTime.now());
        institutionFacade.edit(current);
        institutions = institutionFacade.findAll();
    }

    public List<Institution> getInstitutions() {
        return institutions;
    }

    public List<Area> getAreas() {
        return areas;
    }

    public InstitutionType[] getInstitutionTypes() {
        return InstitutionType.values();
    }

    public Institution getCurrent() {
        return current;
    }

    public void setCurrent(Institution current) {
        this.current = current;
    }

}
