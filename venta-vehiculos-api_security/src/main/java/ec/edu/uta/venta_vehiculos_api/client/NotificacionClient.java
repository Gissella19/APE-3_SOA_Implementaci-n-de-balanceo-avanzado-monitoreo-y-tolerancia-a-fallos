package ec.edu.uta.venta_vehiculos_api.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.*;
import ec.edu.uta.venta_vehiculos_api.dto.NotificacionRequest;

@Component
public class NotificacionClient {
 private final RestClient client;
 private final String destino;

 public NotificacionClient(
  @Value("${servicios.notificaciones.url}") String url,
  @Value("${servicios.notificaciones.key}") String key,
  @Value("${servicios.notificaciones.destinatario}") String destino) {
  var f=new SimpleClientHttpRequestFactory();
  f.setConnectTimeout(Duration.ofSeconds(2));
  f.setReadTimeout(Duration.ofSeconds(3));

  client=RestClient.builder().baseUrl(url).requestFactory(f)

   .defaultHeader("X-Notification-Key",key).build();
  this.destino=destino;
 }

 public void enviar(Integer id,String tipo,String mensaje) {

  try {
   client.post().uri("/api/notificaciones")
    .body(new NotificacionRequest(id,destino,tipo,mensaje))
    .retrieve().toBodilessEntity();
  } catch (RestClientException e) {
   System.err.println("Notificación no enviada para vehículo "+id);
  }
}
}