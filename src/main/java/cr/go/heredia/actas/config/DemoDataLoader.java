package cr.go.heredia.actas.config;

import cr.go.heredia.actas.model.Acta;
import cr.go.heredia.actas.model.EstadoActa;
import cr.go.heredia.actas.model.OrigenActa;
import cr.go.heredia.actas.repository.ActaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class DemoDataLoader {

    @Bean
    @Profile("demo")
    CommandLineRunner cargarDatosDemo(ActaRepository repo) {
        return args -> {
            if (repo.count() > 0) return;

            repo.save(acta("2026-001", "EcoHeredia S.A.", "CT-2024-018",
                    "Josue M.", LocalDate.now().minusDays(10), EstadoActa.EN_NUBE,
                    OrigenActa.SURVEY123, "Notificación empresas"));
            repo.save(acta("2026-002", "Reciclaje Central CR", "CT-2023-044",
                    "Ana G.", LocalDate.now().minusDays(4), EstadoActa.POR_FIRMAR,
                    OrigenActa.SURVEY123, "Inspección ocular"));
            repo.save(acta("2026-003", "Limpieza Urbana MH", "CT-2025-002",
                    "Teresita G.", LocalDate.now().minusDays(2), EstadoActa.FIRMADA,
                    OrigenActa.PORTAL_MUNICIPAL, "Denuncia"));
            repo.save(acta("2026-004", "EcoHeredia S.A.", "CT-2024-018",
                    "Josue M.", LocalDate.now().minusDays(1), EstadoActa.RECIBIDA_CORREO,
                    OrigenActa.SURVEY123, "Control calidad"));
        };
    }

    private Acta acta(String consecutivo, String empresa, String contrato, String inspector,
                      LocalDate fecha, EstadoActa estado, OrigenActa origen, String tipo) {
        Acta a = new Acta();
        a.setConsecutivo(consecutivo);
        a.setEmpresa(empresa);
        a.setNumeroContrato(contrato);
        a.setInspector(inspector);
        a.setFechaActa(fecha);
        a.setEstado(estado);
        a.setOrigen(origen);
        a.setTipoActa(tipo);
        a.setReferenciaCaso("Caso demo - " + tipo);
        a.setCorreoOrigen("sostenible@municipalidad.demo");
        a.setFechaRegistroSistema(LocalDateTime.now().minusDays(1));
        a.setUltimaActualizacion(LocalDateTime.now().minusDays(1));
        return a;
    }
}
