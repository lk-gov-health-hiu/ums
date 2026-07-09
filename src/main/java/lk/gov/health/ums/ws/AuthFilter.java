package lk.gov.health.ums.ws;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Rejects any /api/* call without a logged-in session, except the login
 * endpoint itself. See AuthResource for how the session is established.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    @Context
    private HttpServletRequest httpRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("auth/login")) {
            return;
        }
        Object webUserId = httpRequest.getSession(true).getAttribute("webUserId");
        if (webUserId == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"success\":false,\"message\":\"Not signed in.\"}")
                    .build());
        }
    }

}
