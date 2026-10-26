package ec.edu.uta.notificaciones.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class NotificationKeyFilter extends OncePerRequestFilter {

    private final String key;

    public NotificationKeyFilter(
            @Value("${app.security.notificaciones-key:${app.security.notification-key:${servicios.notificaciones.key:avisos-java-python-aula-2026-key-7G5nP3rQ8c6L2t}}}") String key) {
        if (key == null || key.length() < 32) {
            throw new IllegalStateException("Clave de servicio corta o nula");
        }
        this.key = key;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain)
            throws IOException, ServletException {
        String path = req.getRequestURI();
        boolean interna = path.equals("/api/notificaciones") ||
                path.startsWith("/api/notificaciones/");

        if (interna) {
            String enviada = req.getHeader("X-Notification-Key");
            if (enviada == null) {
                enviada = req.getHeader("X-Service-Key");
            }

            if (enviada == null || !MessageDigest.isEqual(
                    key.getBytes(StandardCharsets.UTF_8),
                    enviada.getBytes(StandardCharsets.UTF_8))) {
                res.setStatus(401);
                res.setContentType("application/problem+json");
                res.getWriter().write("{\"title\":\"Servicio no autorizado\",\"status\":401}");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
