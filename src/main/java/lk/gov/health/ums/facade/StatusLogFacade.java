package lk.gov.health.ums.facade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import lk.gov.health.ums.entity.Equipment;
import lk.gov.health.ums.entity.StatusLog;

@Stateless
public class StatusLogFacade extends AbstractFacade<StatusLog> {

    @PersistenceContext(unitName = "umsPU")
    private EntityManager em;

    public StatusLogFacade() {
        super(StatusLog.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** One row per equipment per day — used to decide insert vs. update on submission. */
    public StatusLog findByEquipmentAndDate(Equipment equipment, LocalDate date) {
        try {
            return em.createQuery(
                    "SELECT s FROM StatusLog s WHERE s.equipment = :equipment AND s.logDate = :date", StatusLog.class)
                    .setParameter("equipment", equipment)
                    .setParameter("date", date)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

}
