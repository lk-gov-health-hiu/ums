package lk.gov.health.ums.facade;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 * Generic JPA CRUD/JPQL facade, ported from dmis/fmis's AbstractFacade and
 * trimmed to the methods actually used here (see §2 of the architecture
 * doc — "port as-is"). Every entity gets a thin subclass, e.g.:
 *
 * <pre>{@code
 * @Stateless
 * public class InstitutionFacade extends AbstractFacade<Institution> {
 *     @PersistenceContext(unitName = "umsPU")
 *     private EntityManager em;
 *     public InstitutionFacade() { super(Institution.class); }
 *     @Override protected EntityManager getEntityManager() { return em; }
 * }
 * }</pre>
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 * @param <T> the entity type
 */
public abstract class AbstractFacade<T> {

    private final Class<T> entityClass;

    protected AbstractFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract EntityManager getEntityManager();

    public void create(T entity) {
        getEntityManager().persist(entity);
    }

    public T edit(T entity) {
        return getEntityManager().merge(entity);
    }

    public void remove(T entity) {
        EntityManager em = getEntityManager();
        em.remove(em.merge(entity));
    }

    public T find(Object id) {
        return getEntityManager().find(entityClass, id);
    }

    public List<T> findAll() {
        return getEntityManager()
                .createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                .getResultList();
    }

    public List<T> findRange(int firstResult, int maxResults) {
        return getEntityManager()
                .createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    public int count() {
        return getEntityManager()
                .createQuery("SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class)
                .getSingleResult()
                .intValue();
    }

    public List<T> findByJpql(String jpql, Map<String, Object> parameters) {
        TypedQuery<T> query = getEntityManager().createQuery(jpql, entityClass);
        parameters.forEach(query::setParameter);
        return query.getResultList();
    }

    public T findFirstByJpql(String jpql, Map<String, Object> parameters) {
        List<T> results = findByJpql(jpql, parameters);
        return results.isEmpty() ? null : results.get(0);
    }

}
