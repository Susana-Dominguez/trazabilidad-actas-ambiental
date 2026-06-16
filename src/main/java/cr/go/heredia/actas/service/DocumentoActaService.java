package cr.go.heredia.actas.service;

import cr.go.heredia.actas.dto.DocumentoActaView;
import cr.go.heredia.actas.model.Acta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

@Service
public class DocumentoActaService {

    private final Path uploadDir;

    public DocumentoActaService(@Value("${app.upload-dir:./uploads}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public Optional<DocumentoActaView> vistaDocumento(Acta acta) {
        if (acta.getRutaDocumento() == null || acta.getRutaDocumento().isBlank()) {
            return Optional.empty();
        }
        Path archivo = resolveSeguro(acta.getRutaDocumento()).orElse(null);
        if (archivo == null || !Files.isRegularFile(archivo)) {
            return Optional.empty();
        }
        String nombre = archivo.getFileName().toString();
        boolean pdf = esPdf(nombre);
        boolean word = esWord(nombre);
        return Optional.of(new DocumentoActaView(
                nombre,
                acta.getEstado().getEtiqueta(),
                acta.getFechaRegistroSistema(),
                pdf,
                word,
                acta.getId()
        ));
    }

    public boolean tieneDocumento(Acta acta) {
        return vistaDocumento(acta).isPresent();
    }

    public Resource cargarArchivo(Acta acta) throws MalformedURLException {
        Path archivo = resolveSeguro(acta.getRutaDocumento())
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
        if (!Files.isRegularFile(archivo)) {
            throw new IllegalArgumentException("Documento no encontrado");
        }
        Resource resource = new UrlResource(archivo.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("No se puede leer el documento");
        }
        return resource;
    }

    public MediaType mediaType(String nombreArchivo) {
        String lower = nombreArchivo.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (lower.endsWith(".doc")) {
            return MediaType.parseMediaType("application/msword");
        }
        if (lower.endsWith(".docx")) {
            return MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    public boolean esPdf(String nombre) {
        return nombre.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    public boolean esWord(String nombre) {
        String lower = nombre.toLowerCase(Locale.ROOT);
        return lower.endsWith(".doc") || lower.endsWith(".docx");
    }

    private Optional<Path> resolveSeguro(String rutaAlmacenada) {
        Path archivo = Paths.get(rutaAlmacenada).normalize().toAbsolutePath();
        if (!archivo.startsWith(uploadDir)) {
            return Optional.empty();
        }
        return Optional.of(archivo);
    }
}
