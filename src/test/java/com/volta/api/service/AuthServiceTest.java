package com.volta.api.service;

import com.volta.api.database.entity.Users;
import com.volta.api.database.repository.UserRepository;
import com.volta.api.dto.auth.LoginRequestDTO;
import com.volta.api.dto.auth.TokenResponseDTO;
import com.volta.api.security.AuthenticatedUser;
import com.volta.api.security.jwt.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private Users stubAuthenticatedUser(Authentication authenticated, Boolean firstLogin) {
        UUID id = UUID.randomUUID();
        Users user = Users.builder().id(id).firstLogin(firstLogin).build();
        when(authenticated.getPrincipal())
                .thenReturn(new AuthenticatedUser(id, "ana@volta.com", null, UUID.randomUUID(), "EMPLOYEE"));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    @DisplayName("login no primeiro acesso devolve firstLogin true e mantém o usuário marcado")
    void login_firstLogin_returnsTrueAndKeepsFlag() {
        Authentication authenticated = mock(Authentication.class);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(tokenProvider.generateToken(authenticated)).thenReturn("fake-token");
        Users user = stubAuthenticatedUser(authenticated, true);

        TokenResponseDTO response = authService.login(new LoginRequestDTO("ana@volta.com", "password123"));

        assertThat(response.firstLogin()).isTrue();
        assertThat(user.getFirstLogin()).isTrue();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login depois do primeiro acesso devolve firstLogin false sem salvar")
    void login_notFirstLogin_returnsFalseWithoutSaving() {
        Authentication authenticated = mock(Authentication.class);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(tokenProvider.generateToken(authenticated)).thenReturn("fake-token");
        stubAuthenticatedUser(authenticated, false);

        TokenResponseDTO response = authService.login(new LoginRequestDTO("ana@volta.com", "password123"));

        assertThat(response.firstLogin()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login com credenciais válidas devolve o token e a expiração")
    void login_validCredentials_returnsTokenAndExpiration() {
        Authentication authenticated = mock(Authentication.class);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(tokenProvider.generateToken(authenticated)).thenReturn("fake-token");
        when(tokenProvider.getExpirationTime()).thenReturn(3_600_000L);
        stubAuthenticatedUser(authenticated, false);

        TokenResponseDTO response = authService.login(new LoginRequestDTO("ana@volta.com", "password123"));

        assertThat(response.token()).isEqualTo("fake-token");
        assertThat(response.expiresIn()).isEqualTo(3_600_000L);

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("ana@volta.com");
        assertThat(captor.getValue().getCredentials()).isEqualTo("password123");
    }

    @Test
    @DisplayName("login com senha errada propaga a exceção e não gera token")
    void login_wrongPassword_propagatesExceptionAndDoesNotGenerateToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("ana@volta.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(tokenProvider);
    }
}
