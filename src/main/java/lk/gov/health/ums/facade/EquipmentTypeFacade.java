package lk.gov.health.ums.facade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.gov.health.ums.entity.EquipmentType;

@Stateless
public class EquipmentTypeFacade extends AbstractFacade<EquipmentType> {

    @PersistenceContext(unitName = "umsPU")
    private EntityManager em;

    public EquipmentTypeFacade() {
        super(EquipmentType.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

}
