package ec.edu.uta.venta_vehiculos_api.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import ec.edu.uta.venta_vehiculos_api.repository.UsuarioApiRepository;
import ec.edu.uta.venta_vehiculos_api.model.UsuarioApi;

@Configuration

@Profile("local")
public class LocalUsers {

 @Bean CommandLineRunner usuarioInicial(UsuarioApiRepository repo,
  PasswordEncoder encoder,
  @Value("${app.bootstrap.user}") String user,
  @Value("${app.bootstrap.password}") String password,
  @Value("${app.bootstrap.role:ADMIN}") String rol) {

  return args -> {

   if (!java.util.List.of("ADMIN","OPERADOR","CONSULTA").contains(rol))

    throw new IllegalArgumentException("Rol inicial inválido");

   if (repo.findByUsername(user).isEmpty()) {
    UsuarioApi u=new UsuarioApi(); u.setUsername(user);
    u.setPassword(encoder.encode(password)); u.setRol(rol);

    repo.save(u);
   }
  };
 }
}
