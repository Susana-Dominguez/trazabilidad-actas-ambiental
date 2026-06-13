package cr.go.heredia.actas.web;

import cr.go.heredia.actas.dto.ActaFiltroDto;
import cr.go.heredia.actas.dto.ActaFormDto;
import cr.go.heredia.actas.model.EstadoActa;
import cr.go.heredia.actas.model.OrigenActa;
import cr.go.heredia.actas.service.ActaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ActaController {

    private final ActaService actaService;

    public ActaController(ActaService actaService) {
        this.actaService = actaService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalActas", actaService.totalActas());
        model.addAttribute("enRevision", actaService.contarEstado(EstadoActa.RECIBIDA_CORREO));
        model.addAttribute("porFirmar", actaService.contarEstado(EstadoActa.POR_FIRMAR));
        model.addAttribute("atrasadas", actaService.alertasPendientes().size());
        model.addAttribute("actasRecientes", actaService.actasRecientes());
        model.addAttribute("topEmpresas", actaService.topEmpresas(5));
        model.addAttribute("resumenEstados", actaService.resumenDashboard());
        model.addAttribute("pendientesFirma", actaService.contarEstado(EstadoActa.POR_FIRMAR));
        model.addAttribute("enBandeja", actaService.contarEstado(EstadoActa.RECIBIDA_CORREO));
        model.addAttribute("sinActualizar7d", actaService.actasSinActualizar(7));
        model.addAttribute("cerradasSemana", actaService.cerradasRecientes(7));
        model.addAttribute("alertas", actaService.alertasPendientes());
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
        model.addAttribute("estados", EstadoActa.values());
        model.addAttribute("origenes", OrigenActa.values());
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
            model.addAttribute("estados", EstadoActa.values());
            model.addAttribute("origenes", OrigenActa.values());
            return "registro";
        }
        try {
            var guardada = actaService.registrar(acta, archivo);
            flash.addFlashAttribute("mensaje", "Acta " + guardada.getConsecutivo() + " registrada correctamente.");
            return "redirect:/actas/" + guardada.getId();
        } catch (IllegalArgumentException e) {
            binding.rejectValue("consecutivo", "duplicado", e.getMessage());
            model.addAttribute("estados", EstadoActa.values());
            model.addAttribute("origenes", OrigenActa.values());
            return "registro";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/actas/nueva";
        }
    }

    @GetMapping("/actas/consulta")
    public String consulta(@ModelAttribute ActaFiltroDto filtro, Model model) {
        model.addAttribute("filtro", filtro);
        model.addAttribute("resultados", actaService.buscar(filtro));
        return "consulta";
    }

    @GetMapping("/actas/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        return actaService.buscarPorId(id)
                .map(acta -> {
                    model.addAttribute("acta", acta);
                    model.addAttribute("historial", actaService.historial(id));
                    model.addAttribute("estados", EstadoActa.values());
                    return "detalle";
                })
                .orElse("redirect:/actas/consulta");
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

    private ActaFormDto formularioVacio() {
        ActaFormDto dto = new ActaFormDto();
        dto.setEstado(EstadoActa.RECIBIDA_CORREO);
        dto.setOrigen(OrigenActa.SURVEY123);
        return dto;
    }
}
