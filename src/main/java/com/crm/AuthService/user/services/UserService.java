package com.crm.AuthService.user.services;

import com.crm.AuthService.user.dtos.CreateUserRequest;
import com.crm.AuthService.user.dtos.UpdateUserRequest;
import com.crm.AuthService.user.dtos.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface UserService {
    Page<UserResponse> getAllUsers(Pageable pageable, String search);
    UserResponse getUserById(Long id);
    UserResponse createUser(CreateUserRequest request);
    UserResponse updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
    UserResponse activateUser(Long id);
    UserResponse deactivateUser(Long id);
    UserResponse assignRoles(Long id, Set<String> roleNames);
}