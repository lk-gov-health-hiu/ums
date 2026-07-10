package lk.gov.health.ums.facade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import lk.gov.health.ums.entity.Equipment;
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

    /** Active equipment with no StatusLog row at all -- never reported, distinct from "reported and down". */
    public List<Equipment> findNeverReported() {
        return em.createQuery(
                "SELECT e FROM Equipment e WHERE e.retired = false "
                + "AND e NOT IN (SELECT DISTINCT s.equipment FROM StatusLog s)", Equipment.class)
                .getResultList();
    }

}
