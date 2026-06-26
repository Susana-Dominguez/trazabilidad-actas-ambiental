package cr.go.heredia.actas.service;

import cr.go.heredia.actas.model.Acta;
import cr.go.heredia.actas.repository.ActaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConsecutivoService {

    private static final Pattern SUFFIX = Pattern.compile("(\\d+)$");

    private final ActaRepository actaRepository;

    public ConsecutivoService(ActaRepository actaRepository) {
        this.actaRepository = actaRepository;
    }

    @Transactional(readOnly = true)
    public String generar(String prefix) {
        int year = LocalDate.now().getYear();
        String base = prefix + "-" + year;
        Optional<Acta> ultimoActa = actaRepository.findTopByConsecutivoStartingWithOrderByConsecutivoDesc(base);

        int siguiente = 1;
        if (ultimoActa.isPresent()) {
            Matcher matcher = SUFFIX.matcher(ultimoActa.get().getConsecutivo());
            if (matcher.find()) {
                siguiente = Integer.parseInt(matcher.group(1)) + 1;
            }
        }
        return base + String.format("%03d", siguiente);
    }
}
