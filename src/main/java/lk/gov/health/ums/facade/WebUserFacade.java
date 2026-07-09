package lk.gov.health.ums.facade;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lk.gov.health.ums.entity.WebUser;

@Stateless
public class WebUserFacade extends AbstractFacade<WebUser> {

    @PersistenceContext(unitName = "umsPU")
    private EntityManager em;

    public WebUserFacade() {
        super(WebUser.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public WebUser findByUsername(String username) {
        try {
            return em.createQuery(
                    "SELECT u FROM WebUser u WHERE u.username = :username AND u.retired = false", WebUser.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

}
