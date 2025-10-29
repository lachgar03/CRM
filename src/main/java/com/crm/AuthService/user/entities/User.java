package com.crm.AuthService.user.entities;

import com.crm.AuthService.role.entities.Permission;
import com.crm.AuthService.role.entities.Role;
import com.crm.AuthService.tenant.entities.Tenant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data

public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    @NotNull
    private String username;
    @NotNull
    private String password;
    @NotNull
    private String email;
    private String firstName;
    private String lastName;
    private String status;

    @ManyToMany
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles=new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (this.roles != null && !this.roles.isEmpty()) {
            for (Role role : this.roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

                if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
                    for (Permission permission : role.getPermissions()) {
                        String authority = permission.getResource() + ":" + permission.getAction();
                        authorities.add(new SimpleGrantedAuthority(authority));
                    }
                }
            }
        }

        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }


}
