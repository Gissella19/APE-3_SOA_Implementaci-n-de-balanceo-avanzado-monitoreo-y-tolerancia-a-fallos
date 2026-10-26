package ec.edu.uta.venta_vehiculos_api.dto;

import java.math.BigDecimal;

import ec.edu.uta.venta_vehiculos_api.model.Vehiculo;

public record VehiculoResponse(

    Integer idVehiculo,

    String marca, String modelo,

    String placa, String chasis, Integer anio,

    String color, BigDecimal precio, String estado

) {

    public static VehiculoResponse de(Vehiculo v) {

        return new VehiculoResponse(

            v.getIdVehiculo(),
            v.getMarca(),
            v.getModelo(),
            v.getPlaca(),
            v.getChasis(),
            v.getAnio(),
            v.getColor(),
            v.getPrecio(),
            v.getEstado()
        );
    }

}
