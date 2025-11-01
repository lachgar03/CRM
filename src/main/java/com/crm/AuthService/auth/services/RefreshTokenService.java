package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.AuthResponse;
import com.crm.AuthService.exception.InvalidTokenException;
import com.crm.AuthService.exception.UserNotFoundException;


public interface RefreshTokenService {
    AuthResponse refreshToken(String refreshToken) throws InvalidTokenException, UserNotFoundException;

}
