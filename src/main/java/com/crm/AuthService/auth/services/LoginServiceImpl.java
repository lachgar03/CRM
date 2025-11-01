package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.AuthResponse;
import com.crm.AuthService.auth.dtos.LoginRequest;
import com.crm.AuthService.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private final AuthHelper authHelper;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) throws BadCredentialsException, DisabledException , LockedException {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )

            );
            User user = (User) authentication.getPrincipal();
            authHelper.validateUserAndTenantStatus(user);
            return authHelper.buildAuthResponse(user);

        }catch (BadCredentialsException e) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        } catch (DisabledException e) {
            throw new DisabledException("Compte utilisateur désactivé");
        } catch (LockedException e) {
            throw new LockedException("Compte utilisateur verrouillé");
        }

    }
}
