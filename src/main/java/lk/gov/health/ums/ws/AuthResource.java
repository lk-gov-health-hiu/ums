package lk.gov.health.ums.ws;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lk.gov.health.ums.bean.PasswordUtil;
import lk.gov.health.ums.entity.WebUser;
import lk.gov.health.ums.facade.WebUserFacade;
import lk.gov.health.ums.ws.dto.ApiResponse;
import lk.gov.health.ums.ws.dto.LoginRequest;
import lk.gov.health.ums.ws.dto.WebUserDto;

/**
 * Session-cookie auth (not a separate JWT/API-key scheme) — the daily-entry
 * PWA is served from the same WAR/domain as this REST layer, so it shares
 * the HttpSession with the JSF admin pages. See ApplicationConfig and
 * AuthFilter.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Path("/auth")
public class AuthResource {

    @Inject
    private WebUserFacade webUserFacade;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse<WebUserDto> login(LoginRequest request, @Context HttpServletRequest httpRequest) {
        WebUser user = webUserFacade.findByUsername(request.getUsername());
        if (user == null || !PasswordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            return ApiResponse.error("Incorrect username or password.");
        }
        httpRequest.getSession(true).setAttribute("webUserId", user.getId());
        return ApiResponse.ok(WebUserDto.from(user));
    }

    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public ApiResponse<Void> logout(@Context HttpServletRequest httpRequest) {
        httpRequest.getSession().invalidate();
        return ApiResponse.ok(null);
    }

}
