package ec.edu.uta.venta_vehiculos_api.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
public class IntegrationFilter extends OncePerRequestFilter {
 private final String key;

 public IntegrationFilter(@Value("${app.security.ventas-key}") String key) {

  if (key.length()<32) throw new IllegalStateException("Clave de servicio corta");
  this.key=key;
 }
 @Override
 protected void doFilterInternal(HttpServletRequest req,
   HttpServletResponse res, FilterChain chain)
   throws IOException, ServletException {
  String path=req.getRequestURI();
  boolean interna=path.equals("/api/reservas") ||
   path.startsWith("/api/reservas/") ||
   path.matches("/api/vehiculos/[0-9]+/reservas");

  if (interna) {

   String enviada=req.getHeader("X-Service-Key");

   if (enviada==null || !MessageDigest.isEqual(
    key.getBytes(StandardCharsets.UTF_8),
    enviada.getBytes(StandardCharsets.UTF_8))) {
    res.setStatus(401); res.setContentType("application/problem+json");
    res.getWriter().write("{\"title\":\"Servicio no autorizado\",\"status\":401}");

    return;
   }

   SecurityContextHolder.getContext().setAuthentication(
    new UsernamePasswordAuthenticationToken("ventas-api",null,
     List.of(new SimpleGrantedAuthority("ROLE_SERVICIO_VENTAS"))));
  }
  chain.doFilter(req,res);
 }
}
