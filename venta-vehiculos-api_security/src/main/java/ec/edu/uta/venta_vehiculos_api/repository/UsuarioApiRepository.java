package ec.edu.uta.venta_vehiculos_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.uta.venta_vehiculos_api.model.UsuarioApi;

public interface UsuarioApiRepository
        extends JpaRepository<UsuarioApi, Integer> {

    Optional<UsuarioApi> findByUsername(String username);
}