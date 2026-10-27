package ec.edu.uta.monitor.service;

import ec.edu.uta.monitor.dto.AlertaResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlertaService {

    private static final int MAXIMO = 20;

    private final LinkedList<AlertaResponse> alertas = new LinkedList<>();

    public synchronized void registrar(String componente, String anterior,
                                       String nuevo, String mensaje) {
        alertas.addFirst(new AlertaResponse(
                OffsetDateTime.now(), componente, anterior, nuevo, mensaje));
        while (alertas.size() > MAXIMO) {
            alertas.removeLast();
        }
    }

    public synchronized List<AlertaResponse> ultimas() {
        return new ArrayList<>(alertas);
    }
}