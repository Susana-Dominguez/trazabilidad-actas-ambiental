package cr.go.heredia.actas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.survey123")
public class Survey123Properties {

    private String webhookSecret = "";
    private InspeccionForm inspeccion = new InspeccionForm();

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public InspeccionForm getInspeccion() {
        return inspeccion;
    }

    public void setInspeccion(InspeccionForm inspeccion) {
        this.inspeccion = inspeccion;
    }

    public static class InspeccionForm {
        private String formTitleKeywords = "Inspeccion,Inspección";
        private String tipoActa = "Inspección";
        private String consecutivoPrefix = "Inspeccion";
        private String fieldInspector = "inspector";
        private String fieldEmpresa = "empresa";
        private String fieldContrato = "numero_contrato";
        private String fieldReferencia = "referencia_caso";
        private String fieldFecha = "fecha_acta";

        public List<String> titleKeywords() {
            return Arrays.stream(formTitleKeywords.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        public List<String> inspectorFields() {
            return splitFields(fieldInspector);
        }

        public List<String> empresaFields() {
            return splitFields(fieldEmpresa);
        }

        public List<String> contratoFields() {
            return splitFields(fieldContrato);
        }

        public List<String> referenciaFields() {
            return splitFields(fieldReferencia);
        }

        public List<String> fechaFields() {
            return splitFields(fieldFecha);
        }

        private List<String> splitFields(String raw) {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        public String getFormTitleKeywords() { return formTitleKeywords; }
        public void setFormTitleKeywords(String formTitleKeywords) { this.formTitleKeywords = formTitleKeywords; }
        public String getTipoActa() { return tipoActa; }
        public void setTipoActa(String tipoActa) { this.tipoActa = tipoActa; }
        public String getConsecutivoPrefix() { return consecutivoPrefix; }
        public void setConsecutivoPrefix(String consecutivoPrefix) { this.consecutivoPrefix = consecutivoPrefix; }
        public String getFieldInspector() { return fieldInspector; }
        public void setFieldInspector(String fieldInspector) { this.fieldInspector = fieldInspector; }
        public String getFieldEmpresa() { return fieldEmpresa; }
        public void setFieldEmpresa(String fieldEmpresa) { this.fieldEmpresa = fieldEmpresa; }
        public String getFieldContrato() { return fieldContrato; }
        public void setFieldContrato(String fieldContrato) { this.fieldContrato = fieldContrato; }
        public String getFieldReferencia() { return fieldReferencia; }
        public void setFieldReferencia(String fieldReferencia) { this.fieldReferencia = fieldReferencia; }
        public String getFieldFecha() { return fieldFecha; }
        public void setFieldFecha(String fieldFecha) { this.fieldFecha = fieldFecha; }
    }
}
