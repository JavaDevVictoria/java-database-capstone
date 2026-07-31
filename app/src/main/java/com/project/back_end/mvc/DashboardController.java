package com.project.back_end.mvc;

import com.project.back_end.services.Service;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DashboardController {

    private final Service service;

    public DashboardController(Service service) {
        this.service = service;
    }

    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        if (service.validateToken(token, "admin").isEmpty()) {
            return "admin/adminDashboard"; // map empty = valid token
        }
        return "redirect:/"; // map not empty = errors exist
    }

    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        if (service.validateToken(token, "doctor").isEmpty()) {
            return "doctor/doctorDashboard"; // map empty = valid token
        }
        return "redirect:/"; // map not empty = errors exist
    }
}