package lk.gov.health.ums.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lk.gov.health.ums.enums.UserRole;

/**
 * RBAC model simplified from dmis/fmis's larger role lists down to the four
 * roles actually needed here — see §5 of the architecture doc.
 * {@code institution} is null for SYSTEM_ADMIN/NATIONAL_USER, and required
 * for INSTITUTION_ADMIN/INSTITUTION_USER, whose visibility is scoped to that
 * institution's subtree.
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
@Entity
@Table(name = "web_user", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class WebUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "username")
    private String username;
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "display_name")
    private String displayName;
    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole role;

    @ManyToOne
    @JoinColumn(name = "institution_id")
    private Institution institution;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    @Override
    public String toString() {
        return displayName != null ? displayName : username;
    }

}
