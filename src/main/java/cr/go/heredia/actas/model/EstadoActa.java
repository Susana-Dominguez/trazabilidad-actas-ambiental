package cr.go.heredia.actas.model;

public enum EstadoActa {
    RECIBIDA_CORREO("Recibida por correo"),
    POR_FIRMAR("Por firmar"),
    FIRMADA("Firmada digitalmente"),
    PENDIENTE_CARGA_NUBE("Pendiente carga en nube"),
    EN_NUBE("Almacenada en nube"),
    CERRADA("Cerrada / expediente completo");

    private final String etiqueta;

    EstadoActa(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
