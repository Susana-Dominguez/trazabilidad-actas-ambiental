package cr.go.heredia.actas.dto;

import cr.go.heredia.actas.model.Acta;
import cr.go.heredia.actas.model.EstadoActa;
import cr.go.heredia.actas.model.OrigenActa;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ActaApiResponse {

    private Long id;
    private String consecutivo;
    private String empresa;
    private String numeroContrato;
    private String inspector;
    private LocalDate fechaActa;
    private EstadoActa estado;
    private String estadoEtiqueta;
    private OrigenActa origen;
    private String tipoActa;
    private String referenciaCaso;
    private String correoOrigen;
    private LocalDateTime fechaRegistroSistema;
    private String enlaceDetalle;

    public static ActaApiResponse from(Acta acta, String baseUrl) {
        ActaApiResponse r = new ActaApiResponse();
        r.id = acta.getId();
        r.consecutivo = acta.getConsecutivo();
        r.empresa = acta.getEmpresa();
        r.numeroContrato = acta.getNumeroContrato();
        r.inspector = acta.getInspector();
        r.fechaActa = acta.getFechaActa();
        r.estado = acta.getEstado();
        r.estadoEtiqueta = acta.getEstado().getEtiqueta();
        r.origen = acta.getOrigen();
        r.tipoActa = acta.getTipoActa();
        r.referenciaCaso = acta.getReferenciaCaso();
        r.correoOrigen = acta.getCorreoOrigen();
        r.fechaRegistroSistema = acta.getFechaRegistroSistema();
        r.enlaceDetalle = baseUrl + "/actas/" + acta.getId();
        return r;
    }

    public Long getId() { return id; }
    public String getConsecutivo() { return consecutivo; }
    public String getEmpresa() { return empresa; }
    public String getNumeroContrato() { return numeroContrato; }
    public String getInspector() { return inspector; }
    public LocalDate getFechaActa() { return fechaActa; }
    public EstadoActa getEstado() { return estado; }
    public String getEstadoEtiqueta() { return estadoEtiqueta; }
    public OrigenActa getOrigen() { return origen; }
    public String getTipoActa() { return tipoActa; }
    public String getReferenciaCaso() { return referenciaCaso; }
    public String getCorreoOrigen() { return correoOrigen; }
    public LocalDateTime getFechaRegistroSistema() { return fechaRegistroSistema; }
    public String getEnlaceDetalle() { return enlaceDetalle; }
}
