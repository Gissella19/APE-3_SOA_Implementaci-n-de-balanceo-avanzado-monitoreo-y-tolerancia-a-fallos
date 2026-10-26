package ec.edu.uta.venta_vehiculos_api.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

 @Bean
 public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter>
 desactivarJwtServlet(JwtAuthenticationFilter f) {
  var b=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(f);
  b.setEnabled(false); return b;
 }

 @Bean
 public org.springframework.boot.web.servlet.FilterRegistrationBean<IntegrationFilter>
 desactivarIntegracionServlet(IntegrationFilter f) {
  var b=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(f);
  b.setEnabled(false); return b;
 }

 @Bean public PasswordEncoder passwordEncoder() {

  return new BCryptPasswordEncoder();
 }

 @Bean

 public SecurityFilterChain securityFilterChain(HttpSecurity http,
   JwtAuthenticationFilter jwt, IntegrationFilter integration)
   throws Exception {

  return http.csrf(c -> c.disable())
   .sessionManagement(s -> s.sessionCreationPolicy(
    SessionCreationPolicy.STATELESS))
   .exceptionHandling(e -> e
    .authenticationEntryPoint((q,r,x) -> {
     r.setStatus(401); r.setContentType("application/problem+json");
     r.getWriter().write("{\"title\":\"No autenticado\",\"status\":401}");
    })
    .accessDeniedHandler((q,r,x) -> {
     r.setStatus(403); r.setContentType("application/problem+json");
     r.getWriter().write("{\"title\":\"Sin permiso\",\"status\":403}");
    }))
   .authorizeHttpRequests(a -> a
    .requestMatchers("/oauth/token").permitAll()
    .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
    .requestMatchers("/api/reservas", "/api/reservas/**",
     "/api/vehiculos/*/reservas").hasRole("SERVICIO_VENTAS")
    .requestMatchers("/api/identidad")
     .hasAnyRole("CONSULTA","OPERADOR","ADMIN")
    .requestMatchers(HttpMethod.GET,"/api/vehiculos/**")
     .hasAnyRole("CONSULTA","OPERADOR","ADMIN")
    .requestMatchers(HttpMethod.POST,"/api/vehiculos/**")
     .hasAnyRole("OPERADOR","ADMIN")
    .requestMatchers(HttpMethod.PUT,"/api/vehiculos/**")
     .hasAnyRole("OPERADOR","ADMIN")
    .requestMatchers(HttpMethod.PATCH,"/api/vehiculos/**")
     .hasAnyRole("OPERADOR","ADMIN")
    .requestMatchers(HttpMethod.DELETE,"/api/vehiculos/**")
     .hasRole("ADMIN")
    .anyRequest().denyAll())
   .addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class)
   .addFilterBefore(integration,JwtAuthenticationFilter.class)
   .build();
 }
}