package lk.gov.health.ums.ws.dto;

import lk.gov.health.ums.entity.Equipment;

public class EquipmentDto {

    private Long id;
    private String equipmentTypeName;
    private String location;
    private String assetTag;

    public static EquipmentDto from(Equipment e) {
        EquipmentDto dto = new EquipmentDto();
        dto.id = e.getId();
        dto.equipmentTypeName = e.getType() != null ? e.getType().getName() : null;
        dto.location = e.getLocation();
        dto.assetTag = e.getAssetTag();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getEquipmentTypeName() {
        return equipmentTypeName;
    }

    public String getLocation() {
        return location;
    }

    public String getAssetTag() {
        return assetTag;
    }

}
