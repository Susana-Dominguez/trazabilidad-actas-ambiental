package cr.go.heredia.actas.config;

import cr.go.heredia.actas.model.Acta;
import cr.go.heredia.actas.model.EstadoActa;
import cr.go.heredia.actas.model.OrigenActa;
import cr.go.heredia.actas.model.TipoActa;
import cr.go.heredia.actas.repository.ActaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Configuration
public class DemoDataLoader {

    @Bean
    @Profile("demo")
    CommandLineRunner cargarDatosDemo(ActaRepository repo) {
        return args -> {
            if (repo.count() > 0) return;

            YearMonth mes = YearMonth.now();
            LocalDateTime inicioMes = mes.atDay(1).atStartOfDay();

            repo.save(acta("2026-001", "EcoHeredia S.A.", "CT-2024-018",
                    "Josué M.", LocalDate.now().minusDays(10), EstadoActa.EN_NUBE,
                    OrigenActa.SURVEY123, TipoActa.NOTIFICACION, inicioMes.plusDays(1)));
            repo.save(acta("2026-002", "Reciclaje Central CR", "CT-2023-044",
                    "Aaron Mora", LocalDate.now().minusDays(4), EstadoActa.POR_FIRMAR,
                    OrigenActa.SURVEY123, TipoActa.INSPECCION, inicioMes.plusDays(3)));
            repo.save(acta("2026-003", "Limpieza Urbana MH", "CT-2025-002",
                    "Susana Domínguez", LocalDate.now().minusDays(2), EstadoActa.PENDIENTE_CARGA_NUBE,
                    OrigenActa.PORTAL_MUNICIPAL, TipoActa.FISCALIZACION, inicioMes.plusDays(5)));
            repo.save(acta("2026-004", "EcoHeredia S.A.", "CT-2024-018",
                    "Josué M.", LocalDate.now().minusDays(1), EstadoActa.RECIBIDA_CORREO,
                    OrigenActa.SURVEY123, TipoActa.OCULAR, inicioMes.plusDays(7)));
            repo.save(acta("2026-005", "Servicios Ambientales CR", "CT-2024-022",
                    "Aaron Mora", LocalDate.now(), EstadoActa.CERRADA,
                    OrigenActa.SURVEY123, TipoActa.NOTIFICACION, inicioMes.plusDays(10)));
            repo.save(acta("2026-006", "Lumar Investment", "CT-2026-018",
                    "Susana Domínguez", LocalDate.now(), EstadoActa.POR_FIRMAR,
                    OrigenActa.MANUAL, TipoActa.INSPECCION, inicioMes.plusDays(12)));
        };
    }

    private Acta acta(String consecutivo, String empresa, String contrato, String inspector,
                      LocalDate fecha, EstadoActa estado, OrigenActa origen, TipoActa tipo,
                      LocalDateTime registro) {
        Acta a = new Acta();
        a.setConsecutivo(consecutivo);
        a.setEmpresa(empresa);
        a.setNumeroContrato(contrato);
        a.setInspector(inspector);
        a.setFechaActa(fecha);
        a.setEstado(estado);
        a.setOrigen(origen);
        a.setTipoActa(tipo.getEtiqueta());
        a.setReferenciaCaso("Caso demo - " + tipo.getEtiqueta());
        a.setCorreoOrigen("sostenible@municipalidad.demo");
        a.setFechaRegistroSistema(registro);
        a.setUltimaActualizacion(registro);
        return a;
    }
}
