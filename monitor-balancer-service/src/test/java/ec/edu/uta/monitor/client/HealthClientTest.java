package ec.edu.uta.monitor.client;

import ec.edu.uta.monitor.dto.EstadoComponente;
import ec.edu.uta.monitor.dto.ResultadoSondeo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HealthClientTest {

    @Test
    @DisplayName("Actividad 5: Respuesta 200 con UP se interpreta como EstadoComponente.UP")
    void testRespuesta200Up() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        HealthClient healthClient = new HealthClient(restClient);

        server.expect(requestTo("http://localhost:8080/actuator/health"))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        ResultadoSondeo r = healthClient.sondear("Vehiculos-A", 8080, "http://localhost:8080/actuator/health");
        assertEquals(EstadoComponente.UP, r.estado());
        assertNotNull(r.latenciaMs());
        assertEquals(200, r.httpStatus());
        server.verify();
    }

    @Test
    @DisplayName("Actividad 5: Respuesta 401 o 403 se interpreta como ERROR_ACCESO y no como servidor caido")
    void testRespuesta401ErrorAcceso() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        HealthClient healthClient = new HealthClient(restClient);

        server.expect(requestTo("http://localhost:8080/actuator/health"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\":\"unauthorized\"}"));

        ResultadoSondeo r = healthClient.sondear("Vehiculos-A", 8080, "http://localhost:8080/actuator/health");
        assertEquals(EstadoComponente.ERROR_ACCESO, r.estado(), "401 debe diferenciarse como ERROR_ACCESO");
        assertNotNull(r.latenciaMs());
        assertEquals(401, r.httpStatus());
        server.verify();
    }

    @Test
    @DisplayName("Actividad 5: Error 503 o status DOWN se interpreta como EstadoComponente.DOWN")
    void testRespuesta503Down() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        HealthClient healthClient = new HealthClient(restClient);

        server.expect(requestTo("http://localhost:8080/actuator/health"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{\"status\":\"DOWN\"}"));

        ResultadoSondeo r = healthClient.sondear("Vehiculos-A", 8080, "http://localhost:8080/actuator/health");
        assertEquals(EstadoComponente.DOWN, r.estado());
        assertNotNull(r.latenciaMs());
        assertEquals(503, r.httpStatus());
        server.verify();
    }

    @Test
    @DisplayName("Actividad 5: Servidor apagado o timeout de red retorna EstadoComponente.DOWN con latencia null")
    void testServidorInaccesible() {
        // Apuntar a una direccion no existente
        RestClient restClient = RestClient.builder().build();
        HealthClient healthClient = new HealthClient(restClient);

        ResultadoSondeo r = healthClient.sondear("Vehiculos-C", 9999, "http://127.0.0.1:9999/actuator/health");
        assertEquals(EstadoComponente.DOWN, r.estado());
        assertNull(r.latenciaMs(), "La latencia debe ser null si no hubo conexion");
        assertNull(r.httpStatus());
    }
}

