package com.project.back_end.services;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TokenService#validateToken(String, String)}.
 * Covers the "extracted email is looked up in the correct repository" fix
 * (previously an undefined variable `id` was referenced instead of `extracted`,
 * which would not compile / was a typo bug).
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    private TokenService tokenService;

    private static final String EMAIL = "user@example.com";
    private String token;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(adminRepository, doctorRepository, patientRepository);
        // @Value("${jwt.secret}") is not populated outside a Spring context; set it directly.
        // Needs to be reasonably long for HMAC-SHA signing.
        ReflectionTestUtils.setField(tokenService, "secret",
                "test-secret-key-for-jwt-signing-must-be-long-enough-1234567890");
        token = tokenService.generateToken(EMAIL);
    }

    @Test
    void validateToken_returnsTrue_forKnownAdmin() {
        when(adminRepository.findByUsername(EMAIL)).thenReturn(new Admin());

        assertThat(tokenService.validateToken(token, "admin")).isTrue();
    }

    @Test
    void validateToken_returnsTrue_forKnownDoctor() {
        when(doctorRepository.findByEmail(EMAIL)).thenReturn(new Doctor());

        assertThat(tokenService.validateToken(token, "doctor")).isTrue();
    }

    @Test
    void validateToken_returnsTrue_forKnownPatient() {
        when(patientRepository.findByEmail(EMAIL)).thenReturn(new Patient());

        assertThat(tokenService.validateToken(token, "patient")).isTrue();
    }

    @Test
    void validateToken_returnsFalse_whenUserNotFoundForRole() {
        when(doctorRepository.findByEmail(EMAIL)).thenReturn(null);

        assertThat(tokenService.validateToken(token, "doctor")).isFalse();
    }

    @Test
    void validateToken_returnsFalse_forUnknownRole() {
        assertThat(tokenService.validateToken(token, "superadmin")).isFalse();
    }

    @Test
    void validateToken_returnsFalse_forMalformedToken() {
        assertThat(tokenService.validateToken("not-a-real-token", "admin")).isFalse();
    }
}
