package cr.go.heredia.actas.dto;

import cr.go.heredia.actas.model.EstadoActa;
import cr.go.heredia.actas.model.OrigenActa;
import cr.go.heredia.actas.model.TipoActa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ActaFormDto {

    @NotBlank(message = "El consecutivo es obligatorio")
    private String consecutivo;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotBlank(message = "El número de contrato es obligatorio")
    private String numeroContrato;

    @NotBlank(message = "El inspector es obligatorio")
    private String inspector;

    @NotNull(message = "La fecha es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaActa;

    @NotNull(message = "El estado es obligatorio")
    private EstadoActa estado;

    @NotNull(message = "El origen es obligatorio")
    private OrigenActa origen;

    @NotNull(message = "El tipo de acta es obligatorio")
    private TipoActa tipoActa;
    private String referenciaCaso;
    private String correoOrigen;
    private String observacionHistorial;
    private String usuario = "gestor.demo";

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
    public TipoActa getTipoActa() { return tipoActa; }
    public void setTipoActa(TipoActa tipoActa) { this.tipoActa = tipoActa; }
    public String getReferenciaCaso() { return referenciaCaso; }
    public void setReferenciaCaso(String referenciaCaso) { this.referenciaCaso = referenciaCaso; }
    public String getCorreoOrigen() { return correoOrigen; }
    public void setCorreoOrigen(String correoOrigen) { this.correoOrigen = correoOrigen; }
    public String getObservacionHistorial() { return observacionHistorial; }
    public void setObservacionHistorial(String observacionHistorial) { this.observacionHistorial = observacionHistorial; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
