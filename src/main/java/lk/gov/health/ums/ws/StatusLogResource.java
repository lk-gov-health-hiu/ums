package lk.gov.health.ums.ws;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lk.gov.health.ums.entity.Equipment;
import lk.gov.health.ums.entity.StatusLog;
import lk.gov.health.ums.entity.WebUser;
import lk.gov.health.ums.enums.MachineStatus;
import lk.gov.health.ums.enums.UserRole;
import lk.gov.health.ums.facade.EquipmentFacade;
import lk.gov.health.ums.facade.StatusLogFacade;
import lk.gov.health.ums.facade.WebUserFacade;
import lk.gov.health.ums.ws.dto.ApiResponse;
import lk.gov.health.ums.ws.dto.StatusLogRequest;

/**
 * The mandatory daily update (§4 of the architecture doc). One PWA screen,
 * one endpoint: submit today's status + count for a machine at the caller's
 * own institution. Upserts on (equipment, date) so a resubmission the same
 * day corrects rather than duplicates.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Path("/status-logs")
public class StatusLogResource {

    @Inject
    private StatusLogFacade statusLogFacade;
    @Inject
    private EquipmentFacade equipmentFacade;
    @Inject
    private WebUserFacade webUserFacade;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse<Void> submit(StatusLogRequest request, @Context HttpServletRequest httpRequest) {
        WebUser user = webUserFacade.find(httpRequest.getSession().getAttribute("webUserId"));
        Equipment equipment = equipmentFacade.find(request.getEquipmentId());
        if (equipment == null) {
            return ApiResponse.error("Unknown equipment.");
        }
        boolean sameInstitution = user.getInstitution() != null
                && user.getInstitution().equals(equipment.getInstitution());
        if (user.getRole() != UserRole.SYSTEM_ADMIN && !sameInstitution) {
            return ApiResponse.error("You can only submit updates for your own institution.");
        }

        LocalDate logDate;
        try {
            logDate = LocalDate.parse(request.getLogDate());
        } catch (RuntimeException e) {
            return ApiResponse.error("A valid date is required.");
        }
        if (logDate.isAfter(LocalDate.now())) {
            return ApiResponse.error("Cannot log a status for a future date.");
        }

        StatusLog log = statusLogFacade.findByEquipmentAndDate(equipment, logDate);
        boolean isNew = log == null;
        if (isNew) {
            log = new StatusLog();
            log.setEquipment(equipment);
            log.setLogDate(logDate);
            log.setCreatedBy(user);
            log.setCreatedAt(LocalDateTime.now());
        } else {
            log.setLastEditBy(user);
            log.setLastEditAt(LocalDateTime.now());
        }
        try {
            log.setStatus(MachineStatus.valueOf(request.getStatus()));
        } catch (RuntimeException e) {
            return ApiResponse.error("Unrecognized status.");
        }
        log.setProcedureCount(request.getProcedureCount());

        if (isNew) {
            statusLogFacade.create(log);
        } else {
            statusLogFacade.edit(log);
        }
        ApiResponse<Void> response = ApiResponse.ok(null);
        response.setMessage(isNew
                ? "Saved — recorded entry for " + logDate + "."
                : "Saved — replaced the entry already recorded for " + logDate + ".");
        return response;
    }

}
