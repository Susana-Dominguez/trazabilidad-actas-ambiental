package cr.go.heredia.actas.model;

public enum TipoActa {
    INSPECCION("Inspección"),
    NOTIFICACION("Notificación"),
    OCULAR("Ocular"),
    FISCALIZACION("Fiscalización");

    private final String etiqueta;

    TipoActa(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
