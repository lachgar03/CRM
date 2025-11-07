package com.crm.AuthService.user.controllers;


import com.crm.AuthService.annotations.RequirePermission;
import com.crm.AuthService.user.dtos.CreateUserRequest;
import com.crm.AuthService.user.dtos.UpdateUserRequest;
import com.crm.AuthService.user.dtos.UserResponse;
import com.crm.AuthService.user.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.Set;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    UserService userService;


    @PostMapping
    @RequirePermission(resource = "USER" ,action = "CREATE" )
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        UserResponse userResponse = userService.createUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }
    @RequirePermission(resource = "USER" ,action = "READ" )
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable,@RequestParam(required = false) String search) {
       return ResponseEntity.ok(userService.getAllUsers(pageable,search)) ;

    }
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id){
        return ResponseEntity.ok(userService.getUserById(id));
    }
    @PutMapping("/{id}")
    @RequirePermission(resource = "USER", action = "UPDATE")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));

    }
    @DeleteMapping("/{id}")
    @RequirePermission(resource = "USER", action = "DELETE")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/assign-roles")
    @RequirePermission(resource = "USER", action = "ASSIGN_ROLE")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable Long id, @RequestBody Set<String> roleNames) {
        return ResponseEntity.ok(userService.assignRoles(id, roleNames));
    }
    @PostMapping("/{id}/activate")
    @RequirePermission(resource = "USER", action = "UPDATE")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }


    @PostMapping("/{id}/deactivate")
    @RequirePermission(resource = "USER", action = "UPDATE")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

}
