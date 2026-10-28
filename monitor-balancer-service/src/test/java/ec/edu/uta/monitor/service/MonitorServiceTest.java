package ec.edu.uta.monitor.service;

import ec.edu.uta.monitor.client.HealthClient;
import ec.edu.uta.monitor.config.MonitorProperties;
import ec.edu.uta.monitor.dto.EstadoComponente;
import ec.edu.uta.monitor.dto.EstadoGeneral;
import ec.edu.uta.monitor.dto.EstadoResponse;
import ec.edu.uta.monitor.dto.ResultadoSondeo;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private HealthClient healthClient;

    private AlertaService alertaService;
    private MonitorProperties props;
    private MonitorService monitorService;

    @BeforeEach
    void setUp() {
        alertaService = new AlertaService();
        props = new MonitorProperties(
                2000,
                2000,
                new MonitorProperties.Nginx("NGINX", "http://127.0.0.1:8090/nginx-ping"),
                List.of(
                        new MonitorProperties.Instancia("Vehiculos-A", 8080, "http://127.0.0.1:8080/actuator/health"),
                        new MonitorProperties.Instancia("Vehiculos-B", 8082, "http://127.0.0.1:8082/actuator/health")
                ),
                new MonitorProperties.Log("logs/access.log", 15, "", List.of("/nginx-ping"))
        );
        monitorService = new MonitorService(healthClient, props, alertaService);
    }

    @Test
    @DisplayName("Actividad 5: Regla NORMAL - NGINX responde y ambos backends estan UP")
    void testEstadoNormal() {
        when(healthClient.sondear(eq("NGINX"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("NGINX", 8090, "url", EstadoComponente.UP, 5L, 200, OffsetDateTime.now()));
        when(healthClient.sondear(eq("Vehiculos-A"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-A", 8080, "url", EstadoComponente.UP, 10L, 200, OffsetDateTime.now()));
        when(healthClient.sondear(eq("Vehiculos-B"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-B", 8082, "url", EstadoComponente.UP, 12L, 200, OffsetDateTime.now()));

        monitorService.verificarTodo();
        EstadoResponse estado = monitorService.estado();

        assertEquals(EstadoGeneral.NORMAL, estado.estadoGeneral());
        assertEquals("UP", estado.nginx());
        assertEquals(2, estado.instancias().size());
        assertEquals(EstadoComponente.UP, estado.instancias().get(0).estado());
        assertEquals(EstadoComponente.UP, estado.instancias().get(1).estado());
    }

    @Test
    @DisplayName("Actividad 5: Regla DEGRADADO - NGINX responde y exactamente un backend esta UP")
    void testEstadoDegradado() {
        when(healthClient.sondear(eq("NGINX"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("NGINX", 8090, "url", EstadoComponente.UP, 5L, 200, OffsetDateTime.now()));
        when(healthClient.sondear(eq("Vehiculos-A"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-A", 8080, "url", EstadoComponente.UP, 10L, 200, OffsetDateTime.now()));
        when(healthClient.sondear(eq("Vehiculos-B"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-B", 8082, "url", EstadoComponente.DOWN, null, null, OffsetDateTime.now()));

        monitorService.verificarTodo();
        EstadoResponse estado = monitorService.estado();

        assertEquals(EstadoGeneral.DEGRADADO, estado.estadoGeneral());
    }

    @Test
    @DisplayName("Actividad 5: Regla SIN_BACKENDS - NGINX responde pero ningun backend esta UP")
    void testEstadoSinBackends() {
        when(healthClient.sondear(eq("NGINX"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("NGINX", 8090, "url", EstadoComponente.UP, 5L, 200, OffsetDateTime.now()));
        when(healthClient.sondear(eq("Vehiculos-A"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-A", 8080, "url", EstadoComponente.DOWN, null, null, OffsetDateTime.now()));
        when(healthClient.sondear(eq("Vehiculos-B"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-B", 8082, "url", EstadoComponente.DOWN, null, null, OffsetDateTime.now()));

        monitorService.verificarTodo();
        EstadoResponse estado = monitorService.estado();

        assertEquals(EstadoGeneral.SIN_BACKENDS, estado.estadoGeneral());
    }

    @Test
    @DisplayName("Actividad 5: Regla NGINX_DOWN - NGINX no responde (informa backends igualmente)")
    void testEstadoNginxDown() {
        when(healthClient.sondear(eq("NGINX"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("NGINX", 8090, "url", EstadoComponente.DOWN, null, null, OffsetDateTime.now()));
        when(healthClient.sondear(eq("Vehiculos-A"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-A", 8080, "url", EstadoComponente.UP, 10L, 200, OffsetDateTime.now()));
        when(healthClient.sondear(eq("Vehiculos-B"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-B", 8082, "url", EstadoComponente.UP, 12L, 200, OffsetDateTime.now()));

        monitorService.verificarTodo();
        EstadoResponse estado = monitorService.estado();

        assertEquals(EstadoGeneral.NGINX_DOWN, estado.estadoGeneral());
        assertEquals("DOWN", estado.nginx());
        // Se deben seguir informando las instancias de negocio
        assertEquals(2, estado.instancias().size());
        assertEquals(EstadoComponente.UP, estado.instancias().get(0).estado());
    }

    @Test
    @DisplayName("Actividad 5 y 8: Generacion de alertas al cambiar estado de NORMAL a DEGRADADO y vuelta a NORMAL")
    void testAlertasEnTransicion() {
        // Ronda 1: Todo NORMAL
        when(healthClient.sondear(eq("NGINX"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("NGINX", 8090, "url", EstadoComponente.UP, 5L, 200, OffsetDateTime.now()));
        when(healthClient.sondear(eq("Vehiculos-A"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-A", 8080, "url", EstadoComponente.UP, 10L, 200, OffsetDateTime.now()));
        when(healthClient.sondear(eq("Vehiculos-B"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-B", 8082, "url", EstadoComponente.UP, 12L, 200, OffsetDateTime.now()));

        monitorService.verificarTodo();
        assertTrue(alertaService.ultimas().isEmpty(), "No deben existir alertas en estado inicial estable");

        // Ronda 2: Cae Vehiculos-B
        when(healthClient.sondear(eq("Vehiculos-B"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-B", 8082, "url", EstadoComponente.DOWN, null, null, OffsetDateTime.now()));

        monitorService.verificarTodo();

        // Deben existir 2 alertas: la de Vehiculos-B UP->DOWN y la GLOBAL NORMAL->DEGRADADO
        assertEquals(2, alertaService.ultimas().size());

        // Ronda 3: Vehiculos-B se mantiene DOWN (ronda sin cambio -> no debe duplicar alertas)
        monitorService.verificarTodo();
        assertEquals(2, alertaService.ultimas().size(), "Rondas consecutivas sin cambios no generan duplicados");

        // Ronda 4: Se recupera Vehiculos-B (vuelve a UP)
        when(healthClient.sondear(eq("Vehiculos-B"), anyInt(), anyString()))
                .thenReturn(new ResultadoSondeo("Vehiculos-B", 8082, "url", EstadoComponente.UP, 15L, 200, OffsetDateTime.now()));

        monitorService.verificarTodo();
        assertEquals(4, alertaService.ultimas().size(), "Debe registrar recuperaciones DOWN->UP y DEGRADADO->NORMAL");
    }
}
