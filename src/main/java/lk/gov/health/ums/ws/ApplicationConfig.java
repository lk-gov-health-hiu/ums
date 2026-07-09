package lk.gov.health.ums.ws;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;

/**
 * REST layer backing the daily-entry PWA and dashboards (§1/§6 of the
 * architecture doc) — deliberately co-located in the same WAR as the JSF
 * admin screens, sharing the same HttpSession, so no separate auth scheme
 * (JWT/API keys) is needed for phase 1. See AuthFilter.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@ApplicationPath("/api")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(AuthResource.class, AuthFilter.class, EquipmentResource.class,
                StatusLogResource.class, JacksonJsonProvider.class);
    }

}
