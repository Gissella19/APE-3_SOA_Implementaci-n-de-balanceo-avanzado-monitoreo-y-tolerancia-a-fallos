package ec.edu.uta.monitor.service;

import ec.edu.uta.monitor.dto.AlertaResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);
    private static final int MAXIMO = 20;

    private final LinkedList<AlertaResponse> alertas = new LinkedList<>();
    private final Map<String, String> ultimoEstadoRegistrado = new ConcurrentHashMap<>();

    /**
     * Registra un cambio de estado en memoria (buffer circular de maximo 20 alertas)
     * con deduplicacion estricta para evitar duplicados en rondas consecutivas sin transicion.
     */
    public synchronized void registrar(String componente, String anterior,
                                       String nuevo, String mensaje) {
        if (componente == null || nuevo == null) {
            return;
        }
        // Deduplicacion: si anterior y nuevo son iguales, no hubo cambio
        if (anterior != null && anterior.equalsIgnoreCase(nuevo)) {
            return;
        }
        // Deduplicacion estricta: si el componente ya registro este estado nuevo, ignorar
        String ultimo = ultimoEstadoRegistrado.get(componente);
        if (ultimo != null && ultimo.equalsIgnoreCase(nuevo)) {
            return;
        }

        AlertaResponse alerta = new AlertaResponse(
                OffsetDateTime.now(), componente, anterior, nuevo, mensaje);
        alertas.addFirst(alerta);
        ultimoEstadoRegistrado.put(componente, nuevo);
        log.info("[ALERTA] {} | Transicion: {} -> {} | Mensaje: {}", componente, anterior, nuevo, mensaje);

        while (alertas.size() > MAXIMO) {
            alertas.removeLast();
        }
    }

    public synchronized List<AlertaResponse> ultimas() {
        return new ArrayList<>(alertas);
    }

    public synchronized void limpiar() {
        alertas.clear();
        ultimoEstadoRegistrado.clear();
    }
}
