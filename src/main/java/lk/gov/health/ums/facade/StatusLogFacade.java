package lk.gov.health.ums.facade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import lk.gov.health.ums.entity.Equipment;
import lk.gov.health.ums.entity.EquipmentType;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.entity.StatusLog;
import lk.gov.health.ums.enums.MachineStatus;

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

    /** Day-by-status submission counts since a given date — feeds the reporting-trend sparkline. */
    public List<Object[]> countByDateAndStatusSince(LocalDate since) {
        return em.createQuery(
                "SELECT s.logDate, s.status, COUNT(s) FROM StatusLog s "
                + "WHERE s.logDate >= :since GROUP BY s.logDate, s.status", Object[].class)
                .setParameter("since", since)
                .getResultList();
    }

    /** Same as {@link #countByDateAndStatusSince}, scoped to a given (institution-owned) equipment list. */
    public List<Object[]> countByDateAndStatusSince(LocalDate since, List<Equipment> equipmentList) {
        if (equipmentList.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                "SELECT s.logDate, s.status, COUNT(s) FROM StatusLog s "
                + "WHERE s.logDate >= :since AND s.equipment IN :equipment GROUP BY s.logDate, s.status",
                Object[].class)
                .setParameter("since", since)
                .setParameter("equipment", equipmentList)
                .getResultList();
    }

    /**
     * Distinct equipment reported on the given day, grouped by equipment type and
     * optionally scoped to one hospital — the national dashboard's "total by
     * equipment" summary (whole fleet, or one hospital's fleet).
     */
    public List<Object[]> countReportedByType(LocalDate date, Institution institution) {
        String jpql = "SELECT s.equipment.type, COUNT(DISTINCT s.equipment) FROM StatusLog s "
                + "WHERE s.logDate = :date"
                + (institution != null ? " AND s.equipment.institution = :institution" : "")
                + " GROUP BY s.equipment.type";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class).setParameter("date", date);
        if (institution != null) {
            query.setParameter("institution", institution);
        }
        return query.getResultList();
    }

    /**
     * Distinct equipment reported on the given day, grouped by hospital and
     * optionally scoped to one equipment type — the national dashboard's
     * "total by hospital" summary when a modality filter is active.
     */
    public List<Object[]> countReportedByInstitution(LocalDate date, EquipmentType type) {
        String jpql = "SELECT s.equipment.institution, COUNT(DISTINCT s.equipment) FROM StatusLog s "
                + "WHERE s.logDate = :date"
                + (type != null ? " AND s.equipment.type = :type" : "")
                + " GROUP BY s.equipment.institution";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class).setParameter("date", date);
        if (type != null) {
            query.setParameter("type", type);
        }
        return query.getResultList();
    }

    /**
     * Sum of procedure counts (scans/studies) for the given day, grouped by equipment type and
     * optionally scoped to one hospital — feeds the dashboard's "scans by type" chart.
     */
    public List<Object[]> sumProcedureCountByType(LocalDate date, Institution institution) {
        String jpql = "SELECT s.equipment.type, COALESCE(SUM(s.procedureCount), 0) FROM StatusLog s "
                + "WHERE s.logDate = :date"
                + (institution != null ? " AND s.equipment.institution = :institution" : "")
                + " GROUP BY s.equipment.type";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class).setParameter("date", date);
        if (institution != null) {
            query.setParameter("institution", institution);
        }
        return query.getResultList();
    }

    /** Same as {@link #sumProcedureCountByType}, grouped by hospital and optionally scoped to one equipment type. */
    public List<Object[]> sumProcedureCountByInstitution(LocalDate date, EquipmentType type) {
        String jpql = "SELECT s.equipment.institution, COALESCE(SUM(s.procedureCount), 0) FROM StatusLog s "
                + "WHERE s.logDate = :date"
                + (type != null ? " AND s.equipment.type = :type" : "")
                + " GROUP BY s.equipment.institution";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class).setParameter("date", date);
        if (type != null) {
            query.setParameter("type", type);
        }
        return query.getResultList();
    }

    /**
     * Daily procedure-count sums since a date, grouped by equipment type and optionally scoped to
     * one hospital — feeds the dashboard's scan-volume trend chart (bucketed into months in Java).
     */
    public List<Object[]> sumProcedureCountByDateAndType(LocalDate since, Institution institution) {
        String jpql = "SELECT s.logDate, s.equipment.type, COALESCE(SUM(s.procedureCount), 0) FROM StatusLog s "
                + "WHERE s.logDate >= :since"
                + (institution != null ? " AND s.equipment.institution = :institution" : "")
                + " GROUP BY s.logDate, s.equipment.type";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class).setParameter("since", since);
        if (institution != null) {
            query.setParameter("institution", institution);
        }
        return query.getResultList();
    }

    /** Same as {@link #sumProcedureCountByDateAndType}, grouped by hospital and optionally scoped to one equipment type. */
    public List<Object[]> sumProcedureCountByDateAndInstitution(LocalDate since, EquipmentType type) {
        String jpql = "SELECT s.logDate, s.equipment.institution, COALESCE(SUM(s.procedureCount), 0) FROM StatusLog s "
                + "WHERE s.logDate >= :since"
                + (type != null ? " AND s.equipment.type = :type" : "")
                + " GROUP BY s.logDate, s.equipment.institution";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class).setParameter("since", since);
        if (type != null) {
            query.setParameter("type", type);
        }
        return query.getResultList();
    }

    /**
     * Distinct equipment reported with a given status on the given day, optionally scoped to
     * one hospital and/or equipment type — feeds the dashboard's "functioning" KPI.
     */
    public long countReportedWithStatus(LocalDate date, MachineStatus status, Institution institution, EquipmentType type) {
        String jpql = "SELECT COUNT(DISTINCT s.equipment) FROM StatusLog s "
                + "WHERE s.logDate = :date AND s.status = :status"
                + (institution != null ? " AND s.equipment.institution = :institution" : "")
                + (type != null ? " AND s.equipment.type = :type" : "");
        TypedQuery<Long> query = em.createQuery(jpql, Long.class)
                .setParameter("date", date)
                .setParameter("status", status);
        if (institution != null) {
            query.setParameter("institution", institution);
        }
        if (type != null) {
            query.setParameter("type", type);
        }
        return query.getSingleResult();
    }

    /**
     * Count of {@link #findLatestNonFunctioning}, optionally scoped to one hospital and/or
     * equipment type — feeds the dashboard's "needs attention" KPI (current fleet state,
     * independent of the date filter).
     */
    public long countLatestNonFunctioning(Institution institution, EquipmentType type) {
        String jpql = "SELECT COUNT(s) FROM StatusLog s "
                + "WHERE s.status <> lk.gov.health.ums.enums.MachineStatus.FUNCTIONING "
                + "AND s.logDate = (SELECT MAX(s2.logDate) FROM StatusLog s2 WHERE s2.equipment = s.equipment)"
                + (institution != null ? " AND s.equipment.institution = :institution" : "")
                + (type != null ? " AND s.equipment.type = :type" : "");
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        if (institution != null) {
            query.setParameter("institution", institution);
        }
        if (type != null) {
            query.setParameter("type", type);
        }
        return query.getSingleResult();
    }

    /** Every submission for one specific day — the trend sparkline's drill-in detail. */
    public List<StatusLog> findByDate(LocalDate date) {
        return em.createQuery(
                "SELECT s FROM StatusLog s WHERE s.logDate = :date ORDER BY s.status", StatusLog.class)
                .setParameter("date", date)
                .getResultList();
    }

    /** Same as {@link #findByDate}, scoped to a given (institution-owned) equipment list. */
    public List<StatusLog> findByDate(LocalDate date, List<Equipment> equipmentList) {
        if (equipmentList.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                "SELECT s FROM StatusLog s WHERE s.logDate = :date AND s.equipment IN :equipment ORDER BY s.status",
                StatusLog.class)
                .setParameter("date", date)
                .setParameter("equipment", equipmentList)
                .getResultList();
    }

}
