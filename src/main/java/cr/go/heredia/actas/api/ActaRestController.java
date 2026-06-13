package cr.go.heredia.actas.api;

import cr.go.heredia.actas.dto.ActaApiRequest;
import cr.go.heredia.actas.dto.ActaApiResponse;
import cr.go.heredia.actas.model.Acta;
import cr.go.heredia.actas.service.ActaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/actas")
public class ActaRestController {

    private final ActaService actaService;
    private final String publicBaseUrl;

    public ActaRestController(
            ActaService actaService,
            @Value("${app.public-base-url}") String publicBaseUrl
    ) {
        this.actaService = actaService;
        this.publicBaseUrl = publicBaseUrl;
    }

    @PostMapping
    public ResponseEntity<ActaApiResponse> crear(@Valid @RequestBody ActaApiRequest request) throws IOException {
        Acta acta = actaService.registrar(request.toFormDto(), null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ActaApiResponse.from(acta, publicBaseUrl));
    }
}
