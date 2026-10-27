package ec.edu.uta.monitor.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monitor")
public record MonitorProperties(
        int connectTimeoutMs,
        int readTimeoutMs,
        Nginx nginx,
        List<Instancia> instancias) {

    public record Nginx(String nombre, String url) { }

    public record Instancia(String nombre, int puerto, String url) { }
}