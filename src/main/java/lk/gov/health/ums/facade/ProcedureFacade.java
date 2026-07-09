package lk.gov.health.ums.facade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.gov.health.ums.entity.Procedure;

@Stateless
public class ProcedureFacade extends AbstractFacade<Procedure> {

    @PersistenceContext(unitName = "umsPU")
    private EntityManager em;

    public ProcedureFacade() {
        super(Procedure.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

}
