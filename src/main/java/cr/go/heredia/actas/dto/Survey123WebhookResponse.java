package cr.go.heredia.actas.dto;

public class Survey123WebhookResponse {

    private boolean success;
    private String consecutivo;
    private Long actaId;
    private String enlaceDetalle;
    private boolean emailEnviado;
    private String mensaje;
    private boolean documentoAlmacenado;

    public static Survey123WebhookResponse ok(
            String consecutivo,
            Long actaId,
            String enlaceDetalle,
            boolean emailEnviado,
            boolean documentoAlmacenado
    ) {
        Survey123WebhookResponse r = new Survey123WebhookResponse();
        r.success = true;
        r.consecutivo = consecutivo;
        r.actaId = actaId;
        r.enlaceDetalle = enlaceDetalle;
        r.emailEnviado = emailEnviado;
        r.documentoAlmacenado = documentoAlmacenado;
        r.mensaje = "Acta registrada correctamente";
        return r;
    }

    public static Survey123WebhookResponse error(String mensaje) {
        Survey123WebhookResponse r = new Survey123WebhookResponse();
        r.success = false;
        r.mensaje = mensaje;
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getConsecutivo() { return consecutivo; }
    public void setConsecutivo(String consecutivo) { this.consecutivo = consecutivo; }
    public Long getActaId() { return actaId; }
    public void setActaId(Long actaId) { this.actaId = actaId; }
    public String getEnlaceDetalle() { return enlaceDetalle; }
    public void setEnlaceDetalle(String enlaceDetalle) { this.enlaceDetalle = enlaceDetalle; }
    public boolean isEmailEnviado() { return emailEnviado; }
    public void setEmailEnviado(boolean emailEnviado) { this.emailEnviado = emailEnviado; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public boolean isDocumentoAlmacenado() { return documentoAlmacenado; }
    public void setDocumentoAlmacenado(boolean documentoAlmacenado) { this.documentoAlmacenado = documentoAlmacenado; }
}
