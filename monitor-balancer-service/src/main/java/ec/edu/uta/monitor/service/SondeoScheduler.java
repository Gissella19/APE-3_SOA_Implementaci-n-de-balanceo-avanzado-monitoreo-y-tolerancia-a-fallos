package ec.edu.uta.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SondeoScheduler {

    private static final Logger log = LoggerFactory.getLogger(SondeoScheduler.class);

    private final MonitorService monitorService;

    public SondeoScheduler(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Scheduled(fixedDelayString = "${monitor.intervalo-ms:5000}")
    public void ejecutar() {
        log.trace("Ejecutando sondeo periodico de salud y balanceo...");
        monitorService.verificarTodo();
    }
}
