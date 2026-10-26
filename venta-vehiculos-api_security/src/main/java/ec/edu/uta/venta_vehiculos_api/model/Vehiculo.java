// Define el paquete dentro del proyecto donde se ubica la clase del modelo
package ec.edu.uta.venta_vehiculos_api.model;

// Importa la clase BigDecimal para manejar valores numéricos de alta precisión
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Declara la clase como una entidad relacional gestionada por JPA/Hibernate
@Entity
// Asocia la entidad a la tabla llamada "vehiculos" en la base de datos
@Table(name = "vehiculos")
public class Vehiculo {

    // Define este campo como el identificador único (clave primaria) de la entidad
    @Id
    // Aplica la estrategia autoincremental delegada a la base de datos (IDENTITY)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Especifica el nombre de la columna correspondiente en la base de datos ("id_vehiculo")
    @Column(name = "id_vehiculo")
    private Integer idVehiculo;

    // Campo para guardar la marca del vehículo
    private String marca;

    // Campo para guardar el modelo del vehículo
    private String modelo;

    // Campo para guardar la placa o matrícula del vehículo
    private String placa;

    // Campo para guardar el número de chasis del vehículo
    private String chasis;

    // Campo para guardar el año de fabricación del vehículo
    private Integer anio;

    // Campo para guardar el color del vehículo
    private String color;

    // Campo para guardar el precio del vehículo con precisión decimal
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal precio;

    // Campo para guardar el estado actual del vehículo (ej. Disponible, Vendido)
    private String estado;

    // Constructor sin parámetros requerido por el estándar de JPA
    public Vehiculo() {
    }

    // Retorna el ID del vehículo
    public Integer getIdVehiculo() {
        return idVehiculo;
    }

    // Asigna o actualiza el ID del vehículo
    public void setIdVehiculo(Integer idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    // Retorna la marca del vehículo
    public String getMarca() {
        return marca;
    }

    // Asigna o actualiza la marca del vehículo
    public void setMarca(String marca) {
        this.marca = marca;
    }

    // Retorna el modelo del vehículo
    public String getModelo() {
        return modelo;
    }

    // Asigna o actualiza el modelo del vehículo
    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    // Retorna la placa del vehículo
    public String getPlaca() {
        return placa;
    }

    // Asigna o actualiza la placa del vehículo
    public void setPlaca(String placa) {
        this.placa = placa;
    }

    // Retorna el código de chasis del vehículo
    public String getChasis() {
        return chasis;
    }

    // Asigna o actualiza el código de chasis del vehículo
    public void setChasis(String chasis) {
        this.chasis = chasis;
    }

    // Retorna el año de fabricación del vehículo
    public Integer getAnio() {
        return anio;
    }

    // Asigna o actualiza el año de fabricación del vehículo
    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    // Retorna el color del vehículo
    public String getColor() {
        return color;
    }

    // Asigna o actualiza el color del vehículo
    public void setColor(String color) {
        this.color = color;
    }

    // Retorna el precio del vehículo
    public BigDecimal getPrecio() {
        return precio;
    }

    // Asigna o actualiza el precio del vehículo
    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    // Retorna el estado del vehículo
    public String getEstado() {
        return estado;
    }

    // Asigna o actualiza el estado del vehículo
    public void setEstado(String estado) {
        this.estado = estado;
    }
}