package ec.edu.uta.monitor.service;

import ec.edu.uta.monitor.config.MonitorProperties;
import ec.edu.uta.monitor.dto.BalanceoResponse;
import ec.edu.uta.monitor.dto.RegistroLog;
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
import org.springframework.stereotype.Service;
import ec.edu.uta.monitor.dto.EstadisticasResponse;

@Service
public class LogMetricsService {

    private static final Pattern CAMPO = Pattern.compile("\"(\\w+)\":\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final MonitorProperties props;

    public LogMetricsService(MonitorProperties props) {
        this.props = props;
    }

    private OffsetDateTime inicioVentana() {
        OffsetDateTime inicio = OffsetDateTime.now().minusMinutes(props.log().ventanaMinutos());
        String config = props.log().inicio();
        if (config != null && !config.isBlank()) {
            OffsetDateTime fijo = OffsetDateTime.parse(config.trim());
            if (fijo.isAfter(inicio)) {
                inicio = fijo;
            }
        }
        return inicio;
    }

    private List<RegistroLog> leerVentana(OffsetDateTime inicio) {
        List<RegistroLog> registros = new ArrayList<>();
        Path ruta = Path.of(props.log().ruta());
        if (!Files.exists(ruta)) {
            return registros;
        }
        String contenido;
        try {
            contenido = new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return registros;
        }
        for (String linea : contenido.split("\\R")) {
            RegistroLog r = parsear(linea);
            if (r != null && !r.timestamp().isBefore(inicio) && !excluido(r.uri())) {
                registros.add(r);
            }
        }
        return registros;
    }

    private boolean excluido(String uri) {
        return props.log().excluirPrefijos().stream()
                .anyMatch(p -> !p.isBlank() && uri.startsWith(p));
    }

    private RegistroLog parsear(String linea) {
        if (!linea.startsWith("{")) {
            return null;
        }
        Map<String, String> c = new HashMap<>();
        Matcher m = CAMPO.matcher(linea);
        while (m.find()) {
            c.put(m.group(1), m.group(2));
        }
        try {
            return new RegistroLog(
                    OffsetDateTime.parse(c.get("timestamp")),
                    c.get("method"),
                    c.get("uri"),
                    Integer.parseInt(c.get("status")),
                    c.getOrDefault("upstream_addr", "-"),
                    Double.parseDouble(c.getOrDefault("request_time", "0")));
        } catch (Exception e) {
            return null;
        }
    }

    // Con reintentos NGINX escribe varios upstreams separados por coma: cuenta solo el ultimo
    private String upstreamFinal(String addr) {
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
        String puertoA = ":" + props.instancias().get(0).puerto();
        String puertoB = ":" + props.instancias().get(1).puerto();
        long a = 0;
        long b = 0;
        long sin = 0;
        for (RegistroLog r : regs) {
            String up = upstreamFinal(r.upstreamAddr());
            if (up.endsWith(puertoA)) {
                a++;
            } else if (up.endsWith(puertoB)) {
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
        double suma = 0;
        for (RegistroLog r : regs) {
            int s = r.status();
            if (s >= 200 && s < 300) {
                c2++;
            } else if (s >= 400 && s < 500) {
                c4++;
            } else if (s >= 500) {
                c5++;
            }
            suma += r.requestTimeSeg();
        }
        double promedio = regs.isEmpty() ? 0.0
                : Math.round(suma / regs.size() * 100000.0) / 100.0;
        String criterio = "Solicitudes finales de vehiculos entre " + inicio + " y " + fin
                + ", sin " + props.log().excluirPrefijos();
        return new EstadisticasResponse(regs.size(), c2, c4, c5, promedio, criterio);
    }
}