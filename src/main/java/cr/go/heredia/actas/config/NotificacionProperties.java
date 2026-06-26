package cr.go.heredia.actas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notificaciones")
public class NotificacionProperties {

    private boolean enabled;
    private String from = "actas-ambiental@municipalidad.go.cr";
    private String copiaGestor = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getCopiaGestor() { return copiaGestor; }
    public void setCopiaGestor(String copiaGestor) { this.copiaGestor = copiaGestor; }
}
