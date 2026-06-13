package cr.go.heredia.actas.repository;

import cr.go.heredia.actas.model.Acta;
import cr.go.heredia.actas.model.EstadoActa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ActaRepository extends JpaRepository<Acta, Long>, JpaSpecificationExecutor<Acta> {

    Optional<Acta> findByConsecutivo(String consecutivo);

    long countByEstado(EstadoActa estado);

    @Query("SELECT a.estado, COUNT(a) FROM Acta a GROUP BY a.estado")
    List<Object[]> contarPorEstado();

    List<Acta> findTop10ByOrderByFechaRegistroSistemaDesc();

    @Query("SELECT a.empresa, COUNT(a) FROM Acta a GROUP BY a.empresa ORDER BY COUNT(a) DESC")
    List<Object[]> contarPorEmpresa();
}
