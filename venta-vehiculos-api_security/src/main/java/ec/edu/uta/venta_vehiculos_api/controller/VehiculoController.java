package ec.edu.uta.venta_vehiculos_api.controller;

import java.net.URI;

import java.util.*;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import ec.edu.uta.venta_vehiculos_api.dto.*;

import ec.edu.uta.venta_vehiculos_api.service.VehiculoService;
import ec.edu.uta.venta_vehiculos_api.client.NotificacionClient;
import org.springframework.core.env.Environment;

@RestController

@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService service;
    private final NotificacionClient avisos;
    private final Environment environment;

    public VehiculoController(
            VehiculoService service,
            NotificacionClient avisos,
            Environment environment) {
        this.service = service;
        this.avisos = avisos;
        this.environment = environment;
    }

    @GetMapping("/instancia")
    public InstanciaResponse obtenerInstancia() {
        int puerto = environment.getProperty("server.port", Integer.class, 8080);
        String instancia = environment.getProperty("app.instancia.id", "instancia-" + puerto);
        return new InstanciaResponse(instancia, puerto);
    }

    @GetMapping
    public List<VehiculoResponse> listar(

            @RequestParam(required = false) String marca) {

        return service.listar(marca);
    }

    @GetMapping("/{id}")
    public VehiculoResponse obtener(

            @PathVariable int id) {

        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<VehiculoResponse> crear(

            @Valid @RequestBody VehiculoRequest d) {

        var r = service.crear(d);

        avisos.enviar(
                r.idVehiculo(),
                "REGISTRO",
                "Vehículo registrado"
        );

        return ResponseEntity.created(
                URI.create("/api/vehiculos/" + r.idVehiculo())
        ).body(r);
    }

    @PutMapping("/{id}")
    public VehiculoResponse actualizar(

            @PathVariable int id,

            @Valid @RequestBody VehiculoRequest d) {

        var r = service.actualizar(id, d);

        avisos.enviar(
                id,
                "ACTUALIZACION",
                "Vehículo actualizado"
        );

        return r;
    }

    @PatchMapping(
            value = "/{id}",
            consumes = "application/merge-patch+json"
    )
    public VehiculoResponse precio(

            @PathVariable int id,

            @RequestBody Map<String, Object> d) {

        var r = service.precio(id, d);

        avisos.enviar(
                id,
                "ACTUALIZACION",
                "Precio actualizado"
        );

        return r;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(

            @PathVariable int id) {

        service.eliminar(id);

        avisos.enviar(
                id,
                "ELIMINACION",
                "Vehículo eliminado"
        );

        return ResponseEntity.noContent().build();
    }

}