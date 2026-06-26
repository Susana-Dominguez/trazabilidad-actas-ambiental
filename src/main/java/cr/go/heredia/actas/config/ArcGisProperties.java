package cr.go.heredia.actas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.arcgis")
public class ArcGisProperties {

    private String token = "";
    private String username = "";
    private String password = "";
    private String portalUrl = "https://www.arcgis.com/sharing/rest/generateToken";
    private String referer = "https://www.arcgis.com";

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPortalUrl() { return portalUrl; }
    public void setPortalUrl(String portalUrl) { this.portalUrl = portalUrl; }
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }

    public boolean hasStaticToken() {
        return token != null && !token.isBlank();
    }

    public boolean hasCredentials() {
        return username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }
}
