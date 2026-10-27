package ec.edu.uta.monitor.controller;

import ec.edu.uta.monitor.dto.BalanceoResponse;
import ec.edu.uta.monitor.dto.EstadoResponse;
import ec.edu.uta.monitor.dto.InstanciaResponse;
import ec.edu.uta.monitor.service.LogMetricsService;
import ec.edu.uta.monitor.service.MonitorService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ec.edu.uta.monitor.dto.EstadisticasResponse;
import ec.edu.uta.monitor.dto.AlertaResponse;
import ec.edu.uta.monitor.service.AlertaService;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService monitorService;
    private final LogMetricsService logMetricsService;
    private final AlertaService alertaService;

    public MonitorController(MonitorService monitorService,
            LogMetricsService logMetricsService,
            AlertaService alertaService) {
        this.monitorService = monitorService;
        this.logMetricsService = logMetricsService;
        this.alertaService = alertaService;
    }

    @GetMapping("/estado")
    public EstadoResponse estado() {
        return monitorService.estado();
    }

    @GetMapping("/instancias")
    public List<InstanciaResponse> instancias() {
        return monitorService.instancias();
    }

    @GetMapping("/balanceo")
    public BalanceoResponse balanceo() {
        return logMetricsService.balanceo();
    }

    @GetMapping("/estadisticas")
    public EstadisticasResponse estadisticas() {
        return logMetricsService.estadisticas();
    }

    @GetMapping("/alertas")
    public List<AlertaResponse> alertas() {
        return alertaService.ultimas();
    }

}