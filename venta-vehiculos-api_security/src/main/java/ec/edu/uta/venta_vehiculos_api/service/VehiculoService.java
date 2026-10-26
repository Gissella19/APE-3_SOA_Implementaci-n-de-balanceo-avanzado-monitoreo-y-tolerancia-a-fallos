package ec.edu.uta.venta_vehiculos_api.service;

import java.math.BigDecimal;

import java.time.Year;

import java.util.*;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import ec.edu.uta.venta_vehiculos_api.dto.*;

import ec.edu.uta.venta_vehiculos_api.exception.ApiError;

import ec.edu.uta.venta_vehiculos_api.model.Vehiculo;

import ec.edu.uta.venta_vehiculos_api.repository.*;

@Service

@Transactional
public class VehiculoService {

    private final VehiculoRepository repo;

    private final ReservaRepository reservas;

    public VehiculoService(
            VehiculoRepository repo,
            ReservaRepository reservas) {

        this.repo = repo;

        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar(String marca) {

        var lista = marca == null
                ? repo.findAll()
                : repo.findByMarcaContainingIgnoreCase(marca);

        return lista.stream()
                .map(VehiculoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehiculoResponse obtener(int id) {

        return VehiculoResponse.de(
                repo.findById(id)
                    .orElseThrow(() -> falta())
        );
    }

    public VehiculoResponse crear(VehiculoRequest d) {

        Vehiculo v = new Vehiculo();

        copiar(v, d);

        v.setEstado("DISPONIBLE");

        return VehiculoResponse.de(repo.saveAndFlush(v));
    }

    public VehiculoResponse actualizar(int id, VehiculoRequest d) {

        Vehiculo v = editable(id);

        copiar(v, d);

        return VehiculoResponse.de(repo.saveAndFlush(v));
    }

    public VehiculoResponse precio(int id, Map<String, Object> d) {

        if (d.size() != 1 ||
                !d.containsKey("precio") ||
                !(d.get("precio") instanceof Number)) {

            throw new ApiError(
                    422,
                    "PATCH_INVALIDO",
                    "PATCH admite únicamente precio numérico y no nulo"
            );
        }

        BigDecimal precio;

        try {

            precio = new BigDecimal(d.get("precio").toString());

        } catch (NumberFormatException e) {

            throw new ApiError(
                    422,
                    "PRECIO_INVALIDO",
                    "Precio inválido"
            );
        }

        if (precio.signum() <= 0 ||
                precio.scale() > 2 ||
                precio.compareTo(
                    new BigDecimal("9999999999.99")) > 0) {

            throw new ApiError(
                    422,
                    "PRECIO_INVALIDO",
                    "Precio positivo con máximo dos decimales"
            );
        }

        Vehiculo v = editable(id);

        v.setPrecio(precio);

        return VehiculoResponse.de(repo.saveAndFlush(v));
    }

    public void eliminar(int id) {

        Vehiculo v = editable(id);

        if (reservas.existsByVehiculo_IdVehiculo(id)) {

            throw new ApiError(
                    409,
                    "HISTORIAL_RESERVAS",
                    "Se conserva el vehículo por su historial de reservas"
            );
        }

        repo.delete(v);

        repo.flush();
    }

    private Vehiculo editable(int id) {

        Vehiculo v = repo.bloquear(id)
                .orElseThrow(() -> falta());

        if (!"DISPONIBLE".equals(v.getEstado())) {

            throw new ApiError(
                    409,
                    "VEHICULO_NO_DISPONIBLE",
                    "Un vehículo reservado o vendido no se modifica ni elimina"
            );
        }

        return v;
    }

    private ApiError falta() {

        return new ApiError(
                404,
                "VEHICULO_NO_ENCONTRADO",
                "El vehículo no existe"
        );
    }

    private void copiar(Vehiculo v, VehiculoRequest d) {

        if (d.anio() < 1900 ||
                d.anio() > Year.now().getValue() + 1) {

            throw new ApiError(
                    422,
                    "ANIO_INVALIDO",
                    "Año fuera de rango"
            );
        }

        v.setMarca(d.marca().trim());

        v.setModelo(d.modelo().trim());

        v.setPlaca(
                d.placa().trim().toUpperCase(Locale.ROOT)
        );

        v.setChasis(
                d.chasis().trim().toUpperCase(Locale.ROOT)
        );

        if (v.getChasis().length() != 17) {

            throw new ApiError(
                    422,
                    "CHASIS_INVALIDO",
                    "Chasis de 17 caracteres"
            );
        }

        v.setAnio(d.anio());

        v.setColor(d.color());

        v.setPrecio(d.precio());
    }

}