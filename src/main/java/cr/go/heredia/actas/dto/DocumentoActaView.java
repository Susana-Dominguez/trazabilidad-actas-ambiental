package cr.go.heredia.actas.dto;

import java.time.LocalDateTime;

public class DocumentoActaView {

    private final String nombreArchivo;
    private final String estadoFirma;
    private final LocalDateTime fechaCarga;
    private final boolean pdf;
    private final boolean word;
    private final Long actaId;

    public DocumentoActaView(String nombreArchivo, String estadoFirma, LocalDateTime fechaCarga,
                             boolean pdf, boolean word, Long actaId) {
        this.nombreArchivo = nombreArchivo;
        this.estadoFirma = estadoFirma;
        this.fechaCarga = fechaCarga;
        this.pdf = pdf;
        this.word = word;
        this.actaId = actaId;
    }

    public String getNombreArchivo() { return nombreArchivo; }
    public String getEstadoFirma() { return estadoFirma; }
    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public boolean isPdf() { return pdf; }
    public boolean isWord() { return word; }
    public Long getActaId() { return actaId; }
    public boolean isPuedeVer() { return pdf; }
    public boolean isPuedeDescargar() { return nombreArchivo != null; }
}
