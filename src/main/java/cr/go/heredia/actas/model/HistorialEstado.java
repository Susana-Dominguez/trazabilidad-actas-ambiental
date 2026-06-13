package cr.go.heredia.actas.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_estados")
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acta_id")
    private Acta acta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoActa estado;

    @Column(nullable = false)
    private LocalDateTime fechaCambio = LocalDateTime.now();

    @Column(length = 255)
    private String observacion;

    @Column(length = 80)
    private String usuario;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Acta getActa() { return acta; }
    public void setActa(Acta acta) { this.acta = acta; }
    public EstadoActa getEstado() { return estado; }
    public void setEstado(EstadoActa estado) { this.estado = estado; }
    public LocalDateTime getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(LocalDateTime fechaCambio) { this.fechaCambio = fechaCambio; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
