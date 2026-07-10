package lk.gov.health.ums.facade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
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

    /** How many distinct machines have a submission for the given day — the reporting-compliance figure. */
    public long countDistinctEquipmentReportedOn(LocalDate date) {
        return em.createQuery(
                "SELECT COUNT(DISTINCT s.equipment) FROM StatusLog s WHERE s.logDate = :date", Long.class)
                .setParameter("date", date)
                .getSingleResult();
    }

    /** Latest status per equipment, grouped and counted — the fleet-wide status breakdown for the dashboard. */
    public List<Object[]> countByLatestStatus() {
        return em.createQuery(
                "SELECT s.status, COUNT(s) FROM StatusLog s "
                + "WHERE s.logDate = (SELECT MAX(s2.logDate) FROM StatusLog s2 WHERE s2.equipment = s.equipment) "
                + "GROUP BY s.status", Object[].class)
                .getResultList();
    }

    /** Latest status per equipment, scoped to a given (typically institution-owned) equipment list. */
    public List<StatusLog> findLatestForEquipmentIn(List<Equipment> equipmentList) {
        if (equipmentList.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                "SELECT s FROM StatusLog s WHERE s.equipment IN :equipment "
                + "AND s.logDate = (SELECT MAX(s2.logDate) FROM StatusLog s2 WHERE s2.equipment = s.equipment)",
                StatusLog.class)
                .setParameter("equipment", equipmentList)
                .getResultList();
    }

    /** Latest status log for every machine currently NOT functioning — the "needs attention" list. */
    public List<StatusLog> findLatestNonFunctioning() {
        return em.createQuery(
                "SELECT s FROM StatusLog s "
                + "WHERE s.status <> lk.gov.health.ums.enums.MachineStatus.FUNCTIONING "
                + "AND s.logDate = (SELECT MAX(s2.logDate) FROM StatusLog s2 WHERE s2.equipment = s.equipment) "
                + "ORDER BY s.logDate ASC", StatusLog.class)
                .getResultList();
    }

}
