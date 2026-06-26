package cr.go.heredia.actas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cr.go.heredia.actas.config.ArcGisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

@Service
public class ArcGisAttachmentService {

    private static final Logger log = LoggerFactory.getLogger(ArcGisAttachmentService.class);

    private final ArcGisProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private volatile String cachedToken;
    private volatile long tokenExpiresAt;

    public ArcGisAttachmentService(ArcGisProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public Optional<Path> descargarPrimerAdjunto(String serviceUrl, int objectId, Path destinoDir) {
        if (serviceUrl == null || serviceUrl.isBlank() || objectId <= 0) {
            return Optional.empty();
        }
        try {
            Files.createDirectories(destinoDir);
            String token = resolverToken();
            String listUrl = serviceUrl + "/0/" + objectId + "/attachments?f=json"
                    + (token != null ? "&token=" + encode(token) : "");
            JsonNode list = fetchJson(listUrl);
            JsonNode attachments = list.path("attachmentInfos");
            if (!attachments.isArray() || attachments.isEmpty()) {
                log.warn("Sin adjuntos en objectId={} service={}", objectId, serviceUrl);
                return Optional.empty();
            }

            JsonNode first = attachments.get(0);
            int attId = first.path("id").asInt();
            String attName = first.path("name").asText("documento.pdf");
            String dataUrl = serviceUrl + "/0/" + objectId + "/attachments/" + attId + "/data?f=json"
                    + (token != null ? "&token=" + encode(token) : "");

            Path destino = destinoDir.resolve(attName);
            descargarBinario(dataUrl, destino);
            return Optional.of(destino);
        } catch (Exception e) {
            log.warn("No se pudo descargar adjunto ArcGIS objectId={}: {}", objectId, e.getMessage());
            return Optional.empty();
        }
    }

    private void descargarBinario(String url, Path destino) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " al descargar adjunto");
        }
        try (InputStream in = response.body()) {
            Files.copy(in, destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private JsonNode fetchJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " en " + url);
        }
        return objectMapper.readTree(response.body());
    }

    private String resolverToken() throws IOException, InterruptedException {
        if (properties.hasStaticToken()) {
            return properties.getToken().trim();
        }
        if (!properties.hasCredentials()) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiresAt) {
            return cachedToken;
        }
        String body = "username=" + encode(properties.getUsername())
                + "&password=" + encode(properties.getPassword())
                + "&referer=" + encode(properties.getReferer())
                + "&expiration=60&f=json";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getPortalUrl()))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());
        if (json.has("error")) {
            throw new IOException("ArcGIS token error: " + json.path("error").path("message").asText());
        }
        cachedToken = json.path("token").asText(null);
        long expires = json.path("expires").asLong(now + 3_600_000L);
        tokenExpiresAt = expires - 60_000L;
        return cachedToken;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
