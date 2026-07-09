package lk.gov.health.ums.converter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import lk.gov.health.ums.entity.EquipmentType;
import lk.gov.health.ums.facade.EquipmentTypeFacade;

@FacesConverter(value = "equipmentTypeConverter", managed = true)
public class EquipmentTypeConverter implements Converter<EquipmentType> {

    @Inject
    private EquipmentTypeFacade equipmentTypeFacade;

    @Override
    public EquipmentType getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return equipmentTypeFacade.find(Long.valueOf(value));
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, EquipmentType value) {
        return value == null || value.getId() == null ? "" : value.getId().toString();
    }

}
