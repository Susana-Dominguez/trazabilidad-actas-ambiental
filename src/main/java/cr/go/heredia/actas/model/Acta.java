package cr.go.heredia.actas.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "actas")
public class Acta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String consecutivo;

    @Column(length = 150)
    private String empresa;

    @Column(length = 60)
    private String numeroContrato;

    @Column(nullable = false, length = 120)
    private String inspector;

    @Column(nullable = false)
    private LocalDate fechaActa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoActa estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenActa origen;

    @Column(length = 80)
    private String tipoActa;

    @Column(length = 255)
    private String referenciaCaso;

    @Column(length = 255)
    private String rutaDocumento;

    @Column(length = 120)
    private String correoOrigen;

    private LocalDateTime fechaRegistroSistema = LocalDateTime.now();

    private LocalDateTime ultimaActualizacion = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getRutaDocumento() { return rutaDocumento; }
    public void setRutaDocumento(String rutaDocumento) { this.rutaDocumento = rutaDocumento; }
    public String getCorreoOrigen() { return correoOrigen; }
    public void setCorreoOrigen(String correoOrigen) { this.correoOrigen = correoOrigen; }
    public LocalDateTime getFechaRegistroSistema() { return fechaRegistroSistema; }
    public void setFechaRegistroSistema(LocalDateTime fechaRegistroSistema) { this.fechaRegistroSistema = fechaRegistroSistema; }
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
}
