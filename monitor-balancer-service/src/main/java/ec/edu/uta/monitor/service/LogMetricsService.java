package ec.edu.uta.monitor.service;

import ec.edu.uta.monitor.config.MonitorProperties;
import ec.edu.uta.monitor.dto.BalanceoResponse;
import ec.edu.uta.monitor.dto.EstadisticasResponse;
import ec.edu.uta.monitor.dto.RegistroLog;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogMetricsService {

    private static final Logger log = LoggerFactory.getLogger(LogMetricsService.class);

    // Captura campos JSON tanto con valores entre comillas como valores numericos
    // sin comillas
    private static final Pattern CAMPO_JSON = Pattern
            .compile("\"(\\w+)\"\\s*:\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|([0-9.-]+))");

    private final MonitorProperties props;

    public LogMetricsService(MonitorProperties props) {
        this.props = props;
    }

    public Path resolverRutaLog() {
        if (props.log() == null || props.log().ruta() == null) {
            return null;
        }
        Path configurada = Path.of(props.log().ruta());
        if (Files.exists(configurada)) {
            return configurada;
        }
        // Fallbacks comunes en entorno Windows local
        List<Path> alternativas = List.of(
                Path.of("C:/nginx/nginx-1.30.5/logs/access.log"),
                Path.of("C:/nginx/logs/access.log"),
                Path.of("nginx/logs/access.log"));
        for (Path alt : alternativas) {
            if (Files.exists(alt)) {
                return alt;
            }
        }
        return configurada;
    }

    public OffsetDateTime inicioVentana() {
        String config = props.log() != null ? props.log().inicio() : null;
        if (config != null && !config.isBlank()) {
            try {
                return OffsetDateTime.parse(config.trim());
            } catch (Exception e) {
                log.warn("Formato de monitor.log.inicio invalido ({}), usando ventana por minutos.", config);
            }
        }
        int minutos = props.log() != null ? props.log().ventanaMinutos() : 15;
        return OffsetDateTime.now().minusMinutes(minutos);
    }

    public List<RegistroLog> leerVentana(OffsetDateTime inicio) {
        List<RegistroLog> registros = new ArrayList<>();
        Path ruta = resolverRutaLog();
        if (ruta == null || !Files.exists(ruta)) {
            return registros;
        }

        try (BufferedReader reader = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || !linea.startsWith("{")) {
                    continue; // Ignorar lineas no-JSON o formato previo
                }
                RegistroLog r = parsear(linea);
                if (r != null && !r.timestamp().isBefore(inicio) && !excluido(r.uri())) {
                    registros.add(r);
                }
            }
        } catch (IOException e) {
            log.error("Error al leer access.log de NGINX: {}", e.getMessage());
        }
        return registros;
    }

    public boolean excluido(String uri) {
        if (uri == null || uri.isBlank()) {
            return true;
        }
        if (props.log() == null || props.log().excluirPrefijos() == null) {
            return false;
        }
        return props.log().excluirPrefijos().stream()
                .anyMatch(p -> !p.isBlank() && uri.startsWith(p));
    }

    public RegistroLog parsear(String linea) {
        if (!linea.startsWith("{")) {
            return null;
        }
        try {
            Matcher m = CAMPO_JSON.matcher(linea);
            Map<String, String> campos = new HashMap<>();
            while (m.find()) {
                String clave = m.group(1);
                String valor = m.group(2) != null ? m.group(2) : m.group(3);
                campos.put(clave, valor);
            }
            if (!campos.containsKey("timestamp")) {
                return null;
            }

            OffsetDateTime timestamp = OffsetDateTime.parse(campos.get("timestamp"));
            String metodo = campos.getOrDefault("method", "");
            String uri = campos.getOrDefault("uri", "");
            int status = parseEntero(campos.getOrDefault("status", "0"));
            String upstreamAddr = campos.getOrDefault("upstream_addr", "-");
            double requestTime = parseDecimal(campos.getOrDefault("request_time", "0"));

            return new RegistroLog(timestamp, metodo, uri, status, upstreamAddr, requestTime);
        } catch (Exception e) {
            return null;
        }
    }

    private int parseEntero(String valor) {
        if (valor == null || valor.isBlank() || valor.equals("-")) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDecimal(String valor) {
        if (valor == null || valor.isBlank() || valor.equals("-")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // NGINX anota los reintentos separados por coma: se contabiliza unicamente el
    // ultimo upstream atendido
    public String upstreamFinal(String addr) {
        if (addr == null || addr.isBlank() || addr.equals("-")) {
            return "-";
        }
        String[] partes = addr.split(",");
        return partes[partes.length - 1].trim();
    }

    private double pct(long parte, long total) {
        return total == 0 ? 0.0 : Math.round(parte * 10000.0 / total) / 100.0;
    }

    public BalanceoResponse balanceo() {
        OffsetDateTime inicio = inicioVentana();
        OffsetDateTime fin = OffsetDateTime.now();
        List<RegistroLog> regs = leerVentana(inicio);

        int puertoA = (props.instancias() != null && !props.instancias().isEmpty())
                ? props.instancias().get(0).puerto()
                : 8080;
        int puertoB = (props.instancias() != null && props.instancias().size() > 1)
                ? props.instancias().get(1).puerto()
                : 8082;

        String patronPuertoA = ":" + puertoA;
        String patronPuertoB = ":" + puertoB;

        long a = 0;
        long b = 0;
        long sin = 0;

        for (RegistroLog r : regs) {
            String up = upstreamFinal(r.upstreamAddr());
            if (up.contains(patronPuertoA)) {
                a++;
            } else if (up.contains(patronPuertoB)) {
                b++;
            } else {
                sin++;
            }
        }
        long total = regs.size();
        return new BalanceoResponse(inicio, fin, total, a, b, sin,
                pct(a, total), pct(b, total), pct(sin, total));
    }

    public EstadisticasResponse estadisticas() {
        OffsetDateTime inicio = inicioVentana();
        OffsetDateTime fin = OffsetDateTime.now();
        List<RegistroLog> regs = leerVentana(inicio);

        long c2 = 0;
        long c4 = 0;
        long c5 = 0;
        double sumaSegundos = 0.0;

        for (RegistroLog r : regs) {
            int s = r.status();
            if (s >= 200 && s < 300) {
                c2++;
            } else if (s >= 400 && s < 500) {
                c4++;
            } else if (s >= 500) {
                c5++;
            }
            sumaSegundos += r.requestTimeSeg();
        }

        // Convertir de segundos a milisegundos y redondear a 2 decimales
        double promedioMs = regs.isEmpty() ? 0.0
                : (sumaSegundos / regs.size()) * 1000.0;
        double promedioRedondeado = Math.round(promedioMs * 100.0) / 100.0;

        String criterio = "Solicitudes registradas entre " + inicio + " y " + fin
                + ", excluyendo: " + (props.log() != null ? props.log().excluirPrefijos() : "[]");

        return new EstadisticasResponse(regs.size(), c2, c4, c5, promedioRedondeado, criterio);
    }
}
