package lk.gov.health.ums.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.facade.InstitutionFacade;

@FacesConverter(value = "institutionConverter", managed = true)
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
