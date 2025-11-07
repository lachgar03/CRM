package com.crm.AuthService.user.dtos;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {

    private String firstName;

    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    private Boolean enabled;

    private Set<String> roleNames;}