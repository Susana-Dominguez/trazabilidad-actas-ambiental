# Demo en vivo — Survey123 Inspección → Spring Boot (sin Power Automate)

**Objetivo:** Al enviar el formulario **Inspección** en Survey123, el sistema:

1. Recibe el webhook en Spring Boot  
2. Genera el **consecutivo** (ej. `Inspeccion-2026001`)  
3. Descarga y almacena el **PDF** desde ArcGIS (si hay adjunto)  
4. Registra el acta en trazabilidad  
5. Envía **correo de confirmación** al inspector con el consecutivo y enlace  

**Endpoint webhook (configurar en Survey123):**

```text
POST https://TU-SERVIDOR-PUBLICO/api/webhooks/survey123/inspeccion
```

No requiere cabecera `X-API-KEY` (ruta excluida del filtro API).

---

## Parte 1 — Lo que SIG configura (una sola vez)

### 1.1 Formulario Survey123 — Inspección demo

Crear **un** formulario de prueba (los otros ~8 formularios no se tocan).

| Pregunta | Tipo | Nombre interno (importante) |
|----------|------|----------------------------|
| Inspector | Texto | `inspector` |
| Empresa | Texto | `empresa` |
| N.º contrato | Texto | `numero_contrato` |
| Fecha acta | Fecha | `fecha_acta` |
| Referencia / caso | Texto | `referencia_caso` |
| Adjuntar acta PDF | **Archivo** | (adjunto en capa ArcGIS) |

**Título del formulario:** incluir la palabra **Inspección** (ej. *Acta Inspección Ambiental — Demo*).

> El **consecutivo lo genera el sistema** automáticamente. No es obligatorio un campo consecutivo en Survey123.

### 1.2 Webhook en Survey123

1. Formulario → **Settings** → **Webhooks** → **Add webhook**  
2. **Payload URL:** `https://TU-DOMINIO/api/webhooks/survey123/inspeccion`  
3. **Trigger:** ✅ New record submitted  
4. **Event data:** ✅ Submitted record, ✅ Survey info, ✅ User info  
5. **Save** y activar  

Documentación Esri: [Webhooks Survey123](https://doc.arcgis.com/en/survey123/browser/analyze-results/webhooks.htm)

### 1.3 Desactivar correo con PDF en Survey123 (opcional demo)

Settings → Email notifications → desactivar o dejar solo copia interna.  
Así demuestras que la **confirmación** la envía el sistema de trazabilidad.

---

## Parte 2 — Despliegue del backend

### 2.1 URL pública (obligatorio)

ArcGIS Online debe alcanzar tu servidor. Opciones:

| Opción | Comando / nota |
|--------|----------------|
| **EC2** (recomendado reunión) | `docs/DESPLIEGUE-AWS.md` |
| **ngrok** (laptop) | `ngrok http 8080` → usar URL `https://xxxx.ngrok.io` |

Probar:

```powershell
curl https://TU-SERVIDOR/api/webhooks/survey123/inspeccion
```

Respuesta esperada: `{"status":"OK",...}`

### 2.2 Variables de entorno

Copiar y ajustar (EC2: archivo `.env` o variables Docker):

```env
# App
APP_PUBLIC_BASE_URL=https://actas.tu-dominio.com
POSTGRES_PASSWORD=tu_password

# Correo confirmación al inspector
NOTIFICACIONES_ENABLED=true
MAIL_HOST=smtp.office365.com
MAIL_PORT=587
MAIL_USERNAME=actas-demo@municipalidad.go.cr
MAIL_PASSWORD=contraseña_o_app_password
MAIL_FROM=actas-demo@municipalidad.go.cr
MAIL_GESTOR=gestor.ambiental@municipalidad.go.cr

# ArcGIS — para descargar PDF adjunto del formulario
ARCGIS_TOKEN=token_generado_en_arcgis
# O bien:
# ARCGIS_USERNAME=usuario_sig
# ARCGIS_PASSWORD=contraseña
```

**Obtener token ArcGIS (SIG):** ArcGIS Online → perfil → **Generate token** (válido 60 min para prueba), o usuario/contraseña de cuenta de servicio.

### 2.3 Levantar aplicación

```powershell
cd C:\Users\susan\Documents\trazabilidad-actas-ambiental
mvn spring-boot:run
```

Docker en EC2:

```bash
docker compose up -d --build app
```

---

## Parte 3 — Probar sin Survey123 (antes del lunes)

### 3.1 Simulación local

```powershell
curl -X POST http://localhost:8080/api/webhooks/survey123/inspeccion/test `
  -H "Content-Type: application/json" `
  -d "@docs/ejemplos/webhook-inspeccion-demo.json"
```

Respuesta esperada:

```json
{
  "success": true,
  "consecutivo": "Inspeccion-2026001",
  "actaId": 12,
  "enlaceDetalle": "http://localhost:8080/actas/12",
  "emailEnviado": true,
  "documentoAlmacenado": false,
  "mensaje": "Acta registrada correctamente"
}
```

`documentoAlmacenado: false` es normal en simulación sin ArcGIS real; con webhook real y token ArcGIS debe ser `true`.

### 3.2 Verificar en la web

1. Abrir `/dashboard` → debe subir contador  
2. Abrir `/actas/consulta` → buscar por consecutivo  
3. Abrir enlace del detalle → historial + PDF si se descargó  

---

## Parte 4 — Guion demo el lunes (8 min)

| Min | Acción |
|:---:|--------|
| 1 | Explicar: *9 formularios distintos, 1 webhook por tipo; hoy demo Inspección* |
| 2 | Mostrar formulario Survey123 (campos + adjunto PDF) |
| 3 | Mostrar webhook configurado (URL apunta a Spring Boot) |
| 4 | **Enviar formulario** desde móvil o web Survey123 |
| 5 | Mostrar respuesta 200 en logs del servidor (opcional) |
| 6 | Abrir **correo del inspector** → consecutivo + enlace |
| 7 | Abrir **dashboard** y **detalle** → acta + PDF |
| 8 | Cierre: *“Los otros 8 formularios repiten el mismo patrón con otra URL o mapeo”* |

---

## Parte 5 — Escalar a los ~9 formularios (sin cambiar SIG mucho)

Cada formulario existente:

1. Mantiene sus preguntas actuales  
2. Agrega **un webhook** al mismo servidor con ruta dedicada, por ejemplo:  
   - `/api/webhooks/survey123/inspeccion`  
   - `/api/webhooks/survey123/minuta` *(fase 2)*  
   - `/api/webhooks/survey123/notificacion` *(fase 2)*  
3. El backend mapea campos distintos → **misma tabla `actas`**

SIG solo configura webhooks; no unifica formularios.

---

## Mapeo de campos (Inspección)

Configurable en `application.yml` → `app.survey123.inspeccion`:

```yaml
app:
  survey123:
    inspeccion:
      form-title-keywords: Inspeccion,Inspección
      consecutivo-prefix: Inspeccion
      field-inspector: inspector,inspector_nombre,gestor
      field-empresa: empresa,contratista
      field-contrato: numero_contrato,contrato
      field-referencia: referencia_caso,caso
      field-fecha: fecha_acta,fecha
```

Si el formulario municipal usa otros nombres, SIG solo indica el alias y se agrega a la lista (sin cambiar Survey123).

---

## Solución de problemas

| Problema | Causa probable | Solución |
|----------|----------------|----------|
| Webhook no llega | URL no pública / HTTP | HTTPS + ngrok o EC2 |
| Acta sin PDF | Token ArcGIS | Configurar `ARCGIS_TOKEN` o user/pass |
| Sin correo | SMTP no configurado | `NOTIFICACIONES_ENABLED=true` + Mailtrap o Outlook |
| 400 Bad Request | JSON distinto | Revisar logs; ajustar mapeo en `application.yml` |
| Consecutivo duplicado | Reenvío webhook | Esri puede reintentar; en producción agregar idempotencia por `globalid` |

---

## Referencias en el código

| Componente | Archivo |
|------------|---------|
| Webhook REST | `api/Survey123WebhookController.java` |
| Lógica Inspección | `service/Survey123WebhookService.java` |
| Consecutivo auto | `service/ConsecutivoService.java` |
| PDF ArcGIS | `service/ArcGisAttachmentService.java` |
| Correo | `service/NotificacionEmailService.java` |

---

*Municipalidad de Heredia — Gestión Ambiental · Demo Survey123 Inspección*
