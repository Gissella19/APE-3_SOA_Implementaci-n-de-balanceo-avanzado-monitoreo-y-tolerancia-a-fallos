package ec.edu.uta.venta_vehiculos_api.repository;

import java.util.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;

import ec.edu.uta.venta_vehiculos_api.model.Vehiculo;

public interface VehiculoRepository

    extends JpaRepository<Vehiculo, Integer> {

    Optional<Vehiculo> findByPlaca(String placa);

    Optional<Vehiculo> findByChasis(String chasis);

    List<Vehiculo> findByMarcaContainingIgnoreCase(String marca);

    @Lock(LockModeType.PESSIMISTIC_WRITE)

    @Query("select v from Vehiculo v where v.idVehiculo = :id")

    Optional<Vehiculo> bloquear(@Param("id") Integer id);

}