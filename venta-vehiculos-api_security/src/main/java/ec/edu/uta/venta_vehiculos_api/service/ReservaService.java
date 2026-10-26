package ec.edu.uta.venta_vehiculos_api.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import ec.edu.uta.venta_vehiculos_api.dto.*;
import ec.edu.uta.venta_vehiculos_api.exception.ApiError;
import ec.edu.uta.venta_vehiculos_api.model.*;
import ec.edu.uta.venta_vehiculos_api.repository.*;

@Service

@Transactional(isolation=Isolation.READ_COMMITTED)
public class ReservaService {
 private final VehiculoRepository vehiculos;
 private final ReservaRepository reservas;

 public ReservaService(VehiculoRepository v, ReservaRepository r) {
  vehiculos=v; reservas=r;
 }

 public ReservaResponse reservar(int id, String op) {

  Vehiculo v=vehiculos.bloquear(id).orElseThrow(() ->
   new ApiError(404,"VEHICULO_NO_ENCONTRADO","Vehículo inexistente"));

  var anterior=reservas.findByOperacionId(op);

  if (anterior.isPresent()) {
   Reserva r=anterior.get();

   if (!r.vehiculo.getIdVehiculo().equals(id))

    throw conflicto("La operación pertenece a otro vehículo");

   return ReservaResponse.de(r);
  }

  if (!"DISPONIBLE".equals(v.getEstado()))

   throw conflicto("El vehículo no está disponible");
  Reserva r=new Reserva();
  r.id=UUID.randomUUID().toString(); r.operacionId=op;
  r.vehiculo=v; r.precio=v.getPrecio();
  r.moneda="USD"; r.estado="RESERVADA";
  v.setEstado("RESERVADO");
  reservas.saveAndFlush(r);

  return ReservaResponse.de(r);
 }

 @Transactional(readOnly=true)

 public ReservaResponse obtener(String id) {

  return ReservaResponse.de(buscar(id));
 }

 @Transactional(readOnly=true)

 public ReservaResponse porOperacion(String op) {

  return ReservaResponse.de(reservas.findByOperacionId(op)
   .orElseThrow(() -> new ApiError(404,"RESERVA_NO_ENCONTRADA",
    "La operación no tiene reserva")));
 }

 public ReservaResponse confirmar(String id) {
  Reserva r=bloquear(id);

  if ("LIBERADA".equals(r.estado))

   throw conflicto("La reserva fue liberada");

  if ("RESERVADA".equals(r.estado)) {

   if (!"RESERVADO".equals(r.vehiculo.getEstado()))

    throw conflicto("Estado del vehículo inconsistente");
   r.estado="CONFIRMADA"; r.vehiculo.setEstado("VENDIDO");
  }

  return ReservaResponse.de(r);
 }

 public void liberar(String id) {
  Reserva r=bloquear(id);

  if ("CONFIRMADA".equals(r.estado))

   throw conflicto("No se libera una reserva confirmada");

  if ("RESERVADA".equals(r.estado)) {
   r.estado="LIBERADA"; r.vehiculo.setEstado("DISPONIBLE");
  }
 }

 private Reserva bloquear(String id) {
  Reserva r=buscar(id);

  vehiculos.bloquear(r.vehiculo.getIdVehiculo()).orElseThrow();

  return refrescar(r);
 }
 @jakarta.persistence.PersistenceContext
 private jakarta.persistence.EntityManager em;

 private Reserva refrescar(Reserva r) {
  em.refresh(r); em.refresh(r.vehiculo); return r;
 }

 private Reserva buscar(String id) {

  return reservas.findById(id).orElseThrow(() ->
   new ApiError(404,"RESERVA_NO_ENCONTRADA","Reserva inexistente"));
 }

 private ApiError conflicto(String detalle) {

  return new ApiError(409,"CONFLICTO_RESERVA",detalle);
 }
}
