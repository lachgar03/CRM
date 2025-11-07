package com.crm.AuthService.role.entities;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(
        name = "permissions",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"resource", "action"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Use IDENTITY (not AUTO)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String description;


    @ManyToMany(mappedBy = "permissions")
    @JsonIgnore
    @Builder.Default
    private Set<Role> roles = new HashSet<>();


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }



    public boolean matches(String resource, String action) {
        return this.resource.equalsIgnoreCase(resource)
                && this.action.equalsIgnoreCase(action);
    }

    public String getFullName() {
        return resource + ":" + action;
    }


    public String getPermissionName() {
        return resource.toUpperCase() + "_" + action.toUpperCase();
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", resource='" + resource + '\'' +
                ", action='" + action + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission)) return false;
        Permission that = (Permission) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}