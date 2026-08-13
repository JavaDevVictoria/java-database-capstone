package com.project.back_end.mvc;

import com.project.back_end.services.SharedService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@Controller
public class DashboardController {

    private final SharedService service;

    public DashboardController(SharedService service) {
        this.service = service;
    }

    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        Map<String, String> body = service.validateToken(token, "admin").getBody();
        if (body == null || body.isEmpty()) {
            return "admin/adminDashboard"; // map empty = valid token
        }
        return "redirect:/"; // map not empty = errors exist
    }

    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        Map<String, String> body = service.validateToken(token, "doctor").getBody();
        if (body == null || body.isEmpty()) {
            return "doctor/doctorDashboard"; // map empty = valid token
        }
        return "redirect:/"; // map not empty = errors exist
    }
}