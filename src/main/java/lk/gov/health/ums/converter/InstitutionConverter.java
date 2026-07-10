package lk.gov.health.ums.converter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.facade.InstitutionFacade;

/** Needs an explicit CDI scope to be discoverable under bean-discovery-mode="annotated"
 *  (beans.xml) — without one, @FacesConverter(managed=true) silently falls back to a
 *  plain `new` instance and @Inject never runs, leaving the facade null. */
@FacesConverter(value = "institutionConverter", managed = true)
@ApplicationScoped
public class InstitutionConverter implements Converter<Institution> {

    @Inject
    private InstitutionFacade institutionFacade;

    @Override
    public Institution getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return institutionFacade.find(Long.valueOf(value));
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Institution value) {
        return value == null || value.getId() == null ? "" : value.getId().toString();
    }

}
