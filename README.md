# Sistema de Trazabilidad y Control Documental — Actas Ambientales

**Prototipo demo** — Municipalidad de Heredia, Gestión Ambiental.

## Opinión / enfoque

Este prototipo **complementa** Survey123, ArcGIS y Outlook. No los reemplaza.

- **Survey123** sigue capturando en campo.
- **Outlook** sigue siendo canal de recepción del PDF/Word.
- **Este sistema** registra metadatos, estados, contrato y trazabilidad hasta la nube.

## Requisitos

- Java 17+
- Maven 3.8+

### Demo rápida (sin PostgreSQL)

```powershell
cd C:\Users\susan\Documents\trazabilidad-actas-ambiental
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Abrir: http://localhost:8080/dashboard

### Con PostgreSQL

```sql
CREATE DATABASE actas_ambiental;
CREATE USER actas_user WITH PASSWORD 'cambiar_en_produccion';
GRANT ALL PRIVILEGES ON DATABASE actas_ambiental TO actas_user;
```

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Pantallas

| Ruta | Función |
|------|---------|
| `/dashboard` | KPIs por estado + alertas |
| `/actas/nueva` | Registro (simula llegada por Outlook) |
| `/actas/consulta` | Buscar por empresa, contrato, consecutivo, inspector |
| `/actas/{id}` | Detalle + historial + cambio de estado |

## Estados del flujo

1. `RECIBIDA_CORREO` — Llegó adjunto a Outlook  
2. `POR_FIRMAR` — En servidor, Word o PDF sin cerrar  
3. `FIRMADA` — Firma digital aplicada  
4. `PENDIENTE_CARGA_NUBE` — Lista pero no subida a JTP  
5. `EN_NUBE` — Resguardo en nube  
6. `CERRADA` — Expediente completo  

## Integración futura (propuesta a TI)

| Fase | Integración |
|------|-------------|
| 1 | Registro manual al recibir correo (web) |
| 2 | **Power Automate → POST `/api/actas`** (ver `docs/API-POWER-AUTOMATE.md`) |
| 3 | PATCH `/api/actas/{consecutivo}/estado` al firmar / subir a nube |
| 4 | Export CSV ArcGIS → importación |
| 5 | Enlace consulta con dashboard SIG |

### API REST (Etapa 2)

| Método | Ruta | Uso |
|--------|------|-----|
| POST | `/api/actas` | Registrar acta desde Outlook |
| GET | `/api/actas` | Consulta con filtros |
| GET | `/api/actas/{consecutivo}` | Detalle |
| PATCH | `/api/actas/{consecutivo}/estado` | Actualizar estado |

Cabecera: `X-API-KEY: demo-api-key-heredia` (configurable en `application.yml`).

Documentación completa: **`docs/API-POWER-AUTOMATE.md`**

Demo Survey123 Inspección (webhook sin Power Automate): **`docs/DEMO-SURVEY123-INSPECCION.md`**

Arquitectura, roadmap e integración (presentación TI): **`docs/ARQUITECTURA-ROADMAP-INTEGRACION.md`**  
PDF con diagramas exportados: **`docs/ARQUITECTURA-ROADMAP-INTEGRACION.pdf`** (regenerar: `python docs/build-pdf.py`)

Despliegue demo en AWS (Route 53 + EC2 + HTTPS): **`docs/DESPLIEGUE-AWS.md`**

## Demo en reunión (5 min)

1. Dashboard con alertas y conteos.  
2. Registrar acta simulando correo Survey123 + contrato `CT-2024-018`.  
3. Consulta por empresa → ver historial de actas del contratista.  
4. Cambiar estado a FIRMADA → EN_NUBE y mostrar historial.  

## Seguridad (propuesta)

- Autenticación LDAP/Active Directory (fase 2).  
- Roles: gestor, supervisor, solo lectura.  
- Auditoría en `historial_estados`.  
- Documentos fuera de webroot; acceso por permiso.  
