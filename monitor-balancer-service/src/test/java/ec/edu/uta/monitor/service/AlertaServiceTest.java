package ec.edu.uta.monitor.service;

import ec.edu.uta.monitor.dto.AlertaResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertaServiceTest {

    private AlertaService alertaService;

    @BeforeEach
    void setUp() {
        alertaService = new AlertaService();
    }

    @Test
    @DisplayName("Actividad 8: Debe registrar transiciones UP -> DOWN y DOWN -> UP")
    void testRegistrarTransicion() {
        alertaService.registrar("Vehiculos-B", "UP", "DOWN", "Instancia B fuera de servicio");

        List<AlertaResponse> alertas = alertaService.ultimas();
        assertEquals(1, alertas.size());
        assertEquals("Vehiculos-B", alertas.get(0).componente());
        assertEquals("UP", alertas.get(0).anterior());
        assertEquals("DOWN", alertas.get(0).nuevo());
        assertTrue(alertas.get(0).mensaje().contains("fuera de servicio"));
    }

    @Test
    @DisplayName("Actividad 8: Deduplicacion estricta: rondas consecutivas sin cambio no deben generar alertas duplicadas")
    void testDeduplicacionEstricta() {
        // Primera transicion a DOWN
        alertaService.registrar("Vehiculos-B", "UP", "DOWN", "Instancia B fuera de servicio");
        assertEquals(1, alertaService.ultimas().size());

        // Verificaciones periodicas subsiguientes donde el estado sigue siendo DOWN
        alertaService.registrar("Vehiculos-B", "DOWN", "DOWN", "Sondeo recurrente sigue DOWN");
        alertaService.registrar("Vehiculos-B", "DOWN", "DOWN", "Sondeo recurrente sigue DOWN");

        // No debe haberse generado ninguna alerta adicional identica
        assertEquals(1, alertaService.ultimas().size(), "No deben existir alertas duplicadas para el mismo estado");

        // Recuperacion de la instancia a UP
        alertaService.registrar("Vehiculos-B", "DOWN", "UP", "Instancia B recuperada");
        assertEquals(2, alertaService.ultimas().size());
        assertEquals("UP", alertaService.ultimas().get(0).nuevo());

        // Rondas subsiguientes donde sigue UP
        alertaService.registrar("Vehiculos-B", "UP", "UP", "Sondeo recurrente sigue UP");
        assertEquals(2, alertaService.ultimas().size(), "No debe duplicar alertas en UP");
    }

    @Test
    @DisplayName("Actividad 8: Buffer circular en memoria: conservar como maximo los ultimos 20 eventos")
    void testCapacidadMaxima20Eventos() {
        for (int i = 1; i <= 30; i++) {
            alertaService.registrar("Comp-" + i, "UP", "DOWN", "Evento " + i);
        }

        List<AlertaResponse> alertas = alertaService.ultimas();
        assertEquals(20, alertas.size(), "La lista de alertas en memoria debe limitar a un maximo de 20");
        // El evento mas reciente debe estar al inicio
        assertEquals("Comp-30", alertas.get(0).componente());
    }

    @Test
    @DisplayName("Actividad 8: Registro de transicion de estado global")
    void testTransicionEstadoGlobal() {
        alertaService.registrar("GLOBAL", "NORMAL", "DEGRADADO", "El estado general cambio de NORMAL a DEGRADADO");

        List<AlertaResponse> alertas = alertaService.ultimas();
        assertEquals(1, alertas.size());
        assertEquals("GLOBAL", alertas.get(0).componente());
        assertEquals("NORMAL", alertas.get(0).anterior());
        assertEquals("DEGRADADO", alertas.get(0).nuevo());
    }
}
