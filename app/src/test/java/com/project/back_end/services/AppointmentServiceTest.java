package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AppointmentService}. All repositories/collaborators are mocked
 * so these tests run without a real MySQL database.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private SharedService sharedService;

    @Mock
    private TokenService tokenService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    private AppointmentService appointmentService;

    private static final String TOKEN = "some.jwt.token";

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(
                appointmentRepository, sharedService, tokenService, patientRepository, doctorRepository);
    }

    private Doctor doctorWithId(Long id) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setName("Dr. House");
        return doctor;
    }

    private Patient patientWithId(Long id, String name) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setName(name);
        patient.setEmail("patient@example.com");
        patient.setPhone("1234567890");
        patient.setAddress("123 Main St");
        return patient;
    }

    private Appointment appointment(Long id, Doctor doctor, Patient patient) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentTime(LocalDateTime.of(2026, 8, 20, 9, 0));
        appointment.setStatus(0);
        return appointment;
    }

    // ----------------------------------------------------------------
    // getAppointment
    // ----------------------------------------------------------------

    @Test
    void getAppointment_returnsAppointmentsForDoctor_whenNoNameFilterGiven() {
        Doctor doctor = doctorWithId(1L);
        Patient patient = patientWithId(2L, "John Smith");
        Appointment appt = appointment(10L, doctor, patient);

        when(tokenService.extractEmail(TOKEN)).thenReturn("doctor@example.com");
        when(doctorRepository.findByEmail("doctor@example.com")).thenReturn(doctor);
        when(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of(appt));

        Map<String, Object> result = appointmentService.getAppointment(null, LocalDate.of(2026, 8, 20), TOKEN);

        assertThat(result).doesNotContainKey("error");
        assertThat(result.get("appointments")).isInstanceOf(List.class);
        List<?> appointments = (List<?>) result.get("appointments");
        assertThat(appointments).hasSize(1);
        verify(appointmentRepository, never())
                .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                        anyLong(), anyString(), any(), any());
    }

    @Test
    void getAppointment_treatsLiteralStringNull_asNoFilter() {
        Doctor doctor = doctorWithId(1L);
        when(tokenService.extractEmail(TOKEN)).thenReturn("doctor@example.com");
        when(doctorRepository.findByEmail("doctor@example.com")).thenReturn(doctor);
        when(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        Map<String, Object> result = appointmentService.getAppointment("null", LocalDate.of(2026, 8, 20), TOKEN);

        assertThat(result.get("appointments")).isEqualTo(List.of());
        verify(appointmentRepository).findByDoctorIdAndAppointmentTimeBetween(eq(1L), any(), any());
        verify(appointmentRepository, never())
                .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                        anyLong(), anyString(), any(), any());
    }

    @Test
    void getAppointment_filtersByPatientName_whenNameProvided() {
        Doctor doctor = doctorWithId(1L);
        Patient patient = patientWithId(2L, "Jane Doe");
        Appointment appt = appointment(11L, doctor, patient);

        when(tokenService.extractEmail(TOKEN)).thenReturn("doctor@example.com");
        when(doctorRepository.findByEmail("doctor@example.com")).thenReturn(doctor);
        when(appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                eq(1L), eq("jane"), any(), any())).thenReturn(List.of(appt));

        Map<String, Object> result = appointmentService.getAppointment("jane", LocalDate.of(2026, 8, 20), TOKEN);

        List<?> appointments = (List<?>) result.get("appointments");
        assertThat(appointments).hasSize(1);
        verify(appointmentRepository, never())
                .findByDoctorIdAndAppointmentTimeBetween(anyLong(), any(), any());
    }

    @Test
    void getAppointment_returnsInvalidTokenError_whenDoctorCannotBeResolved() {
        when(tokenService.extractEmail(TOKEN)).thenReturn("unknown@example.com");
        when(doctorRepository.findByEmail("unknown@example.com")).thenReturn(null);

        Map<String, Object> result = appointmentService.getAppointment(null, LocalDate.of(2026, 8, 20), TOKEN);

        assertThat(result.get("error")).isEqualTo("Invalid token");
        assertThat(result.get("appointments")).isEqualTo(List.of());
    }

    @Test
    void getAppointment_returnsInternalServerError_onException() {
        when(tokenService.extractEmail(TOKEN)).thenThrow(new RuntimeException("boom"));

        Map<String, Object> result = appointmentService.getAppointment(null, LocalDate.of(2026, 8, 20), TOKEN);

        assertThat(result.get("error")).isEqualTo("Internal Server Error");
    }

    // ----------------------------------------------------------------
    // cancelAppointment
    // ----------------------------------------------------------------

    @Test
    void cancelAppointment_deletesAppointment_whenCallerIsOwner() {
        Patient owner = patientWithId(2L, "John Smith");
        Appointment appt = appointment(10L, doctorWithId(1L), owner);

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));
        when(tokenService.extractEmail(TOKEN)).thenReturn("owner@example.com");
        when(patientRepository.findByEmail("owner@example.com")).thenReturn(owner);

        ResponseEntity<Map<String, String>> response = appointmentService.cancelAppointment(10L, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Appointment Deleted Successfully");
        verify(appointmentRepository, times(1)).delete(appt);
    }

    @Test
    void cancelAppointment_returnsNotFound_whenAppointmentDoesNotExist() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, String>> response = appointmentService.cancelAppointment(999L, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "No appointment with ID: 999");
        verify(appointmentRepository, never()).delete(any());
    }

    @Test
    void cancelAppointment_returnsBadRequest_whenCallerIsNotOwner() {
        Patient owner = patientWithId(2L, "John Smith");
        Patient otherPatient = patientWithId(3L, "Someone Else");
        Appointment appt = appointment(10L, doctorWithId(1L), owner);

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));
        when(tokenService.extractEmail(TOKEN)).thenReturn("other@example.com");
        when(patientRepository.findByEmail("other@example.com")).thenReturn(otherPatient);

        ResponseEntity<Map<String, String>> response = appointmentService.cancelAppointment(10L, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Patient ID Mismatch");
        verify(appointmentRepository, never()).delete(any());
    }

    @Test
    void cancelAppointment_returnsBadRequest_whenCallerCannotBeResolvedFromToken() {
        Patient owner = patientWithId(2L, "John Smith");
        Appointment appt = appointment(10L, doctorWithId(1L), owner);

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));
        when(tokenService.extractEmail(TOKEN)).thenReturn("ghost@example.com");
        when(patientRepository.findByEmail("ghost@example.com")).thenReturn(null);

        ResponseEntity<Map<String, String>> response = appointmentService.cancelAppointment(10L, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Patient ID Mismatch");
    }

    // ----------------------------------------------------------------
    // updateAppointment — Long comparison regression (was `!=`, now `.equals()`)
    // ----------------------------------------------------------------
    //
    // Ownership is now established from the token-resolved caller, compared
    // against the DB record's patient (never against the request body), so
    // these tests mock patientRepository.findByEmail/tokenService.extractEmail
    // to resolve the "caller" identity.

    @Test
    void updateAppointment_succeeds_whenCallerIdMatchesOwner_aboveLongCacheBoundary() {
        // Patient IDs above 127 fall outside the Long autoboxing cache (-128..127).
        // Two distinct Long instances holding the same value 200L would compare
        // unequal with `!=` (the old, buggy code) but equal with `.equals()`.
        Patient existingPatient = patientWithId(Long.valueOf(200), "Big Id Patient");
        Appointment existing = appointment(50L, doctorWithId(1L), existingPatient);

        Patient callerPatient = patientWithId(Long.valueOf(200), "Big Id Patient");
        Patient irrelevantBodyPatient = patientWithId(Long.valueOf(999), "Irrelevant Body Patient");
        Appointment updateRequest = appointment(50L, doctorWithId(1L), irrelevantBodyPatient);
        updateRequest.setAppointmentTime(existing.getAppointmentTime().plusHours(1));

        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(tokenService.extractEmail(TOKEN)).thenReturn("owner@example.com");
        when(patientRepository.findByEmail("owner@example.com")).thenReturn(callerPatient);
        when(sharedService.validateAppointment(updateRequest)).thenReturn(1);

        ResponseEntity<Map<String, String>> response = appointmentService.updateAppointment(updateRequest, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Appointment Updated Successfully");
        verify(appointmentRepository).save(existing);
        // Only the mutable time/status fields are copied onto the persisted record;
        // the body's (irrelevant) patient must never overwrite the real owner.
        assertThat(existing.getAppointmentTime()).isEqualTo(updateRequest.getAppointmentTime());
        assertThat(existing.getPatient()).isEqualTo(existingPatient);
    }

    @Test
    void updateAppointment_rejectsMismatch_whenCallerIsNotOwner_aboveLongCacheBoundary() {
        Patient existingPatient = patientWithId(Long.valueOf(200), "Original Patient");
        Appointment existing = appointment(51L, doctorWithId(1L), existingPatient);

        Patient callerPatient = patientWithId(Long.valueOf(201), "Different Patient");
        Appointment updateRequest = appointment(51L, doctorWithId(1L), existingPatient);

        when(appointmentRepository.findById(51L)).thenReturn(Optional.of(existing));
        when(tokenService.extractEmail(TOKEN)).thenReturn("other@example.com");
        when(patientRepository.findByEmail("other@example.com")).thenReturn(callerPatient);

        ResponseEntity<Map<String, String>> response = appointmentService.updateAppointment(updateRequest, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Patient ID Mismatch");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void updateAppointment_returnsNotFound_whenAppointmentDoesNotExist() {
        Appointment updateRequest = appointment(999L, doctorWithId(1L), patientWithId(1L, "Someone"));
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, String>> response = appointmentService.updateAppointment(updateRequest, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAppointment_returnsConflict_whenSlotUnavailable() {
        Patient patient = patientWithId(Long.valueOf(200), "Big Id Patient");
        Appointment existing = appointment(52L, doctorWithId(1L), patient);
        Appointment updateRequest = appointment(52L, doctorWithId(1L), patient);
        updateRequest.setAppointmentTime(existing.getAppointmentTime().plusHours(2));

        when(appointmentRepository.findById(52L)).thenReturn(Optional.of(existing));
        when(tokenService.extractEmail(TOKEN)).thenReturn("owner@example.com");
        when(patientRepository.findByEmail("owner@example.com")).thenReturn(patient);
        when(sharedService.validateAppointment(updateRequest)).thenReturn(0);

        ResponseEntity<Map<String, String>> response = appointmentService.updateAppointment(updateRequest, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(appointmentRepository, never()).save(any());
    }

    // ----------------------------------------------------------------
    // updateAppointment — Issue 1 fix: ownership must come from the token,
    // never from the request body.
    // ----------------------------------------------------------------

    @Test
    void updateAppointment_rejectsUpdate_whenCallerTokenDoesNotOwnAppointment_evenIfBodyClaimsVictimPatientId() {
        // Attack scenario: a logged-in attacker sends PUT /appointments/{herOwnValidToken}
        // with a body naming the VICTIM's appointment id and the VICTIM's patient id.
        // The old code compared the body against itself, so this always passed.
        // The fix must resolve the caller from the token and compare against the
        // DB record's patient, so this must be rejected regardless of the body.
        Patient victim = patientWithId(1L, "Victim");
        Appointment existing = appointment(70L, doctorWithId(1L), victim);

        Patient attacker = patientWithId(2L, "Attacker");
        Appointment updateRequest = appointment(70L, doctorWithId(1L), victim);

        when(appointmentRepository.findById(70L)).thenReturn(Optional.of(existing));
        when(tokenService.extractEmail(TOKEN)).thenReturn("attacker@example.com");
        when(patientRepository.findByEmail("attacker@example.com")).thenReturn(attacker);

        ResponseEntity<Map<String, String>> response = appointmentService.updateAppointment(updateRequest, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Patient ID Mismatch");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void updateAppointment_rejectsUpdate_whenCallerCannotBeResolvedFromToken() {
        Patient owner = patientWithId(1L, "Owner");
        Appointment existing = appointment(71L, doctorWithId(1L), owner);
        Appointment updateRequest = appointment(71L, doctorWithId(1L), owner);

        when(appointmentRepository.findById(71L)).thenReturn(Optional.of(existing));
        when(tokenService.extractEmail(TOKEN)).thenReturn("ghost@example.com");
        when(patientRepository.findByEmail("ghost@example.com")).thenReturn(null);

        ResponseEntity<Map<String, String>> response = appointmentService.updateAppointment(updateRequest, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Patient ID Mismatch");
        verify(appointmentRepository, never()).save(any());
    }

    // ----------------------------------------------------------------
    // updateAppointment — Issue 2 fix: a no-op time update must not be
    // rejected by availability validation.
    // ----------------------------------------------------------------

    @Test
    void updateAppointment_succeeds_whenTimeUnchanged_withoutRevalidatingAvailability() {
        Patient patient = patientWithId(2L, "John Smith");
        Appointment existing = appointment(60L, doctorWithId(1L), patient);
        Appointment updateRequest = appointment(60L, doctorWithId(1L), patient);
        updateRequest.setStatus(1); // only the status changes; time is identical

        when(appointmentRepository.findById(60L)).thenReturn(Optional.of(existing));
        when(tokenService.extractEmail(TOKEN)).thenReturn("owner@example.com");
        when(patientRepository.findByEmail("owner@example.com")).thenReturn(patient);

        ResponseEntity<Map<String, String>> response = appointmentService.updateAppointment(updateRequest, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Appointment Updated Successfully");
        verify(sharedService, never()).validateAppointment(any());
        verify(appointmentRepository).save(existing);
        assertThat(existing.getStatus()).isEqualTo(1);
    }

    // ----------------------------------------------------------------
    // updateAppointment — doctor for availability validation must come from
    // the persisted record, never from the request body.
    // ----------------------------------------------------------------

    @Test
    void updateAppointment_validatesAgainstPersistedDoctor_whenBodyNamesDifferentDoctor() {
        // Attack scenario: the caller genuinely owns an appointment with Dr. 1, but
        // sends a body naming Dr. 2 (found via the unauthenticated GET /doctor) and a
        // time that happens to be free on Dr. 2's schedule. Validation must still be
        // performed against Dr. 1 (the persisted doctor), since Dr. 1 is who the
        // appointment is actually saved against.
        Patient owner = patientWithId(2L, "John Smith");
        Doctor persistedDoctor = doctorWithId(1L);
        Doctor bodyDoctor = doctorWithId(999L);

        Appointment existing = appointment(80L, persistedDoctor, owner);
        Appointment updateRequest = appointment(80L, bodyDoctor, owner);
        updateRequest.setAppointmentTime(existing.getAppointmentTime().plusHours(3));

        when(appointmentRepository.findById(80L)).thenReturn(Optional.of(existing));
        when(tokenService.extractEmail(TOKEN)).thenReturn("owner@example.com");
        when(patientRepository.findByEmail("owner@example.com")).thenReturn(owner);
        when(sharedService.validateAppointment(any())).thenReturn(1);

        ResponseEntity<Map<String, String>> response = appointmentService.updateAppointment(updateRequest, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(sharedService).validateAppointment(captor.capture());
        assertThat(captor.getValue().getDoctor().getId()).isEqualTo(persistedDoctor.getId());

        verify(appointmentRepository).save(existing);
        assertThat(existing.getDoctor().getId()).isEqualTo(persistedDoctor.getId());
    }
}
