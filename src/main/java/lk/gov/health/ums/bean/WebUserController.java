package lk.gov.health.ums.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.entity.WebUser;
import lk.gov.health.ums.enums.UserRole;
import lk.gov.health.ums.facade.InstitutionFacade;
import lk.gov.health.ums.facade.WebUserFacade;

/**
 * User account admin screen — scoped per §5: System Admin manages any
 * account; Institution Admin only manages INSTITUTION_ADMIN/INSTITUTION_USER
 * accounts at their own institution. Password is a transient field on this
 * bean, hashed via PasswordUtil before it ever reaches WebUser.passwordHash —
 * the entity never holds a plaintext password.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Named
@ViewScoped
public class WebUserController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private WebUserFacade webUserFacade;
    @Inject
    private InstitutionFacade institutionFacade;
    @Inject
    private SessionController sessionController;

    private List<WebUser> users;
    private List<Institution> institutions;
    private WebUser current;
    private String newPassword;

    @PostConstruct
    public void init() {
        if (sessionController.isSystemAdmin()) {
            users = webUserFacade.findAll();
            institutions = institutionFacade.findAll();
        } else {
            Institution own = sessionController.getScopeInstitution();
            users = own != null ? webUserFacade.findByJpql(
                    "SELECT u FROM WebUser u WHERE u.institution = :institution",
                    java.util.Map.of("institution", own)) : List.of();
            institutions = own != null ? List.of(own) : List.of();
        }
    }

    public void prepareNew() {
        current = new WebUser();
        newPassword = null;
        if (!sessionController.isSystemAdmin()) {
            current.setInstitution(sessionController.getScopeInstitution());
            current.setRole(UserRole.INSTITUTION_USER);
        }
    }

    public void save() {
        if (newPassword != null && !newPassword.isBlank()) {
            current.setPasswordHash(PasswordUtil.hash(newPassword));
        }
        if (current.getCreatedAt() == null) {
            current.setCreatedBy(sessionController.getWebUser());
            current.setCreatedAt(LocalDateTime.now());
            webUserFacade.create(current);
        } else {
            current.setLastEditBy(sessionController.getWebUser());
            current.setLastEditAt(LocalDateTime.now());
            webUserFacade.edit(current);
        }
        newPassword = null;
        init();
    }

    public void retire() {
        current.setRetired(true);
        current.setRetiredBy(sessionController.getWebUser());
        current.setRetiredAt(LocalDateTime.now());
        webUserFacade.edit(current);
        init();
    }

    /** Institution Admins may only grant institutional roles, not national/system ones. */
    public UserRole[] getAssignableRoles() {
        if (sessionController.isSystemAdmin()) {
            return UserRole.values();
        }
        return new UserRole[]{UserRole.INSTITUTION_ADMIN, UserRole.INSTITUTION_USER};
    }

    public List<WebUser> getUsers() {
        return users;
    }

    public List<Institution> getInstitutions() {
        return institutions;
    }

    public WebUser getCurrent() {
        return current;
    }

    public void setCurrent(WebUser current) {
        this.current = current;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

}
