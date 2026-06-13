package cr.go.heredia.actas.dto;

import cr.go.heredia.actas.model.EstadoActa;
import jakarta.validation.constraints.NotNull;

public class ActaEstadoApiRequest {

    @NotNull
    private EstadoActa estado;

    private String observacion;
    private String usuario = "integracion.outlook";

    public EstadoActa getEstado() { return estado; }
    public void setEstado(EstadoActa estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
