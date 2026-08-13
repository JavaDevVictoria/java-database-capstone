package com.project.back_end.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.SharedService;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/patient")
public class PatientController {

	// 1. Set Up the Controller Class:
	//    - Annotate the class with `@RestController` to define it as a REST API controller for patient-related operations.
	//    - Use `@RequestMapping("/patient")` to prefix all endpoints with `/patient`, grouping all patient functionalities under a common route.


	// 2. Autowire Dependencies:
	//    - Inject `PatientService` to handle patient-specific logic such as creation, retrieval, and appointments.
	//    - Inject the shared `Service` class for tasks like token validation and login authentication.
	private final PatientService patientService;
	private final SharedService service;

	public PatientController(PatientService patientService, SharedService service) {
        this.patientService = patientService;
        this.service = service;
    }

	// 3. Define the `getPatient` Method:
	//    - Handles HTTP GET requests to retrieve patient details using a token.
	//    - Validates the token for the `"patient"` role using the shared service.
	//    - If the token is valid, returns patient information; otherwise, returns an appropriate error message.
	@GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {
        Map<String, Object> map = new HashMap<>();
        ResponseEntity<Map<String,String>> tempMap = service.validateToken(token, "patient");
        if (!tempMap.getBody().isEmpty()) {
            map.putAll(tempMap.getBody());
            return new ResponseEntity<>(map, tempMap.getStatusCode());
        }
        return patientService.getPatientDetails(token);
    }

	// 4. Define the `createPatient` Method:
	//    - Handles HTTP POST requests for patient registration.
	//    - Accepts a validated `Patient` object in the request body.
	//    - First checks if the patient already exists using the shared service.
	//    - If validation passes, attempts to create the patient and returns success or error messages based on the outcome.
	@PostMapping()
    public ResponseEntity<Map<String, String>> createPatient(@RequestBody @Valid Patient patient) {
        Map<String, String> map = new HashMap<>();
        if(service.validatePatient(patient)) {
            int response = patientService.createPatient(patient);
            if(response == 1) {
                map.put("message", "Registration successful");
                return ResponseEntity.status(HttpStatus.CREATED).body(map); 
            }
            if(response == 0) {
                map.put("message", "Internal server error");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map); 
            }
        }
        map.put("message", "A patient with this email address or phone number already exists");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map); 
    }

	// 5. Define the `login` Method:
	//    - Handles HTTP POST requests for patient login.
	//    - Accepts a `Login` DTO containing email/username and password.
	//    - Delegates authentication to the `validatePatientLogin` method in the shared service.
	//    - Returns a response with a token or an error message depending on login success.
	@PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Login login) {
        return service.validatePatientLogin(login);   
    }

	// 6. Define the `getPatientAppointment` Method:
	//    - Handles HTTP GET requests to fetch appointment details for a specific patient.
	//    - Requires the patient ID, token, and user role as path variables.
	//    - Validates the token using the shared service.
	//    - If valid, retrieves the patient's appointment data from `PatientService`; otherwise, returns a validation error.
	@GetMapping("/{id}/{user}/{token}")
    public ResponseEntity<Map<String,Object>> getPatientAppointment(@PathVariable Long id,@PathVariable String token, @PathVariable String user) {
        Map<String, Object> map = new HashMap<>();
        ResponseEntity<Map<String,String>> tempMap = service.validateToken(token, user);
        if (!tempMap.getBody().isEmpty()) {
            map.putAll(tempMap.getBody());
            return new ResponseEntity<>(map, tempMap.getStatusCode());
        }
        return patientService.getPatientAppointment(id,token);
    }

	// 7. Define the `filterPatientAppointment` Method:
	//    - Handles HTTP GET requests to filter a patient's appointments based on specific conditions.
	//    - Accepts filtering parameters: `condition`, `name`, and a token.
	//    - Token must be valid for a `"patient"` role.
	//    - If valid, delegates filtering logic to the shared service and returns the filtered result.
	@GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<Map<String,Object>> filterPatientAppointment(@PathVariable String condition, @PathVariable String name, @PathVariable String token) {
        Map<String, Object> map = new HashMap<>();
        ResponseEntity<Map<String,String>> tempMap = service.validateToken(token, "patient");
        if (!tempMap.getBody().isEmpty()) {
            map.putAll(tempMap.getBody());
            return new ResponseEntity<>(map, tempMap.getStatusCode());
        }
        return service.filterPatient(condition,name,token);
    }
}

