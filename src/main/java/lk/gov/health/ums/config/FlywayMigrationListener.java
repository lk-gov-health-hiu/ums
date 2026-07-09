package lk.gov.health.ums.config;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * Runs versioned migrations from src/main/resources/db/migration against
 * jdbc/ums before the persistence unit is first used. Schema is owned by
 * Flyway, not by EclipseLink auto-DDL — see §8 of the architecture doc
 * (dmis/fmis rely on ad-hoc SQL scripts and manual EclipseLink DDL, which
 * this project deliberately does not repeat).
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@WebListener
public class FlywayMigrationListener implements ServletContextListener {

    @Resource(lookup = "jdbc/ums")
    private DataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

}
