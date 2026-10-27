package ec.edu.uta.monitor.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SondeoScheduler {

    private final MonitorService monitorService;

    public SondeoScheduler(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Scheduled(fixedDelayString = "${monitor.intervalo-ms}")
    public void ejecutar() {
        monitorService.verificarTodo();
    }
}