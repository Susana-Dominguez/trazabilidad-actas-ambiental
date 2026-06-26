package cr.go.heredia.actas.service;

import cr.go.heredia.actas.config.NotificacionProperties;
import cr.go.heredia.actas.model.Acta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificacionEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final NotificacionProperties properties;
    private final String publicBaseUrl;

    public NotificacionEmailService(
            ObjectProvider<JavaMailSender> mailSender,
            NotificacionProperties properties,
            @Value("${app.public-base-url}") String publicBaseUrl
    ) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.publicBaseUrl = publicBaseUrl;
    }

    public boolean enviarConfirmacionActa(Acta acta, String destinatario) {
        if (!properties.isEnabled() || destinatario == null || destinatario.isBlank()) {
            log.info("Notificación omitida (enabled={}, destinatario={})", properties.isEnabled(), destinatario);
            return false;
        }
        if (mailSender.getIfAvailable() == null) {
            log.warn("JavaMailSender no configurado; configure spring.mail.host y credenciales");
            return false;
        }

        String enlace = publicBaseUrl + "/actas/" + acta.getId();
        String asunto = "[Actas Ambientales] Acta registrada — " + acta.getConsecutivo();
        String cuerpo = """
                Estimado(a) colaborador(a),

                Su acta fue registrada en el sistema de trazabilidad de Gestión Ambiental.

                Consecutivo: %s
                Tipo: %s
                Inspector / gestor: %s
                Estado: %s
                Fecha del acta: %s

                Ver detalle en el sistema: %s

                Este mensaje confirma el registro. El documento PDF queda disponible para revisión del gestor.

                Municipalidad de Heredia — Gestión Ambiental
                """.formatted(
                acta.getConsecutivo(),
                acta.getTipoActa() != null ? acta.getTipoActa() : "—",
                acta.getInspector(),
                acta.getEstado().getEtiqueta(),
                acta.getFechaActa(),
                enlace
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(destinatario);
        if (properties.getCopiaGestor() != null && !properties.getCopiaGestor().isBlank()) {
            message.setCc(properties.getCopiaGestor());
        }
        message.setSubject(asunto);
        message.setText(cuerpo);
        mailSender.getObject().send(message);
        log.info("Correo de confirmación enviado a {} para acta {}", destinatario, acta.getConsecutivo());
        return true;
    }
}
