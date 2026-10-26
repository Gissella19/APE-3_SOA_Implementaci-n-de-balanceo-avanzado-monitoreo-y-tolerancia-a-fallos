package ec.edu.uta.venta_vehiculos_api.dto;

public class NotificacionResponse {

    private boolean success;
    private String estado;
    private String mensaje;

    public NotificacionResponse(){

    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getEstado() {
        return estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    
    

    
}
