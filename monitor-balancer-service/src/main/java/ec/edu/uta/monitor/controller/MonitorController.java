package ec.edu.uta.monitor.controller;

import ec.edu.uta.monitor.dto.EstadoResponse;
import ec.edu.uta.monitor.service.MonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/estado")
    public EstadoResponse estado() {
        return monitorService.estado();
    }
}