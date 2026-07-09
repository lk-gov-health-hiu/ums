package lk.gov.health.ums.ws.dto;

import lk.gov.health.ums.entity.WebUser;

public class WebUserDto {

    private Long id;
    private String displayName;
    private String role;
    private Long institutionId;
    private String institutionName;

    public static WebUserDto from(WebUser u) {
        WebUserDto dto = new WebUserDto();
        dto.id = u.getId();
        dto.displayName = u.getDisplayName();
        dto.role = u.getRole().name();
        if (u.getInstitution() != null) {
            dto.institutionId = u.getInstitution().getId();
            dto.institutionName = u.getInstitution().getName();
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

}
