package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.AuthResponse;
import com.crm.AuthService.exception.InvalidTokenException;
import com.crm.AuthService.exception.UserNotFoundException;
import com.crm.AuthService.security.JwtService;
import com.crm.AuthService.user.entities.User;
import com.crm.AuthService.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JwtService jwtService;
    private final AuthHelper authHelper;
    private final UserRepository userRepository;

    @Override
    public AuthResponse refreshToken(String refreshToken) throws InvalidTokenException, UserNotFoundException {
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new InvalidTokenException("Refresh token invalide ou expiré");
        }
        authHelper.validateUserAndTenantStatus(user);
        String newAccessToken = jwtService.generateToken(user, user.getTenant().getId());
        return authHelper.buildAuthResponse(user, newAccessToken, refreshToken);

    }
}
