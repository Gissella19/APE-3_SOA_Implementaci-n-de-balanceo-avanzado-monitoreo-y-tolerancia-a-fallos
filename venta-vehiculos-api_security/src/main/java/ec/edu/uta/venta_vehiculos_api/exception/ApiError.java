package ec.edu.uta.venta_vehiculos_api.exception;

public class ApiError extends RuntimeException {
 public final int status;
 public final String codigo;
 public ApiError(int status, String codigo, String detalle) {
  super(detalle);
  this.status = status;
  this.codigo = codigo;
 }
}
