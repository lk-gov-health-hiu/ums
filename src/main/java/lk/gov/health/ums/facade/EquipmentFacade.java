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

}
