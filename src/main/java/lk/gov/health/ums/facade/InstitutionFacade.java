package lk.gov.health.ums.facade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.gov.health.ums.entity.Institution;

@Stateless
public class InstitutionFacade extends AbstractFacade<Institution> {

    @PersistenceContext(unitName = "umsPU")
    private EntityManager em;

    public InstitutionFacade() {
        super(Institution.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

}
