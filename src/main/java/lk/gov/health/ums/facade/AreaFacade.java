package lk.gov.health.ums.facade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.gov.health.ums.entity.Area;

@Stateless
public class AreaFacade extends AbstractFacade<Area> {

    @PersistenceContext(unitName = "umsPU")
    private EntityManager em;

    public AreaFacade() {
        super(Area.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

}
