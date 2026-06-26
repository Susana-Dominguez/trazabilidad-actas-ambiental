package cr.go.heredia.actas.service;

import cr.go.heredia.actas.dto.ActaFiltroDto;
import cr.go.heredia.actas.dto.ActaFormDto;
import cr.go.heredia.actas.model.Acta;
import cr.go.heredia.actas.model.EstadoActa;
import cr.go.heredia.actas.model.HistorialEstado;
import cr.go.heredia.actas.model.TipoActa;
import cr.go.heredia.actas.repository.ActaRepository;
import cr.go.heredia.actas.repository.HistorialEstadoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActaService {

    private final ActaRepository actaRepository;
    private final HistorialEstadoRepository historialRepository;
    private final Path uploadDir;
    private final int diasPendienteFirma;
    private final int diasPendienteNube;

    public ActaService(
            ActaRepository actaRepository,
            HistorialEstadoRepository historialRepository,
            @Value("${app.upload-dir:./uploads}") String uploadDir,
            @Value("${app.alertas.dias-pendiente-firma:3}") int diasPendienteFirma,
            @Value("${app.alertas.dias-pendiente-nube:5}") int diasPendienteNube
    ) throws IOException {
        this.actaRepository = actaRepository;
        this.historialRepository = historialRepository;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath();
        this.diasPendienteFirma = diasPendienteFirma;
        this.diasPendienteNube = diasPendienteNube;
        Files.createDirectories(this.uploadDir);
    }

    @Transactional
    public Acta registrar(ActaFormDto dto, MultipartFile archivo) throws IOException {
        actaRepository.findByConsecutivo(dto.getConsecutivo()).ifPresent(a -> {
            throw new IllegalArgumentException("Ya existe una acta con consecutivo " + dto.getConsecutivo());
        });

        Acta acta = new Acta();
        mapear(acta, dto);
        acta.setFechaRegistroSistema(LocalDateTime.now());
        acta.setUltimaActualizacion(LocalDateTime.now());

        if (archivo != null && !archivo.isEmpty()) {
            String nombre = dto.getConsecutivo().replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + archivo.getOriginalFilename();
            Path destino = uploadDir.resolve(nombre);
            Files.copy(archivo.getInputStream(), destino);
            acta.setRutaDocumento(destino.toString());
        }

        Acta guardada = actaRepository.save(acta);
        registrarHistorial(guardada, guardada.getEstado(), dto.getObservacionHistorial(), dto.getUsuario());
        return guardada;
    }

    @Transactional
    public Acta registrarConArchivo(ActaFormDto dto, java.nio.file.Path archivoOrigen, String nombreArchivo) throws IOException {
        actaRepository.findByConsecutivo(dto.getConsecutivo()).ifPresent(a -> {
            throw new IllegalArgumentException("Ya existe una acta con consecutivo " + dto.getConsecutivo());
        });

        Acta acta = new Acta();
        mapear(acta, dto);
        acta.setFechaRegistroSistema(LocalDateTime.now());
        acta.setUltimaActualizacion(LocalDateTime.now());

        if (archivoOrigen != null && java.nio.file.Files.isRegularFile(archivoOrigen)) {
            String safeName = nombreArchivo != null && !nombreArchivo.isBlank()
                    ? nombreArchivo
                    : archivoOrigen.getFileName().toString();
            String nombre = dto.getConsecutivo().replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + safeName;
            Path destino = uploadDir.resolve(nombre);
            java.nio.file.Files.copy(archivoOrigen, destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            acta.setRutaDocumento(destino.toString());
        }

        Acta guardada = actaRepository.save(acta);
        registrarHistorial(guardada, guardada.getEstado(), dto.getObservacionHistorial(), dto.getUsuario());
        return guardada;
    }

    @Transactional
    public Acta actualizarEstado(Long id, EstadoActa nuevoEstado, String observacion, String usuario) {
        Acta acta = actaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Acta no encontrada"));
        if (acta.getEstado() != nuevoEstado) {
            acta.setEstado(nuevoEstado);
            acta.setUltimaActualizacion(LocalDateTime.now());
            actaRepository.save(acta);
            registrarHistorial(acta, nuevoEstado, observacion, usuario);
        }
        return acta;
    }

    public Optional<Acta> buscarPorId(Long id) {
        return actaRepository.findById(id);
    }

    public Optional<Acta> buscarPorConsecutivo(String consecutivo) {
        return actaRepository.findByConsecutivo(consecutivo);
    }

    public List<Acta> buscar(ActaFiltroDto filtro) {
        Specification<Acta> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (notBlank(filtro.getEmpresa())) {
                preds.add(cb.like(cb.lower(root.get("empresa")), "%" + filtro.getEmpresa().toLowerCase() + "%"));
            }
            if (notBlank(filtro.getNumeroContrato())) {
                preds.add(cb.like(cb.lower(root.get("numeroContrato")), "%" + filtro.getNumeroContrato().toLowerCase() + "%"));
            }
            if (notBlank(filtro.getConsecutivo())) {
                preds.add(cb.like(cb.lower(root.get("consecutivo")), "%" + filtro.getConsecutivo().toLowerCase() + "%"));
            }
            if (notBlank(filtro.getInspector())) {
                preds.add(cb.like(cb.lower(root.get("inspector")), "%" + filtro.getInspector().toLowerCase() + "%"));
            }
            if (filtro.getEstado() != null) {
                preds.add(cb.equal(root.get("estado"), filtro.getEstado()));
            }
            if (filtro.getFechaDesde() != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("fechaActa"), filtro.getFechaDesde()));
            }
            if (filtro.getFechaHasta() != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("fechaActa"), filtro.getFechaHasta()));
            }
            query.orderBy(cb.desc(root.get("fechaActa")));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return actaRepository.findAll(spec);
    }

    public List<HistorialEstado> historial(Long actaId) {
        return historialRepository.findByActaIdOrderByFechaCambioDesc(actaId);
    }

    public Map<String, Long> resumenPorEstado() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (EstadoActa e : EstadoActa.values()) {
            map.put(e.name(), actaRepository.countByEstado(e));
        }
        return map;
    }

    public List<Acta> alertasPendientes() {
        LocalDateTime limiteFirma = LocalDateTime.now().minusDays(diasPendienteFirma);
        LocalDateTime limiteNube = LocalDateTime.now().minusDays(diasPendienteNube);
        List<Acta> todas = actaRepository.findAll();
        List<Acta> alertas = new ArrayList<>();
        for (Acta a : todas) {
            if (a.getEstado() == EstadoActa.POR_FIRMAR && a.getUltimaActualizacion().isBefore(limiteFirma)) {
                alertas.add(a);
            } else if ((a.getEstado() == EstadoActa.FIRMADA || a.getEstado() == EstadoActa.PENDIENTE_CARGA_NUBE)
                    && a.getUltimaActualizacion().isBefore(limiteNube)) {
                alertas.add(a);
            }
        }
        return alertas;
    }

    public long totalActas() {
        return actaRepository.count();
    }

    public long contarEstado(EstadoActa estado) {
        return actaRepository.countByEstado(estado);
    }

    public List<Acta> actasRecientes() {
        return actaRepository.findTop10ByOrderByFechaRegistroSistemaDesc();
    }

    public List<Map.Entry<String, Long>> topEmpresas(int limite) {
        return actaRepository.contarPorEmpresa().stream()
                .limit(limite)
                .map(row -> Map.entry((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    public long actasSinActualizar(int dias) {
        LocalDateTime limite = LocalDateTime.now().minusDays(dias);
        return actaRepository.findAll().stream()
                .filter(a -> a.getEstado() != EstadoActa.CERRADA)
                .filter(a -> a.getUltimaActualizacion().isBefore(limite))
                .count();
    }

    public long cerradasRecientes(int dias) {
        LocalDateTime limite = LocalDateTime.now().minusDays(dias);
        return actaRepository.findAll().stream()
                .filter(a -> a.getEstado() == EstadoActa.CERRADA)
                .filter(a -> a.getUltimaActualizacion().isAfter(limite))
                .count();
    }

    public long recibidasEsteMes() {
        YearMonth mes = YearMonth.now();
        LocalDateTime inicio = mes.atDay(1).atStartOfDay();
        LocalDateTime fin = mes.plusMonths(1).atDay(1).atStartOfDay();
        return actaRepository.countRegistradasEntre(inicio, fin);
    }

    public List<Map.Entry<String, Long>> datosGraficoEstados() {
        List<Map.Entry<String, Long>> datos = new ArrayList<>();
        for (EstadoActa estado : EstadoActa.values()) {
            long count = actaRepository.countByEstado(estado);
            if (count > 0) {
                datos.add(Map.entry(estado.getEtiqueta(), count));
            }
        }
        return datos;
    }

    public List<Map.Entry<String, Long>> datosGraficoTipos() {
        return actaRepository.contarPorTipoActa().stream()
                .map(row -> Map.entry((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    public List<Map.Entry<String, Long>> datosGraficoGestores() {
        return actaRepository.contarPorInspector().stream()
                .map(row -> Map.entry((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    public String nombreMesActual() {
        YearMonth mes = YearMonth.now();
        String[] nombres = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
        return nombres[mes.getMonthValue() - 1] + " " + mes.getYear();
    }

    public List<Map<String, Object>> resumenDashboard() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (EstadoActa estado : EstadoActa.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("codigo", estado.name());
            item.put("etiqueta", estado.getEtiqueta());
            item.put("cantidad", actaRepository.countByEstado(estado));
            items.add(item);
        }
        return items;
    }

    private void registrarHistorial(Acta acta, EstadoActa estado, String obs, String usuario) {
        HistorialEstado h = new HistorialEstado();
        h.setActa(acta);
        h.setEstado(estado);
        h.setObservacion(obs != null && !obs.isBlank() ? obs : "Registro / cambio de estado");
        h.setUsuario(usuario != null && !usuario.isBlank() ? usuario : "sistema");
        historialRepository.save(h);
    }

    private void mapear(Acta acta, ActaFormDto dto) {
        acta.setConsecutivo(dto.getConsecutivo().trim());
        acta.setEmpresa(blankToNull(dto.getEmpresa()));
        acta.setNumeroContrato(blankToNull(dto.getNumeroContrato()));
        acta.setInspector(dto.getInspector().trim());
        acta.setFechaActa(dto.getFechaActa());
        acta.setEstado(dto.getEstado());
        acta.setOrigen(dto.getOrigen());
        acta.setTipoActa(dto.getTipoActa() != null ? dto.getTipoActa().getEtiqueta() : null);
        acta.setReferenciaCaso(dto.getReferenciaCaso());
        acta.setCorreoOrigen(dto.getCorreoOrigen());
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String blankToNull(String s) {
        return s != null && !s.isBlank() ? s.trim() : null;
    }
}
