package cr.go.heredia.actas.dto;

import cr.go.heredia.actas.model.EstadoActa;
import cr.go.heredia.actas.model.OrigenActa;
import cr.go.heredia.actas.model.TipoActa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Cuerpo JSON para POST /api/actas (integración Power Automate / Outlook).
 */
public class ActaApiRequest {

    @NotBlank
    @Size(max = 80)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$")
    private String consecutivo;

    private String empresa;

    private String numeroContrato;

    @NotBlank
    private String inspector;

    @NotNull
    private LocalDate fechaActa;

    private EstadoActa estado = EstadoActa.RECIBIDA_CORREO;

    private OrigenActa origen = OrigenActa.SURVEY123;

    private String tipoActa;
    private String referenciaCaso;
    private String correoOrigen;
    private String observacionHistorial = "Registro automático vía API";
    private String usuario = "integracion.outlook";

    public ActaFormDto toFormDto() {
        ActaFormDto dto = new ActaFormDto();
        dto.setConsecutivo(consecutivo);
        dto.setEmpresa(empresa);
        dto.setNumeroContrato(numeroContrato);
        dto.setInspector(inspector);
        dto.setFechaActa(fechaActa);
        dto.setEstado(estado != null ? estado : EstadoActa.RECIBIDA_CORREO);
        dto.setOrigen(origen != null ? origen : OrigenActa.SURVEY123);
        dto.setTipoActa(resolverTipoActa(tipoActa));
        dto.setReferenciaCaso(referenciaCaso);
        dto.setCorreoOrigen(correoOrigen);
        dto.setObservacionHistorial(observacionHistorial);
        dto.setUsuario(usuario);
        return dto;
    }

    private TipoActa resolverTipoActa(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (TipoActa tipo : TipoActa.values()) {
            if (tipo.getEtiqueta().equalsIgnoreCase(valor.trim()) || tipo.name().equalsIgnoreCase(valor.trim())) {
                return tipo;
            }
        }
        return null;
    }

    public String getConsecutivo() { return consecutivo; }
    public void setConsecutivo(String consecutivo) { this.consecutivo = consecutivo; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }
    public String getNumeroContrato() { return numeroContrato; }
    public void setNumeroContrato(String numeroContrato) { this.numeroContrato = numeroContrato; }
    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }
    public LocalDate getFechaActa() { return fechaActa; }
    public void setFechaActa(LocalDate fechaActa) { this.fechaActa = fechaActa; }
    public EstadoActa getEstado() { return estado; }
    public void setEstado(EstadoActa estado) { this.estado = estado; }
    public OrigenActa getOrigen() { return origen; }
    public void setOrigen(OrigenActa origen) { this.origen = origen; }
    public String getTipoActa() { return tipoActa; }
    public void setTipoActa(String tipoActa) { this.tipoActa = tipoActa; }
    public String getReferenciaCaso() { return referenciaCaso; }
    public void setReferenciaCaso(String referenciaCaso) { this.referenciaCaso = referenciaCaso; }
    public String getCorreoOrigen() { return correoOrigen; }
    public void setCorreoOrigen(String correoOrigen) { this.correoOrigen = correoOrigen; }
    public String getObservacionHistorial() { return observacionHistorial; }
    public void setObservacionHistorial(String observacionHistorial) { this.observacionHistorial = observacionHistorial; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
