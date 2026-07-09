package lk.gov.health.ums.ws;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;
import lk.gov.health.ums.entity.WebUser;
import lk.gov.health.ums.facade.EquipmentFacade;
import lk.gov.health.ums.facade.WebUserFacade;
import lk.gov.health.ums.ws.dto.ApiResponse;
import lk.gov.health.ums.ws.dto.EquipmentDto;

/**
 * Feeds the daily-entry PWA's equipment picker — scoped to the caller's own
 * institution, matching the RBAC scoping in §5 of the architecture doc.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Path("/equipment")
public class EquipmentResource {

    @Inject
    private EquipmentFacade equipmentFacade;
    @Inject
    private WebUserFacade webUserFacade;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse<List<EquipmentDto>> myInstitutionEquipment(@Context HttpServletRequest httpRequest) {
        WebUser user = webUserFacade.find(httpRequest.getSession().getAttribute("webUserId"));
        if (user.getInstitution() == null) {
            return ApiResponse.error("This account is not linked to an institution.");
        }
        List<EquipmentDto> list = equipmentFacade.findActiveByInstitution(user.getInstitution())
                .stream().map(EquipmentDto::from).collect(Collectors.toList());
        return ApiResponse.ok(list);
    }

}
