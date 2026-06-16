package cr.go.heredia.actas.web;

import cr.go.heredia.actas.dto.ActaFiltroDto;
import cr.go.heredia.actas.dto.ActaFormDto;
import cr.go.heredia.actas.dto.DocumentoActaView;
import cr.go.heredia.actas.model.Acta;
import cr.go.heredia.actas.model.EstadoActa;
import cr.go.heredia.actas.model.OrigenActa;
import cr.go.heredia.actas.model.TipoActa;
import cr.go.heredia.actas.service.ActaService;
import cr.go.heredia.actas.service.DocumentoActaService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ActaController {

    private final ActaService actaService;
    private final DocumentoActaService documentoActaService;

    public ActaController(ActaService actaService, DocumentoActaService documentoActaService) {
        this.actaService = actaService;
        this.documentoActaService = documentoActaService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("recibidasMes", actaService.recibidasEsteMes());
        model.addAttribute("nombreMes", actaService.nombreMesActual());
        model.addAttribute("pendientesFirma", actaService.contarEstado(EstadoActa.POR_FIRMAR));
        model.addAttribute("pendientesNube", actaService.contarEstado(EstadoActa.PENDIENTE_CARGA_NUBE));
        model.addAttribute("expedientesCerrados", actaService.contarEstado(EstadoActa.CERRADA));
        model.addAttribute("actasRecientes", actaService.actasRecientes());
        model.addAttribute("topEmpresas", actaService.topEmpresas(5));
        model.addAttribute("resumenEstados", actaService.resumenDashboard());
        model.addAttribute("alertas", actaService.alertasPendientes());
        model.addAttribute("chartEstadosLabels", labels(actaService.datosGraficoEstados()));
        model.addAttribute("chartEstadosData", values(actaService.datosGraficoEstados()));
        model.addAttribute("chartTiposLabels", labels(actaService.datosGraficoTipos()));
        model.addAttribute("chartTiposData", values(actaService.datosGraficoTipos()));
        model.addAttribute("chartGestoresLabels", labels(actaService.datosGraficoGestores()));
        model.addAttribute("chartGestoresData", values(actaService.datosGraficoGestores()));
        return "dashboard";
    }

    @GetMapping("/ping")
    @ResponseBody
    public String ping() {
        return "OK";
    }

    @GetMapping("/actas/nueva")
    public String formularioNuevo(Model model) {
        model.addAttribute("acta", formularioVacio());
        agregarCatalogosRegistro(model);
        return "registro";
    }

    @PostMapping("/actas")
    public String guardar(
            @Valid @ModelAttribute("acta") ActaFormDto acta,
            BindingResult binding,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            Model model,
            RedirectAttributes flash
    ) {
        if (binding.hasErrors()) {
            agregarCatalogosRegistro(model);
            return "registro";
        }
        try {
            var guardada = actaService.registrar(acta, archivo);
            flash.addFlashAttribute("mensaje", "Acta " + guardada.getConsecutivo() + " registrada correctamente.");
            return "redirect:/actas/" + guardada.getId();
        } catch (IllegalArgumentException e) {
            binding.rejectValue("consecutivo", "duplicado", e.getMessage());
            agregarCatalogosRegistro(model);
            return "registro";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/actas/nueva";
        }
    }

    @GetMapping("/actas/consulta")
    public String consulta(@ModelAttribute ActaFiltroDto filtro, Model model) {
        List<Acta> resultados = actaService.buscar(filtro);
        model.addAttribute("filtro", filtro);
        model.addAttribute("resultados", resultados);
        model.addAttribute("estados", EstadoActa.values());
        model.addAttribute("documentosPorActa", mapaDocumentos(resultados));
        return "consulta";
    }

    @GetMapping("/actas/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        return actaService.buscarPorId(id)
                .map(acta -> {
                    model.addAttribute("acta", acta);
                    model.addAttribute("historial", actaService.historial(id));
                    model.addAttribute("estados", EstadoActa.values());
                    documentoActaService.vistaDocumento(acta).ifPresent(d -> model.addAttribute("documento", d));
                    return "detalle";
                })
                .orElse("redirect:/actas/consulta");
    }

    @GetMapping("/actas/{id}/documento/ver")
    public ResponseEntity<Resource> verDocumento(@PathVariable Long id) {
        Acta acta = actaService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada"));
        DocumentoActaView vista = documentoActaService.vistaDocumento(acta)
                .orElseThrow(() -> new IllegalArgumentException("Sin documento adjunto"));
        if (!vista.isPuedeVer()) {
            throw new IllegalArgumentException("Este tipo de archivo solo puede descargarse");
        }
        return respuestaDocumento(acta, vista.getNombreArchivo(), false);
    }

    @GetMapping("/actas/{id}/documento/descargar")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id) {
        Acta acta = actaService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada"));
        DocumentoActaView vista = documentoActaService.vistaDocumento(acta)
                .orElseThrow(() -> new IllegalArgumentException("Sin documento adjunto"));
        return respuestaDocumento(acta, vista.getNombreArchivo(), true);
    }

    @PostMapping("/actas/{id}/estado")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoActa estado,
            @RequestParam(required = false) String observacion,
            @RequestParam(defaultValue = "gestor.demo") String usuario,
            RedirectAttributes flash
    ) {
        actaService.actualizarEstado(id, estado, observacion, usuario);
        flash.addFlashAttribute("mensaje", "Estado actualizado.");
        return "redirect:/actas/" + id;
    }

    private ResponseEntity<Resource> respuestaDocumento(Acta acta, String nombreArchivo, boolean descarga) {
        try {
            Resource resource = documentoActaService.cargarArchivo(acta);
            MediaType mediaType = documentoActaService.mediaType(nombreArchivo);
            HttpHeaders headers = new HttpHeaders();
            if (descarga) {
                headers.setContentDisposition(ContentDisposition.attachment()
                        .filename(nombreArchivo, StandardCharsets.UTF_8)
                        .build());
            } else {
                headers.setContentDisposition(ContentDisposition.inline()
                        .filename(nombreArchivo, StandardCharsets.UTF_8)
                        .build());
            }
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(mediaType)
                    .body(resource);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo abrir el documento: " + e.getMessage());
        }
    }

    private Map<Long, DocumentoActaView> mapaDocumentos(List<Acta> actas) {
        Map<Long, DocumentoActaView> mapa = new HashMap<>();
        for (Acta acta : actas) {
            documentoActaService.vistaDocumento(acta).ifPresent(d -> mapa.put(acta.getId(), d));
        }
        return mapa;
    }

    private ActaFormDto formularioVacio() {
        ActaFormDto dto = new ActaFormDto();
        dto.setEstado(EstadoActa.RECIBIDA_CORREO);
        dto.setOrigen(OrigenActa.SURVEY123);
        dto.setTipoActa(TipoActa.INSPECCION);
        return dto;
    }

    private void agregarCatalogosRegistro(Model model) {
        model.addAttribute("estados", EstadoActa.values());
        model.addAttribute("origenes", OrigenActa.values());
        model.addAttribute("tiposActa", TipoActa.values());
    }

    private List<String> labels(List<Map.Entry<String, Long>> datos) {
        return datos.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private List<Long> values(List<Map.Entry<String, Long>> datos) {
        return datos.stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }
}
