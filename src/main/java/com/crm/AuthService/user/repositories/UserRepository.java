package com.crm.AuthService.user.repositories;

import com.crm.AuthService.user.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}
