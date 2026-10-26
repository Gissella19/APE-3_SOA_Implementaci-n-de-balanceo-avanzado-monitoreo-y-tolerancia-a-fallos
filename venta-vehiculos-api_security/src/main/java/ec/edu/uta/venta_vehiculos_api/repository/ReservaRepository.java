package ec.edu.uta.venta_vehiculos_api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ec.edu.uta.venta_vehiculos_api.model.Reserva;

public interface ReservaRepository
 extends JpaRepository<Reserva,String> {

 Optional<Reserva> findByOperacionId(String operacionId);
 boolean existsByVehiculo_IdVehiculo(Integer id);
}
