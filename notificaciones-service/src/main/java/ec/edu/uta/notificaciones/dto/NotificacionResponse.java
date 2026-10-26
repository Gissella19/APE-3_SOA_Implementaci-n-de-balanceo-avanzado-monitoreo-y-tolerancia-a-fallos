package ec.edu.uta.notificaciones.dto;

public class NotificacionResponse {

    private boolean success;
    private String estado;
    private String mensaje;

    public NotificacionResponse() {
    }

    public NotificacionResponse(boolean success, String estado, String mensaje) {
        this.success = success;
        this.estado = estado;
        this.mensaje = mensaje;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
