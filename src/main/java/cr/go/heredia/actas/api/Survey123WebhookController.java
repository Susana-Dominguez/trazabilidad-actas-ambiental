package cr.go.heredia.actas.api;

import com.fasterxml.jackson.databind.JsonNode;
import cr.go.heredia.actas.config.Survey123Properties;
import cr.go.heredia.actas.dto.Survey123WebhookResponse;
import cr.go.heredia.actas.service.Survey123WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Webhook Survey123 — demo formulario Inspección.
 * URL para configurar en Survey123: POST /api/webhooks/survey123/inspeccion
 */
@RestController
@RequestMapping("/api/webhooks/survey123")
public class Survey123WebhookController {

    private static final Logger log = LoggerFactory.getLogger(Survey123WebhookController.class);

    private final Survey123WebhookService webhookService;
    private final Survey123Properties properties;

    public Survey123WebhookController(
            Survey123WebhookService webhookService,
            Survey123Properties properties
    ) {
        this.webhookService = webhookService;
        this.properties = properties;
    }

    @GetMapping("/inspeccion")
    public ResponseEntity<java.util.Map<String, String>> pingInspeccion() {
        return ResponseEntity.ok(java.util.Map.of(
                "status", "OK",
                "endpoint", "/api/webhooks/survey123/inspeccion",
                "descripcion", "Webhook demo — formulario Inspección Survey123"
        ));
    }

    @PostMapping("/inspeccion")
    public ResponseEntity<Survey123WebhookResponse> recibirInspeccion(
            HttpServletRequest request,
            @RequestBody(required = false) String body
    ) {
        try {
            String raw = body;
            if (raw == null || raw.isBlank()) {
                raw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            }
            log.info("Webhook Inspección recibido ({} bytes)", raw.length());

            JsonNode payload = webhookService.parsePayload(raw);
            Survey123WebhookResponse response = webhookService.procesarInspeccion(payload);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error en webhook Inspección", e);
            return ResponseEntity.internalServerError()
                    .body(Survey123WebhookResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/inspeccion/test")
    public ResponseEntity<Survey123WebhookResponse> simularInspeccion(@RequestBody JsonNode payload) {
        log.info("Simulación webhook Inspección");
        Survey123WebhookResponse response = webhookService.procesarInspeccion(payload);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
