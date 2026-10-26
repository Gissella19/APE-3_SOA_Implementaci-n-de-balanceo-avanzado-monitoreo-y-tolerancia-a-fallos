package ec.edu.uta.venta_vehiculos_api.security;

import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtService jwt;

 public JwtAuthenticationFilter(JwtService jwt) { this.jwt=jwt; }
 @Override
 protected void doFilterInternal(HttpServletRequest req,
   HttpServletResponse res, FilterChain chain)
   throws IOException, ServletException {

  String h=req.getHeader("Authorization");

  if (SecurityContextHolder.getContext().getAuthentication()==null &&
      h!=null && h.startsWith("Bearer ")) {

   try {
    var c=jwt.validarToken(h.substring(7));

    if (!"access".equals(c.get("tipo",String.class)))

     throw new IllegalArgumentException("Se requiere access token");
    String rol=c.get("rol",String.class);

    if (!List.of("CONSULTA","OPERADOR","ADMIN").contains(rol))

     throw new IllegalArgumentException("Rol inválido");

    SecurityContextHolder.getContext().setAuthentication(
     new UsernamePasswordAuthenticationToken(c.getSubject(),null,
      List.of(new SimpleGrantedAuthority("ROLE_"+rol))));
   } catch (Exception ex) { SecurityContextHolder.clearContext(); }
  }
  chain.doFilter(req,res);
 }
}