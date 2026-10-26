package ec.edu.uta.venta_vehiculos_api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ec.edu.uta.venta_vehiculos_api.model.UsuarioApi;
import ec.edu.uta.venta_vehiculos_api.repository.UsuarioApiRepository;

@Service
public class UsuarioApiService {

    private final UsuarioApiRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioApiService(
            UsuarioApiRepository repository,
            PasswordEncoder passwordEncoder) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioApi autenticar(
            String username,
            String password) {

        UsuarioApi usuario =
                repository.findByUsername(username)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Usuario o contraseña incorrectos"
                                )
                        );

        if (!passwordEncoder.matches(
                password,
                usuario.getPassword())) {

            throw new IllegalArgumentException(
                    "Usuario o contraseña incorrectos"
            );
        }

        return usuario;
    }
}