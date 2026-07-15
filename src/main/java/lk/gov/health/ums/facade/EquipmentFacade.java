package lk.gov.health.ums.facade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import lk.gov.health.ums.entity.Equipment;
import lk.gov.health.ums.entity.EquipmentType;
import lk.gov.health.ums.entity.Institution;

@Stateless
public class EquipmentFacade extends AbstractFacade<Equipment> {

    @PersistenceContext(unitName = "umsPU")
    private EntityManager em;

    public EquipmentFacade() {
        super(Equipment.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Equipment> findActiveByInstitution(Institution institution) {
        return em.createQuery(
                "SELECT e FROM Equipment e WHERE e.institution = :institution AND e.retired = false", Equipment.class)
                .setParameter("institution", institution)
                .getResultList();
    }

    public long countActive() {
        return em.createQuery("SELECT COUNT(e) FROM Equipment e WHERE e.retired = false", Long.class)
                .getSingleResult();
    }

    /** Active equipment, optionally scoped to one hospital and/or one equipment type — the dashboard's "tracked" KPI. */
    public long countActive(Institution institution, EquipmentType type) {
        String jpql = "SELECT COUNT(e) FROM Equipment e WHERE e.retired = false"
                + (institution != null ? " AND e.institution = :institution" : "")
                + (type != null ? " AND e.type = :type" : "");
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        if (institution != null) {
            query.setParameter("institution", institution);
        }
        if (type != null) {
            query.setParameter("type", type);
        }
        return query.getSingleResult();
    }

    /** Active equipment counts grouped by type, optionally scoped to one hospital — feeds the dashboard's equipment-by-type chart. */
    public List<Object[]> countActiveByType(Institution institution) {
        String jpql = "SELECT e.type, COUNT(e) FROM Equipment e WHERE e.retired = false"
                + (institution != null ? " AND e.institution = :institution" : "")
                + " GROUP BY e.type";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        if (institution != null) {
            query.setParameter("institution", institution);
        }
        return query.getResultList();
    }

    /** Active equipment counts grouped by hospital, optionally scoped to one equipment type — feeds the dashboard's equipment-by-hospital chart. */
    public List<Object[]> countActiveByInstitution(EquipmentType type) {
        String jpql = "SELECT e.institution, COUNT(e) FROM Equipment e WHERE e.retired = false"
                + (type != null ? " AND e.type = :type" : "")
                + " GROUP BY e.institution";
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        if (type != null) {
            query.setParameter("type", type);
        }
        return query.getResultList();
    }

    /** Active equipment with no StatusLog row at all -- never reported, distinct from "reported and down". */
    public List<Equipment> findNeverReported() {
        return em.createQuery(
                "SELECT e FROM Equipment e WHERE e.retired = false "
                + "AND e NOT IN (SELECT DISTINCT s.equipment FROM StatusLog s)", Equipment.class)
                .getResultList();
    }

    /** Same as {@link #findNeverReported}, counted and optionally scoped to one hospital and/or equipment type. */
    public long countNeverReported(Institution institution, EquipmentType type) {
        String jpql = "SELECT COUNT(e) FROM Equipment e WHERE e.retired = false "
                + "AND e NOT IN (SELECT DISTINCT s.equipment FROM StatusLog s)"
                + (institution != null ? " AND e.institution = :institution" : "")
                + (type != null ? " AND e.type = :type" : "");
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        if (institution != null) {
            query.setParameter("institution", institution);
        }
        if (type != null) {
            query.setParameter("type", type);
        }
        return query.getSingleResult();
    }

}
