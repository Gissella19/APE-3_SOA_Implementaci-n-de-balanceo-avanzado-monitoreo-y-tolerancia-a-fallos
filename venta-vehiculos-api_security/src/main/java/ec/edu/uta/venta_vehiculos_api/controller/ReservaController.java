package ec.edu.uta.venta_vehiculos_api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ec.edu.uta.venta_vehiculos_api.dto.*;
import ec.edu.uta.venta_vehiculos_api.service.ReservaService;

@RestController
public class ReservaController {
 private final ReservaService service;

 public ReservaController(ReservaService service) {
  this.service=service;
 }

 @PostMapping("/api/vehiculos/{id}/reservas")

 public ReservaResponse reservar(@PathVariable int id,
   @Valid @RequestBody ReservaRequest d) {

  return service.reservar(id,d.operacionId());
 }

 @GetMapping("/api/reservas/{id}")

 public ReservaResponse obtener(@PathVariable String id) {

  return service.obtener(id);
 }

 @GetMapping("/api/reservas")

 public ReservaResponse operacion(@RequestParam String operacionId) {

  return service.porOperacion(operacionId);
 }

 @PutMapping("/api/reservas/{id}/confirmacion")

 public ReservaResponse confirmar(@PathVariable String id) {

  return service.confirmar(id);
 }

 @DeleteMapping("/api/reservas/{id}")

 public ResponseEntity<Void> liberar(@PathVariable String id) {
  service.liberar(id); return ResponseEntity.noContent().build();
 }
}
