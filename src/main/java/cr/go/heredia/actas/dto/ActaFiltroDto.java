package cr.go.heredia.actas.dto;

import cr.go.heredia.actas.model.EstadoActa;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ActaFiltroDto {
    private String empresa;
    private String numeroContrato;
    private String consecutivo;
    private String inspector;
    private EstadoActa estado;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDesde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaHasta;

    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }
    public String getNumeroContrato() { return numeroContrato; }
    public void setNumeroContrato(String numeroContrato) { this.numeroContrato = numeroContrato; }
    public String getConsecutivo() { return consecutivo; }
    public void setConsecutivo(String consecutivo) { this.consecutivo = consecutivo; }
    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }
    public EstadoActa getEstado() { return estado; }
    public void setEstado(EstadoActa estado) { this.estado = estado; }
    public LocalDate getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(LocalDate fechaDesde) { this.fechaDesde = fechaDesde; }
    public LocalDate getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(LocalDate fechaHasta) { this.fechaHasta = fechaHasta; }
}
