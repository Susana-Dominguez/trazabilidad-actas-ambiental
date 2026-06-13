# API REST — Integración Power Automate (Etapa 2)

Base URL demo: `http://localhost:8080`

Cabecera de seguridad (si `app.api.key` está configurada):

```
X-API-KEY: demo-api-key-heredia
Content-Type: application/json
```

---

## POST /api/actas — Registrar acta desde Outlook

Disparador sugerido en Power Automate: **When a new email arrives** con adjunto PDF/Word de acta.

### Cuerpo JSON (ejemplo)

```json
{
  "consecutivo": "2026-010",
  "empresa": "EcoHeredia S.A.",
  "numeroContrato": "CT-2024-018",
  "inspector": "Josue M.",
  "fechaActa": "2026-06-15",
  "estado": "RECIBIDA_CORREO",
  "origen": "SURVEY123",
  "tipoActa": "Notificación empresas",
  "referenciaCaso": "Incumplimiento ruta Guararí",
  "correoOrigen": "sostenible@municipalidad.go.cr",
  "observacionHistorial": "Registro automático desde correo Outlook",
  "usuario": "power-automate"
}
```

### Respuesta 201 Created

```json
{
  "id": 5,
  "consecutivo": "2026-010",
  "empresa": "EcoHeredia S.A.",
  "numeroContrato": "CT-2024-018",
  "inspector": "Josue M.",
  "fechaActa": "2026-06-15",
  "estado": "RECIBIDA_CORREO",
  "estadoEtiqueta": "Recibida por correo",
  "origen": "SURVEY123",
  "enlaceDetalle": "http://localhost:8080/actas/5"
}
```

### cURL de prueba

```powershell
curl -X POST http://localhost:8080/api/actas `
  -H "Content-Type: application/json" `
  -H "X-API-KEY: demo-api-key-heredia" `
  -d "{\"consecutivo\":\"2026-010\",\"empresa\":\"EcoHeredia S.A.\",\"numeroContrato\":\"CT-2024-018\",\"inspector\":\"Josue M.\",\"fechaActa\":\"2026-06-15\",\"origen\":\"SURVEY123\",\"estado\":\"RECIBIDA_CORREO\"}"
```

---

## GET /api/actas — Consulta

```
GET /api/actas?empresa=EcoHeredia&numeroContrato=CT-2024-018
GET /api/actas?consecutivo=2026-010
```

---

## GET /api/actas/{consecutivo} — Detalle

```
GET /api/actas/2026-010
```

---

## PATCH /api/actas/{consecutivo}/estado — Cambiar estado

Útil cuando otro flujo detecta firma o subida a nube.

```json
{
  "estado": "FIRMADA",
  "observacion": "Firma digital aplicada",
  "usuario": "gestor.ambiental"
}
```

Estados válidos: `RECIBIDA_CORREO`, `POR_FIRMAR`, `FIRMADA`, `PENDIENTE_CARGA_NUBE`, `EN_NUBE`, `CERRADA`

---

## Power Automate — pasos del flujo

1. **Trigger:** When a new email arrives (V3) — carpeta Actas, asunto contiene "Acta".
2. **Parse body** o **Compose** para extraer consecutivo, empresa, contrato (etiquetas en plantilla SIG).
3. **HTTP — POST** `http://servidor-interno:8080/api/actas`
   - Headers: `Content-Type: application/json`, `X-API-KEY: @variables('ApiKey')`
   - Body: JSON con campos arriba
4. **Condition:** status code 201 → opcional notificación al gestor con `enlaceDetalle`.

### Mapeo campos desde correo Survey123 (ejemplo)

| Campo API | Origen en correo |
|-----------|------------------|
| consecutivo | `Numero_acta:` en cuerpo |
| empresa | `Empresa:` |
| numeroContrato | `Contrato:` |
| inspector | `Inspector:` o From |
| fechaActa | `Fecha_acta:` (YYYY-MM-DD) |
| origen | `SURVEY123` fijo o `Tipo_origen:` |
| correoOrigen | From |

---

## Seguridad (propuesta Etapa 2)

| Control | Implementación |
|---------|----------------|
| Autenticación API | Cabecera `X-API-KEY` (demo); producción: OAuth2 / mTLS con TI |
| HTTPS | Obligatorio en servidor municipal |
| Mínimo privilegio | Clave API solo para flujo Power Automate |
| Auditoría | Tabla `historial_estados` + logs del servidor |

En producción la clave debe almacenarse en **Azure Key Vault** o gestor de secretos de TI, no en el flujo en texto plano.
