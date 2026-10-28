package ec.edu.uta.monitor.service;

import ec.edu.uta.monitor.config.MonitorProperties;
import ec.edu.uta.monitor.dto.BalanceoResponse;
import ec.edu.uta.monitor.dto.EstadisticasResponse;
import ec.edu.uta.monitor.dto.RegistroLog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class LogMetricsServiceTest {

    private LogMetricsService service;
    private MonitorProperties props;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Path fakeLog = tempDir.resolve("test_access.log");

        props = new MonitorProperties(
                2000,
                2000,
                new MonitorProperties.Nginx("NGINX", "http://127.0.0.1:8090/nginx-ping"),
                List.of(
                        new MonitorProperties.Instancia("Vehiculos-A", 8080, "http://127.0.0.1:8080/actuator/health"),
                        new MonitorProperties.Instancia("Vehiculos-B", 8082, "http://127.0.0.1:8082/actuator/health")
                ),
                new MonitorProperties.Log(
                        fakeLog.toString().replace('\\', '/'),
                        60,
                        "",
                        List.of("/nginx-ping", "/actuator", "/oauth")
                )
        );
        service = new LogMetricsService(props);
    }

    @Test
    @DisplayName("Actividad 6: Parser de linea individual en formato JSON de NGINX")
    void testParsearLineaJson() {
        String linea = "{\"timestamp\":\"2026-10-28T17:00:00-05:00\",\"method\":\"GET\",\"uri\":\"/api/vehiculos/instancia\",\"status\":\"200\",\"upstream_addr\":\"127.0.0.1:8080\",\"upstream_status\":\"200\",\"request_time\":\"0.025\",\"upstream_response_time\":\"0.025\"}";
        RegistroLog reg = service.parsear(linea);

        assertNotNull(reg);
        assertEquals("GET", reg.metodo());
        assertEquals("/api/vehiculos/instancia", reg.uri());
        assertEquals(200, reg.status());
        assertEquals("127.0.0.1:8080", reg.upstreamAddr());
        assertEquals(0.025, reg.requestTimeSeg());
    }

    @Test
    @DisplayName("Actividad 6: Tolerancia a lineas con campos vacios, guiones o no-JSON")
    void testParsearLineasEspeciales() {
        // Linea no-JSON no debe provocar excepcion
        assertNull(service.parsear("127.0.0.1 GET /something 200"));

        // Linea con request_time=\"-\" o upstream_addr=\"-\"
        String lineaFallo = "{\"timestamp\":\"2026-10-28T17:00:00-05:00\",\"method\":\"GET\",\"uri\":\"/api/vehiculos/instancia\",\"status\":\"502\",\"upstream_addr\":\"-\",\"upstream_status\":\"-\",\"request_time\":\"-\",\"upstream_response_time\":\"-\"}";
        RegistroLog reg = service.parsear(lineaFallo);
        assertNotNull(reg);
        assertEquals(502, reg.status());
        assertEquals("-", reg.upstreamAddr());
        assertEquals(0.0, reg.requestTimeSeg());
    }

    @Test
    @DisplayName("Actividad 6: Reintentos en NGINX: contabilizar unicamente el ultimo upstream atendido")
    void testUpstreamFinalConReintentos() {
        // En failover, NGINX anota ambos upstreams separados por coma
        String addrMultiple = "127.0.0.1:8082, 127.0.0.1:8080";
        assertEquals("127.0.0.1:8080", service.upstreamFinal(addrMultiple));

        assertEquals("127.0.0.1:8082", service.upstreamFinal("127.0.0.1:8082"));
        assertEquals("-", service.upstreamFinal("-"));
        assertEquals("-", service.upstreamFinal(null));
    }

    @Test
    @DisplayName("Actividad 6: Exclusion de /nginx-ping y rutas administrativas")
    void testExclusionRutas() {
        assertTrue(service.excluido("/nginx-ping"));
        assertTrue(service.excluido("/actuator/health"));
        assertTrue(service.excluido("/oauth/token"));
        assertFalse(service.excluido("/api/vehiculos/instancia"));
        assertFalse(service.excluido("/api/vehiculos/catalogo"));
    }

    @Test
    @DisplayName("Actividad 6: Calculo de metricas reales de balanceo (75% Instancia A vs 25% Instancia B)")
    void testMetricasBalanceoReal() throws IOException {
        Path logFile = Path.of(props.log().ruta());
        String ahora = OffsetDateTime.now().toString();

        StringBuilder sb = new StringBuilder();
        // 75 peticiones hacia Instancia A (puerto 8080)
        for (int i = 0; i < 75; i++) {
            sb.append(String.format("{\"timestamp\":\"%s\",\"method\":\"GET\",\"uri\":\"/api/vehiculos/instancia\",\"status\":\"200\",\"upstream_addr\":\"127.0.0.1:8080\",\"upstream_status\":\"200\",\"request_time\":\"0.010\",\"upstream_response_time\":\"0.010\"}\n", ahora));
        }
        // 25 peticiones hacia Instancia B (puerto 8082)
        for (int i = 0; i < 25; i++) {
            sb.append(String.format("{\"timestamp\":\"%s\",\"method\":\"GET\",\"uri\":\"/api/vehiculos/instancia\",\"status\":\"200\",\"upstream_addr\":\"127.0.0.1:8082\",\"upstream_status\":\"200\",\"request_time\":\"0.020\",\"upstream_response_time\":\"0.020\"}\n", ahora));
        }
        // 5 peticiones a /nginx-ping (deben ser excluidas)
        for (int i = 0; i < 5; i++) {
            sb.append(String.format("{\"timestamp\":\"%s\",\"method\":\"GET\",\"uri\":\"/nginx-ping\",\"status\":\"200\",\"upstream_addr\":\"-\",\"upstream_status\":\"-\",\"request_time\":\"0.001\",\"upstream_response_time\":\"-\"}\n", ahora));
        }
        Files.writeString(logFile, sb.toString());

        BalanceoResponse balanceo = service.balanceo();
        assertEquals(100, balanceo.totalSolicitudes(), "El total de solicitudes debe ser exactamente 100 (excluyendo ping)");
        assertEquals(75, balanceo.instanciaA());
        assertEquals(25, balanceo.instanciaB());
        assertEquals(0, balanceo.sinUpstream());
        assertEquals(75.0, balanceo.porcentajeA());
        assertEquals(25.0, balanceo.porcentajeB());
        assertEquals(0.0, balanceo.porcentajeSinUpstream());
    }

    @Test
    @DisplayName("Actividad 6: Estadisticas con conteo 2xx/4xx/5xx y promedio de latencia en milisegundos")
    void testEstadisticasConteoYPromedioMs() throws IOException {
        Path logFile = Path.of(props.log().ruta());
        String ahora = OffsetDateTime.now().toString();

        StringBuilder sb = new StringBuilder();
        // 80 solicitudes 200 OK (0.010 seg = 10 ms)
        for (int i = 0; i < 80; i++) {
            sb.append(String.format("{\"timestamp\":\"%s\",\"method\":\"GET\",\"uri\":\"/api/vehiculos\",\"status\":\"200\",\"upstream_addr\":\"127.0.0.1:8080\",\"request_time\":\"0.010\"}\n", ahora));
        }
        // 10 solicitudes 401 Unauthorized (0.005 seg = 5 ms)
        for (int i = 0; i < 10; i++) {
            sb.append(String.format("{\"timestamp\":\"%s\",\"method\":\"GET\",\"uri\":\"/api/vehiculos\",\"status\":\"401\",\"upstream_addr\":\"127.0.0.1:8080\",\"request_time\":\"0.005\"}\n", ahora));
        }
        // 10 solicitudes 502 Bad Gateway (0.025 seg = 25 ms)
        for (int i = 0; i < 10; i++) {
            sb.append(String.format("{\"timestamp\":\"%s\",\"method\":\"GET\",\"uri\":\"/api/vehiculos\",\"status\":\"502\",\"upstream_addr\":\"127.0.0.1:8082\",\"request_time\":\"0.025\"}\n", ahora));
        }
        Files.writeString(logFile, sb.toString());

        EstadisticasResponse stats = service.estadisticas();
        assertEquals(100, stats.totalPeticiones());
        assertEquals(80, stats.respuestas2xx());
        assertEquals(10, stats.respuestas4xx());
        assertEquals(10, stats.respuestas5xx());
        // Promedio esperado: (80*10 + 10*5 + 10*25)/100 = (800 + 50 + 250)/100 = 1100/100 = 11.0 ms
        assertEquals(11.0, stats.promedioRespuestaMs());
    }
}
