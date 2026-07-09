package lk.gov.health.ums.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import lk.gov.health.ums.entity.Institution;
import lk.gov.health.ums.entity.WebUser;
import lk.gov.health.ums.enums.UserRole;
import lk.gov.health.ums.facade.WebUserFacade;

/**
 * Holds the logged-in WebUser for the session and the role checks JSF pages
 * use to show/hide admin actions — the same manual-boolean-getter pattern
 * dmis/fmis use (no Spring Security / Shiro), simplified to the four roles
 * in §5 of the architecture doc.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Named
@SessionScoped
public class SessionController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private WebUserFacade webUserFacade;

    private WebUser webUser;
    private String username;
    private String password;

    public String login() {
        WebUser candidate = webUserFacade.findByUsername(username);
        if (candidate != null && PasswordUtil.matches(password, candidate.getPasswordHash())) {
            this.webUser = candidate;
            this.password = null;
            return "/admin/index.xhtml?faces-redirect=true";
        }
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login failed", "Incorrect username or password."));
        return null;
    }

    public String logout() {
        webUser = null;
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/admin/login.xhtml?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return webUser != null;
    }

    public boolean isSystemAdmin() {
        return isLoggedIn() && webUser.getRole() == UserRole.SYSTEM_ADMIN;
    }

    public boolean isNationalUser() {
        return isLoggedIn() && webUser.getRole() == UserRole.NATIONAL_USER;
    }

    public boolean isInstitutionAdmin() {
        return isLoggedIn() && webUser.getRole() == UserRole.INSTITUTION_ADMIN;
    }

    public boolean isInstitutionUser() {
        return isLoggedIn() && webUser.getRole() == UserRole.INSTITUTION_USER;
    }

    /** National-level roles manage master data (institutions, equipment types); institutional roles don't. */
    public boolean canManageMasterData() {
        return isSystemAdmin() || isNationalUser();
    }

    /** Non-national roles are locked to their own institution's subtree — mirrors dmis's isRestrictedToInstitution(). */
    public boolean isRestrictedToInstitution() {
        return isInstitutionAdmin() || isInstitutionUser();
    }

    public Institution getScopeInstitution() {
        return isLoggedIn() ? webUser.getInstitution() : null;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
