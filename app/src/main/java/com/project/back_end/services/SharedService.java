package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

@Service
public class SharedService {
	// 1. **@Service Annotation**
	// The @Service annotation marks this class as a service component in Spring. This allows Spring to automatically detect it through component scanning
	// and manage its lifecycle, enabling it to be injected into controllers or other services using @Autowired or constructor injection.

	// 2. **Constructor Injection for Dependencies**
	// The constructor injects all required dependencies (TokenService, Repositories, and other Services). This approach promotes loose coupling, improves testability,
	// and ensures that all required dependencies are provided at object creation time.
	
	private final TokenService tokenService;
	private final AdminRepository adminRepository;
	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;
	private final DoctorService doctorService;
	private final PatientService patientService;
	
	public SharedService(TokenService tokenService, AdminRepository adminRepository, DoctorRepository doctorRepository, PatientRepository patientRepository, DoctorService doctorService, PatientService patientService) {
		this.tokenService = tokenService;
		this.adminRepository = adminRepository;
		this.doctorRepository = doctorRepository;
		this.patientRepository = patientRepository;
		this.doctorService = doctorService;
		this.patientService = patientService;
	}

	// 3. **validateToken Method**
	// This method checks if the provided JWT token is valid for a specific user. It uses the TokenService to perform the validation.
	// If the token is invalid or expired, it returns a 401 Unauthorized response with an appropriate error message. This ensures security by preventing
	// unauthorized access to protected resources.
	public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
		Map<String, String> response = new HashMap<>();
        if (!tokenService.validateToken(token, user)) {
            response.put("error", "Invalid or expired token");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	// 4. **validateAdmin Method**
	// This method validates the login credentials for an admin user.
	// - It first searches the admin repository using the provided username.
	// - If an admin is found, it checks if the password matches.
	// - If the password is correct, it generates and returns a JWT token (using the admin’s username) with a 200 OK status.
	// - If the password is incorrect, it returns a 401 Unauthorized status with an error message.
	// - If no admin is found, it also returns a 401 Unauthorized.
	// - If any unexpected error occurs during the process, a 500 Internal Server Error response is returned.
	// This method ensures that only valid admin users can access secured parts of the system.
	public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> map = new HashMap<>();
        try {
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin != null) {
                if (admin.getPassword().equals(receivedAdmin.getPassword())) {
                    map.put("token", tokenService.generateToken(admin.getUsername()));
                    return ResponseEntity.status(HttpStatus.OK).body(map);
                } else {
                    map.put("error", "Password does not match");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(map);
                }
            }
            map.put("error", "Invalid email id");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(map);

        } catch (Exception e) {
            System.out.println("Error: " + e);
            map.put("error", "Internal Server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

	// 5. **filterDoctor Method**
	// This method provides filtering functionality for doctors based on name, specialty, and available time slots.
	// - It supports various combinations of the three filters.
	// - If none of the filters are provided, it returns all available doctors.
	// This flexible filtering mechanism allows the frontend or consumers of the API to search and narrow down doctors based on user criteria.
	
	public Map<String, Object> filterDoctor(String name, String specialty, String time) {
		Map<String, Object> result = new HashMap<>();
		try {
			boolean hasName = name != null && !name.equals("null") && !name.isBlank();
			boolean hasSpecialty = specialty != null && !specialty.equals("null") && !specialty.isBlank();
			boolean hasTime = time != null && !time.equals("null") && !time.isBlank();

			if (hasName && hasSpecialty && hasTime) {
				return doctorService.filterDoctorsByNameSpecialtyAndTime(name, specialty, time);
			} else if (hasName && hasSpecialty) {
				return doctorService.filterDoctorByNameAndSpecialty(name, specialty);
			} else if (hasName && hasTime) {
				return doctorService.filterDoctorByNameAndTime(name, time);
			} else if (hasSpecialty && hasTime) {
				return doctorService.filterDoctorByTimeAndSpecialty(specialty, time);
			} else if (hasName) {
				return doctorService.findDoctorByName(name);
			} else if (hasSpecialty) {
				return doctorService.filterDoctorBySpecialty(specialty);
			} else if (hasTime) {
				return doctorService.filterDoctorsByTime(time);
			} else {
				result.put("doctors", doctorService.getDoctors());
				return result;
			}
		} catch (Exception e) {
			result.put("error", "Failed to filter doctors");
			return result;
		}
	}

	// 6. **validateAppointment Method**
	// This method validates if the requested appointment time for a doctor is available.
	// - It first checks if the doctor exists in the repository.
	// - Then, it retrieves the list of available time slots for the doctor on the specified date.
	// - It compares the requested appointment time with the start times of these slots.
	// - If a match is found, it returns 1 (valid appointment time).
	// - If no matching time slot is found, it returns 0 (invalid).
	// - If the doctor doesn’t exist, it returns -1.
	// This logic prevents overlapping or invalid appointment bookings.
	public int validateAppointment(Appointment appointment) {
        Doctor doctor = appointment.getDoctor();
        Optional<Doctor> result = doctorRepository.findById(doctor.getId());
        if (result.isEmpty()) {
            return -1;
        }
        LocalDate appointmentDate = appointment.getAppointmentDate();
        LocalTime appointmentTime = appointment.getAppointmentTimeOnly();
        List<String> availableTime = doctorService.getDoctorAvailability(doctor.getId(), appointmentDate);

        for (String timeSlot : availableTime) {
            // Split the available time slot into start and end times (e.g., "9:00-10:00" ->
            // ["9:00", "10:00"])
            String[] times = timeSlot.split("-");

            // Parse the start time and end time as LocalTime
            LocalTime startTime = LocalTime.parse(times[0]);

            if (appointmentTime.equals(startTime)) {
                return 1; // The appointment time matches the start time of an available slot
            }
        }
        return 0;
    }

	// 7. **validatePatient Method**
	// This method checks whether a patient with the same email or phone number already exists in the system.
	// - If a match is found, it returns false (indicating the patient is not valid for new registration).
	// - If no match is found, it returns true.
	// This helps enforce uniqueness constraints on patient records and prevent duplicate entries.
	public boolean validatePatient(Patient patient) {
        Patient result = patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone());
        if (result != null) {
            return false;
        }
        return true;
    }

	// 8. **validatePatientLogin Method**
	// This method handles login validation for patient users.
	// - It looks up the patient by email.
	// - If found, it checks whether the provided password matches the stored one.
	// - On successful validation, it generates a JWT token and returns it with a 200 OK status.
	// - If the password is incorrect or the patient doesn't exist, it returns a 401 Unauthorized with a relevant error.
	// - If an exception occurs, it returns a 500 Internal Server Error.
	// This method ensures only legitimate patients can log in and access their data securely.
	public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> map = new HashMap<>();
        try {
            Patient result = patientRepository.findByEmail(login.getEmail());
            if (result != null) {
                if (result.getPassword().equals(login.getPassword())) {
                    map.put("token", tokenService.generateToken(login.getEmail()));
                    return ResponseEntity.status(HttpStatus.OK).body(map);
                }

                else {
                    map.put("error", "Password does not match");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(map);
                }
            }
            map.put("error", "Invalid email id");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(map);

        }
        catch (Exception e) {
            System.out.println("Error: " + e);
            map.put("error", "Internal Server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

	// 9. **filterPatient Method**
	// This method filters a patient's appointment history based on condition and doctor name.
	// - It extracts the email from the JWT token to identify the patient.
	// - Depending on which filters (condition, doctor name) are provided, it delegates the filtering logic to PatientService.
	// - If no filters are provided, it retrieves all appointments for the patient.
	// This flexible method supports patient-specific querying and enhances user experience on the client side.
	public ResponseEntity<Map<String,Object>> filterPatient(String condition,String name,String token) {
        String extractedEmail = tokenService.extractEmail(token);
        Long patientId = patientRepository.findByEmail(extractedEmail).getId();
		if(name.equals("null") && !condition.equals("null")) {
            return patientService.filterByCondition(condition,patientId);
        } else if(condition.equals("null")&& !name.equals("null")) {
            return patientService.filterByDoctor(name,patientId);
        } else if(!condition.equals("null")&& !name.equals("null")) {
            return patientService.filterByDoctorAndCondition(condition,name,patientId);
        } else {
            return patientService.getPatientAppointment(patientId,token);
        }
    }
}
