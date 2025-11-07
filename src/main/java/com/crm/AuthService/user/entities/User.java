package com.crm.AuthService.user.entities;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean accountNonExpired = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean credentialsNonExpired = true;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role_id")
    @Builder.Default
    private Set<Long> roleIds = new HashSet<>();


    @Transient
    private Long tenantId;

    @Transient
    private String tenantName;


    @Transient
    @Builder.Default
    private Set<String> roleNames = new HashSet<>();

    @Transient
    @Builder.Default
    private Set<String> permissions = new HashSet<>();

    @Transient
    private String tenantStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }


    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    public void addRole(Long roleId) {
        if (this.roleIds == null) {
            this.roleIds = new HashSet<>();
        }
        this.roleIds.add(roleId);
    }

    public void removeRole(Long roleId) {
        if (this.roleIds != null) {
            this.roleIds.remove(roleId);
        }
    }

    public boolean hasRole(String roleName) {
        return roleNames != null && roleNames.contains(roleName);
    }


    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", enabled=" + enabled +
                ", roleCount=" + (roleIds != null ? roleIds.size() : 0) +
                ", tenantId=" + tenantId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}