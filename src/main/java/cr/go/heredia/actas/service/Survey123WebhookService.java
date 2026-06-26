package cr.go.heredia.actas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cr.go.heredia.actas.config.Survey123Properties;
import cr.go.heredia.actas.dto.ActaFormDto;
import cr.go.heredia.actas.dto.Survey123WebhookResponse;
import cr.go.heredia.actas.model.Acta;
import cr.go.heredia.actas.model.EstadoActa;
import cr.go.heredia.actas.model.OrigenActa;
import cr.go.heredia.actas.model.TipoActa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class Survey123WebhookService {

    private static final Logger log = LoggerFactory.getLogger(Survey123WebhookService.class);

    private final Survey123Properties properties;
    private final ActaService actaService;
    private final ConsecutivoService consecutivoService;
    private final ArcGisAttachmentService arcGisAttachmentService;
    private final NotificacionEmailService notificacionEmailService;
    private final ObjectMapper objectMapper;
    private final Path uploadDir;
    private final String publicBaseUrl;

    public Survey123WebhookService(
            Survey123Properties properties,
            ActaService actaService,
            ConsecutivoService consecutivoService,
            ArcGisAttachmentService arcGisAttachmentService,
            NotificacionEmailService notificacionEmailService,
            ObjectMapper objectMapper,
            @Value("${app.upload-dir:./uploads}") String uploadDir,
            @Value("${app.public-base-url}") String publicBaseUrl
    ) throws IOException {
        this.properties = properties;
        this.actaService = actaService;
        this.consecutivoService = consecutivoService;
        this.arcGisAttachmentService = arcGisAttachmentService;
        this.notificacionEmailService = notificacionEmailService;
        this.objectMapper = objectMapper;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath();
        this.publicBaseUrl = publicBaseUrl;
        Files.createDirectories(this.uploadDir);
    }

    public Survey123WebhookResponse procesarInspeccion(JsonNode payload) {
        return procesarInspeccion(payload, true);
    }

    public Survey123WebhookResponse procesarInspeccion(JsonNode payload, boolean forzarInspeccion) {
        try {
            if (!forzarInspeccion && !esFormularioInspeccion(payload)) {
                return Survey123WebhookResponse.error(
                        "Webhook ignorado: no corresponde al formulario de Inspección configurado");
            }

            JsonNode attributes = extraerAttributes(payload);
            JsonNode userInfo = payload.path("userInfo");
            Survey123Properties.InspeccionForm cfg = properties.getInspeccion();

            String consecutivo = leerTexto(attributes, List.of("consecutivo", "numero_acta", "no_acta"));
            if (consecutivo == null || consecutivo.isBlank()) {
                consecutivo = consecutivoService.generar(cfg.getConsecutivoPrefix());
            }

            String inspector = leerTexto(attributes, cfg.inspectorFields());
            if (inspector == null || inspector.isBlank()) {
                inspector = userInfo.path("fullName").asText(null);
            }
            if (inspector == null || inspector.isBlank()) {
                inspector = userInfo.path("username").asText("inspector.survey123");
            }

            LocalDate fechaActa = leerFecha(attributes, cfg.fechaFields());
            String empresa = leerTexto(attributes, cfg.empresaFields());
            String contrato = leerTexto(attributes, cfg.contratoFields());
            String referencia = leerTexto(attributes, cfg.referenciaFields());
            String correo = userInfo.path("email").asText(null);

            int objectId = leerObjectId(attributes);
            String serviceUrl = payload.path("surveyInfo").path("serviceUrl").asText(null);
            String formTitle = payload.path("surveyInfo").path("formTitle").asText("Inspección");

            ActaFormDto dto = new ActaFormDto();
            dto.setConsecutivo(consecutivo);
            dto.setEmpresa(empresa);
            dto.setNumeroContrato(contrato);
            dto.setInspector(inspector);
            dto.setFechaActa(fechaActa);
            dto.setEstado(EstadoActa.RECIBIDA_CORREO);
            dto.setOrigen(OrigenActa.SURVEY123);
            dto.setTipoActa(TipoActa.INSPECCION);
            dto.setReferenciaCaso(construirReferencia(referencia, formTitle, objectId));
            dto.setCorreoOrigen(correo);
            dto.setObservacionHistorial("Registro automático vía webhook Survey123 — " + formTitle);
            dto.setUsuario("survey123.webhook");

            Path tempDir = uploadDir.resolve("tmp-" + consecutivo.replaceAll("[^a-zA-Z0-9_-]", "_"));
            Optional<Path> adjunto = arcGisAttachmentService.descargarPrimerAdjunto(serviceUrl, objectId, tempDir);

            Acta acta;
            boolean documentoAlmacenado;
            if (adjunto.isPresent()) {
                acta = actaService.registrarConArchivo(dto, adjunto.get(), adjunto.get().getFileName().toString());
                documentoAlmacenado = true;
                limpiarDirectorio(tempDir);
            } else {
                acta = actaService.registrarConArchivo(dto, null, null);
                documentoAlmacenado = false;
                log.warn("Acta {} registrada sin PDF (objectId={}, serviceUrl={})", consecutivo, objectId, serviceUrl);
            }

            boolean emailEnviado = notificacionEmailService.enviarConfirmacionActa(acta, correo);

            return Survey123WebhookResponse.ok(
                    acta.getConsecutivo(),
                    acta.getId(),
                    publicBaseUrl + "/actas/" + acta.getId(),
                    emailEnviado,
                    documentoAlmacenado
            );
        } catch (IllegalArgumentException e) {
            return Survey123WebhookResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error procesando webhook Survey123", e);
            return Survey123WebhookResponse.error("Error interno: " + e.getMessage());
        }
    }

    public JsonNode parsePayload(String rawBody) throws IOException {
        if (rawBody == null || rawBody.isBlank()) {
            throw new IOException("Cuerpo vacío");
        }
        String trimmed = rawBody.trim();
        if (trimmed.startsWith("{")) {
            return objectMapper.readTree(trimmed);
        }
        if (trimmed.contains("payload=")) {
            String encoded = trimmed.split("payload=", 2)[1].split("&", 2)[0];
            String json = java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
            return objectMapper.readTree(json);
        }
        return objectMapper.readTree(trimmed);
    }

    private boolean esFormularioInspeccion(JsonNode payload) {
        String formTitle = payload.path("surveyInfo").path("formTitle").asText("");
        String formName = payload.path("surveyInfo").path("name").asText("");
        String combined = (formTitle + " " + formName).toLowerCase(Locale.ROOT);
        for (String keyword : properties.getInspeccion().titleKeywords()) {
            if (combined.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        // Demo: si no hay título, aceptar endpoint dedicado /inspeccion
        return formTitle.isBlank() && formName.isBlank();
    }

    private JsonNode extraerAttributes(JsonNode payload) {
        JsonNode feature = payload.path("feature");
        if (feature.has("attributes")) {
            return feature.path("attributes");
        }
        if (payload.has("attributes")) {
            return payload.path("attributes");
        }
        if (payload.has("adds") && payload.path("adds").isArray() && !payload.path("adds").isEmpty()) {
            return payload.path("adds").get(0).path("attributes");
        }
        return objectMapper.createObjectNode();
    }

    private String leerTexto(JsonNode attributes, List<String> campos) {
        for (String campo : campos) {
            if (attributes.has(campo) && !attributes.get(campo).isNull()) {
                String val = attributes.get(campo).asText("").trim();
                if (!val.isBlank()) {
                    return val;
                }
            }
        }
        Iterator<Map.Entry<String, JsonNode>> it = attributes.properties().iterator();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            for (String campo : campos) {
                if (key.equals(campo.toLowerCase(Locale.ROOT)) && !entry.getValue().isNull()) {
                    String val = entry.getValue().asText("").trim();
                    if (!val.isBlank()) {
                        return val;
                    }
                }
            }
        }
        return null;
    }

    private LocalDate leerFecha(JsonNode attributes, List<String> campos) {
        for (String campo : campos) {
            if (!attributes.has(campo) || attributes.get(campo).isNull()) {
                continue;
            }
            JsonNode node = attributes.get(campo);
            if (node.isNumber()) {
                long millis = node.asLong();
                if (millis < 1_000_000_000_000L) {
                    millis *= 1000L;
                }
                return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            String text = node.asText("").trim();
            if (!text.isBlank()) {
                try {
                    return LocalDate.parse(text.substring(0, Math.min(10, text.length())));
                } catch (Exception ignored) {
                    // continuar
                }
            }
        }
        return LocalDate.now();
    }

    private int leerObjectId(JsonNode attributes) {
        if (attributes.has("objectid")) {
            return attributes.path("objectid").asInt();
        }
        if (attributes.has("OBJECTID")) {
            return attributes.path("OBJECTID").asInt();
        }
        return attributes.path("objectId").asInt(0);
    }

    private String construirReferencia(String referencia, String formTitle, int objectId) {
        String base = referencia != null && !referencia.isBlank()
                ? referencia
                : "Formulario: " + formTitle;
        if (objectId > 0) {
            return base + " (ArcGIS objectId=" + objectId + ")";
        }
        return base;
    }

    private void limpiarDirectorio(Path dir) {
        try {
            if (Files.isDirectory(dir)) {
                Files.list(dir).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
                Files.deleteIfExists(dir);
            }
        } catch (IOException ignored) {
        }
    }
}
