package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.services.TokenService;

import jakarta.transaction.Transactional;

@Service
public class AppointmentService {
	// 1. **Add @Service Annotation**:
	//    - To indicate that this class is a service layer class for handling business logic.
	//    - The `@Service` annotation should be added before the class declaration to mark it as a Spring service component.
	//    - Instruction: Add `@Service` above the class definition.

	// 2. **Constructor Injection for Dependencies**:
	//    - The `AppointmentService` class requires several dependencies like `AppointmentRepository`, `Service`, `TokenService`, `PatientRepository`, and `DoctorRepository`.
	//    - These dependencies should be injected through the constructor.
	//    - Instruction: Ensure constructor injection is used for proper dependency management in Spring.

    private final AppointmentRepository appointmentRepository;
    private final SharedService sharedService;
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;


	public AppointmentService(AppointmentRepository appointmentRepository, SharedService sharedService, TokenService tokenService, PatientRepository patientRepository, DoctorRepository doctorRepository) {
		this.appointmentRepository = appointmentRepository;
        this.sharedService = sharedService;
        this.tokenService= tokenService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
	}

	// 3. **Add @Transactional Annotation for Methods that Modify Database**:
	//    - The methods that modify or update the database should be annotated with `@Transactional` to ensure atomicity and consistency of the operations.
	//    - Instruction: Add the `@Transactional` annotation above methods that interact with the database, especially those modifying data.

	// 4. **Book Appointment Method**:
	//    - Responsible for saving the new appointment to the database.
	//    - If the save operation fails, it returns `0`; otherwise, it returns `1`.
	//    - Instruction: Ensure that the method handles any exceptions and returns an appropriate result code.
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1; // Success code
        } catch (Exception e) {
            // Log the exception details so the root cause is not permanently lost
            System.out.println("Failed to save appointment " + e);
            return 0; // Error code
        }
    }

	// 5. **Update Appointment Method**:
	//    - This method is used to update an existing appointment based on its ID.
	//    - It validates whether the patient ID matches, checks if the appointment is available for updating, and ensures that the doctor is available at the specified time.
	//    - If the update is successful, it saves the appointment; otherwise, it returns an appropriate error message.
	//    - Instruction: Ensure proper validation and error handling is included for appointment updates.
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment, String token) {
        Map<String, String> response = new HashMap<>();

        Optional<Appointment> result = appointmentRepository.findById(appointment.getId());
        if (!result.isPresent()) {
            response.put("message", "No appointment with ID: " + appointment.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        Appointment existing = result.get();

        // Ownership must be established from the token, never from the request body:
        // both appointment.getId() and appointment.getPatient().getId() are attacker
        // controlled, so comparing the body against itself is not a real check.
        Patient caller = patientRepository.findByEmail(tokenService.extractEmail(token));
        if (caller == null || !caller.getId().equals(existing.getPatient().getId())) {
            response.put("message", "Patient ID Mismatch");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Skip availability validation when the requested time matches the persisted
        // time: the appointment's own pre-update row still occupies that slot, so
        // getDoctorAvailability would incorrectly report a same-time update as booked.
        boolean timeChanged = !Objects.equals(existing.getAppointmentTime(), appointment.getAppointmentTime());

        // The doctor must always come from the persisted record, never from the
        // request body: the body's doctor field is attacker-controlled, and only
        // `existing` (with its real doctor) is ever saved. Validating against the
        // body's doctor while saving against `existing`'s doctor would let a caller
        // pick an unrelated doctor with a free slot to pass validation, then have
        // the appointment persisted against their real (possibly unavailable)
        // doctor — bypassing the only scheduling-integrity check in the app.
        appointment.setDoctor(existing.getDoctor());
        int validated = timeChanged ? sharedService.validateAppointment(appointment) : 1;

        if (validated == 1) {
            try {
                // Copy only the genuinely mutable fields onto the persisted record and
                // save that, rather than saving the client-supplied entity wholesale,
                // so the request body cannot reassign patient/doctor on this row.
                existing.setAppointmentTime(appointment.getAppointmentTime());
                existing.setStatus(appointment.getStatus());
                appointmentRepository.save(existing);
                response.put("message", "Appointment Updated Successfully");
                return ResponseEntity.status(HttpStatus.OK).body(response);

            } catch (Exception e) {
                System.out.println("Error: " + e);
                response.put("message", "Internal Server Error");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } else if (validated == -1) {
            response.put("message", "Invalid doctor ID");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        response.put("message", "Appointment already booked for given time or doctor not available");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

    }

	// 6. **Cancel Appointment Method**:
	//    - This method cancels an appointment by deleting it from the database.
	//    - It ensures the patient who owns the appointment is trying to cancel it and handles possible errors.
	//    - Instruction: Make sure that the method checks for the patient ID match before deleting the appointment.
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment (Long id, String token) {
        Map<String, String> response = new HashMap<>();

        Optional<Appointment> appointment = appointmentRepository.findById(id);
        if (appointment.isEmpty()) {
            response.put("message", "No appointment with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Patient caller = patientRepository.findByEmail(tokenService.extractEmail(token));
        if (caller == null || !caller.getId().equals(appointment.get().getPatient().getId())) {
            response.put("message", "Patient ID Mismatch");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            appointmentRepository.delete(appointment.get());
            response.put("message", "Appointment Deleted Successfully");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            System.out.println("Error: " + e);
            response.put("message", "Internal Server Error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

	// 7. **Get Appointments Method**:
	//    - This method retrieves a list of appointments for a specific doctor on a particular day, optionally filtered by the patient's name.
	//    - It uses `@Transactional` to ensure that database operations are consistent and handled in a single transaction.
	//    - Instruction: Ensure the correct use of transaction boundaries, especially when querying the database for appointments.
    @Transactional
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> map = new HashMap<>();
        try {
            Doctor doctor = doctorRepository.findByEmail(tokenService.extractEmail(token));
            if (doctor == null) {
                map.put("error", "Invalid token");
                map.put("appointments", List.of());
                return map;
            }

            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end   = date.atTime(LocalTime.MAX);

            List<Appointment> appointments;
            if (pname == null || pname.equals("null") || pname.isBlank()) {
                appointments = appointmentRepository
                        .findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), start, end);
            } else {
                appointments = appointmentRepository
                        .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                                doctor.getId(), pname, start, end);
            }

            map.put("appointments", toAppointmentDTOs(appointments));
        } catch (Exception e) {
            System.out.println("Error: " + e);
            map.put("error", "Internal Server Error");
        }
        return map;
    }

	// private copy — intentionally duplicates PatientService.toAppointmentDTOs to keep these two files
	// independently owned. Do NOT widen or reuse PatientService's private mapper.
	private List<AppointmentDTO> toAppointmentDTOs(List<Appointment> appointments) {
		return appointments.stream()
				.map(app -> new AppointmentDTO(
						app.getId(),
						app.getDoctor().getId(), app.getDoctor().getName(),
						app.getPatient().getId(), app.getPatient().getName(),
						app.getPatient().getEmail(), app.getPatient().getPhone(),
						app.getPatient().getAddress(),
						app.getAppointmentTime(), app.getStatus()))
				.collect(Collectors.toList());
	}

	// 8. **Change Status Method**:
	//    - This method updates the status of an appointment by changing its value in the database.
	//    - It should be annotated with `@Transactional` to ensure the operation is executed in a single transaction.
	//    - Instruction: Add `@Transactional` before this method to ensure atomicity when updating appointment status.
    @Transactional
    public void changeStatus(Long appointmentId)
    {
        appointmentRepository.updateStatus(1, appointmentId);
    }


}
