package ec.edu.uta.venta_vehiculos_api.dto;

public class NotificacionRequest {

    private Integer vehiculoId;
    private String destinatario;
    private String tipo;
    private String mensaje;

    public NotificacionRequest() {
    }

    public NotificacionRequest(
            Integer vehiculoId,
            String destinatario,
            String tipo,
            String mensaje) {

        this.vehiculoId = vehiculoId;
        this.destinatario = destinatario;
        this.tipo = tipo;
        this.mensaje = mensaje;
    }

    public Integer getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(Integer vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}