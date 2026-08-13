package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DoctorService#getDoctorAvailability(Long, LocalDate)}.
 * The repositories are mocked so no real database (MySQL) is required.
 */
@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private DoctorRepository doctorRepository;

    private DoctorService doctorService;

    private static final Long DOCTOR_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 20);

    @BeforeEach
    void setUp() {
        doctorService = new DoctorService(appointmentRepository, tokenService, doctorRepository);
    }

    private Doctor doctorWithSlots(List<String> slots) {
        Doctor doctor = new Doctor();
        doctor.setId(DOCTOR_ID);
        doctor.setAvailableTimes(slots);
        return doctor;
    }

    private Appointment appointmentAt(LocalTime time) {
        Appointment appointment = new Appointment();
        appointment.setAppointmentTime(LocalDateTime.of(DATE, time));
        appointment.setDoctor(doctorWithSlots(null));
        appointment.setPatient(new Patient());
        appointment.setStatus(0);
        return appointment;
    }

    @Test
    void returnsAllSlotsWhenNoneAreBooked() {
        Doctor doctor = doctorWithSlots(List.of("09:00-10:00", "10:00-11:00"));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(anyLong(), any(), any()))
                .thenReturn(List.of());

        List<String> available = doctorService.getDoctorAvailability(DOCTOR_ID, DATE);

        assertThat(available).containsExactly("09:00-10:00", "10:00-11:00");
    }

    @Test
    void removesSlotWhoseExactStartTimeIsBooked() {
        Doctor doctor = doctorWithSlots(List.of("09:00-10:00", "10:00-11:00"));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(anyLong(), any(), any()))
                .thenReturn(List.of(appointmentAt(LocalTime.of(9, 0))));

        List<String> available = doctorService.getDoctorAvailability(DOCTOR_ID, DATE);

        assertThat(available).containsExactly("10:00-11:00");
    }

    @Test
    void bookingWithNonExactStartTimeDoesNotConsumeSlot() {
        // A 09:15 booking should NOT be treated as consuming the "09:00-10:00" slot
        // because matching is by exact start time only.
        Doctor doctor = doctorWithSlots(List.of("09:00-10:00"));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(anyLong(), any(), any()))
                .thenReturn(List.of(appointmentAt(LocalTime.of(9, 15))));

        List<String> available = doctorService.getDoctorAvailability(DOCTOR_ID, DATE);

        assertThat(available).containsExactly("09:00-10:00");
    }

    @Test
    void malformedSlotIsSilentlyDropped() {
        Doctor doctor = doctorWithSlots(List.of("09:00-10:00", "not-a-time-slot", ""));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(anyLong(), any(), any()))
                .thenReturn(List.of());

        List<String> available = doctorService.getDoctorAvailability(DOCTOR_ID, DATE);

        assertThat(available).containsExactly("09:00-10:00");
    }

    @Test
    void returnsEmptyListForUnknownDoctor() {
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        List<String> available = doctorService.getDoctorAvailability(DOCTOR_ID, DATE);

        assertThat(available).isEmpty();
    }

    @Test
    void returnsEmptyListWhenAvailableTimesIsNull() {
        Doctor doctor = doctorWithSlots(null);
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

        List<String> available = doctorService.getDoctorAvailability(DOCTOR_ID, DATE);

        assertThat(available).isEmpty();
    }

    @Test
    void bothScheduledAndCompletedAppointmentsCountAsBooked() {
        // No status filter: status 0 (scheduled) and status 1 (completed) should both
        // remove their slot from availability.
        Doctor doctor = doctorWithSlots(List.of("09:00-10:00", "10:00-11:00"));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

        Appointment scheduled = appointmentAt(LocalTime.of(9, 0));
        scheduled.setStatus(0);
        Appointment completed = appointmentAt(LocalTime.of(10, 0));
        completed.setStatus(1);

        when(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(anyLong(), any(), any()))
                .thenReturn(List.of(scheduled, completed));

        List<String> available = doctorService.getDoctorAvailability(DOCTOR_ID, DATE);

        assertThat(available).isEmpty();
    }
}
