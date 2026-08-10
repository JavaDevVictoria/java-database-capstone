package com.project.back_end.services;

import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.services.Service;
import com.project.back_end.services.TokenService;

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
    private final Service service;
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;


	public AppointmentService(AppointmentRepository appointmentRepository, Service service, TokenService tokenService, PatientRepository patientRepository, DoctorRepository doctorRepository) {
		this.appointmentRepository = appointmentRepository;
        this.service = service;
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
            log.error("Failed to save appointment", e);
            return 0; // Error code
        }
    }

	// 5. **Update Appointment Method**:
	//    - This method is used to update an existing appointment based on its ID.
	//    - It validates whether the patient ID matches, checks if the appointment is available for updating, and ensures that the doctor is available at the specified time.
	//    - If the update is successful, it saves the appointment; otherwise, it returns an appropriate error message.
	//    - Instruction: Ensure proper validation and error handling is included for appointment updates.
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();

        Optional<Appointment> result = appointmentRepository.findById(appointment.getId());
        if (!result.isPresent()) {
            response.put("message", "No appointment with ID: " + appointment.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        if (result.get().getPatient().getId() != appointment.getPatient().getId()) {
            response.put("message", "Patient ID Mismatch");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        int validated = service.validateAppointment(appointment);
        if (validated == 1) {
            try {
                appointmentRepository.save(appointment);
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

        Optional<Appointment> result = appointmentRepository.findById(appointment.getId());
        if (!result.isPresent()) {
            response.put("message", "No appointment with ID: " + appointment.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } 
        
        if (result.get().getPatient().getId() != appointment.getPatient().getId()) {
            response.put("message", "Patient ID Mismatch");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            appointmentRepository.delete(appointment);
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
        appointmentRepository.findByDoctorIdAndAppointmentTimeBetween();
    }

	// 8. **Change Status Method**:
	//    - This method updates the status of an appointment by changing its value in the database.
	//    - It should be annotated with `@Transactional` to ensure the operation is executed in a single transaction.
	//    - Instruction: Add `@Transactional` before this method to ensure atomicity when updating appointment status.
    @Transactional
    public void changeStatus(long appointmentId)
    {
        appointmentRepository.updateStatus(1, appointmentId);
    }


}
