package com.gray.singifyback.unit;

import com.gray.singifyback.dto.request.LoginRequest;
import com.gray.singifyback.dto.request.RegisterRequest;
import com.gray.singifyback.dto.response.AuthResponse;
import com.gray.singifyback.model.User;
import com.gray.singifyback.repository.UserRepository;
import com.gray.singifyback.security.JwtUtil;
import com.gray.singifyback.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    @Test
    void register_success_returnsToken() {
        RegisterRequest req = new RegisterRequest("new@example.com", "newuser", "secret99");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(userRepository.existsByUsername(req.username())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        User saved = new User();
        saved.setEmail(req.email());
        saved.setUsername(req.username());
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtUtil.generateToken(req.email())).thenReturn("tok");

        AuthResponse res = authService.register(req);

        assertThat(res.token()).isEqualTo("tok");
        assertThat(res.email()).isEqualTo(req.email());
        assertThat(res.username()).isEqualTo(req.username());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest("dup@example.com", "anyuser", "secret99");
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateUsername_throws() {
        RegisterRequest req = new RegisterRequest("fresh@example.com", "taken", "secret99");
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(userRepository.existsByUsername(req.username())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest req = new LoginRequest("user@example.com", "pass");
        User user = new User();
        user.setEmail(req.email());
        user.setUsername("someone");
        when(userRepository.findByEmail(req.email())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(req.email())).thenReturn("jwt");

        AuthResponse res = authService.login(req);

        assertThat(res.token()).isEqualTo("jwt");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_wrongPassword_propagatesException() {
        LoginRequest req = new LoginRequest("user@example.com", "wrong");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
