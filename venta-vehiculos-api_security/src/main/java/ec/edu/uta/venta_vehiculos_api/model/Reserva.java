package ec.edu.uta.venta_vehiculos_api.model;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity

@Table(name="reservas")
public class Reserva {
 @Id @Column(length=36)
 public String id;
 @Column(name="operacion_id", nullable=false, unique=true, length=64)
 public String operacionId;
 @ManyToOne(fetch=FetchType.LAZY, optional=false)
 @JoinColumn(name="vehiculo_id", nullable=false)
 public Vehiculo vehiculo;
 @Column(nullable=false, precision=12, scale=2)
 public BigDecimal precio;
 @Column(nullable=false, length=3)
 public String moneda;
 @Column(nullable=false, length=20)
 public String estado;

 public Reserva() {}
}
