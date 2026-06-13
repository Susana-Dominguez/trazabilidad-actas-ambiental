package cr.go.heredia.actas.repository;

import cr.go.heredia.actas.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {
    List<HistorialEstado> findByActaIdOrderByFechaCambioDesc(Long actaId);
}
