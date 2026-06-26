# Arquitectura, Roadmap e Integración

**Sistema de Trazabilidad y Control Documental de Actas Ambientales**  
**Municipalidad de Heredia — Gestión Ambiental**  
**Versión del prototipo:** 0.1.0-DEMO

Documento de apoyo para presentación ante el departamento de Tecnología.

---

## 1. Arquitectura

### 1.1 Visión general

El sistema es una **aplicación monolítica** (Spring Boot) que centraliza **metadatos**, **estados documentales**, **historial auditado** y **documentos adjuntos** de las actas ambientales. **No reemplaza** Survey123, ArcGIS ni Outlook; los **complementa** como capa de trazabilidad y control.

```mermaid
flowchart LR
    subgraph Existente["Ecosistema actual (sin cambios)"]
        S123[Survey123 / ArcGIS]
        OUT[Microsoft Outlook]
    end

    subgraph Nuevo["Nuevo — trazabilidad-actas-ambiental"]
        APP[Sistema de actas<br/>Spring Boot]
    end

    subgraph Usuarios["Usuarios internos"]
        GEST[Gestor ambiental]
        SUP[Supervisor]
    end

    S123 -->|Correo con PDF| OUT
    OUT -->|Automatización| APP
    GEST --> APP
    SUP --> APP
```

### 1.2 Principios de diseño

| Principio | Descripción |
|-----------|-------------|
| **Complementar, no reemplazar** | Survey123 sigue en campo; Outlook sigue recibiendo correos. |
| **Bajo costo** | Stack open source: Java 17, Spring Boot, PostgreSQL, Liquibase. |
| **Evolución por fases** | Registro manual → API → firma/nube automática → SIG. |
| **API-first para integración** | Power Automate y futuros sistemas se conectan vía REST JSON. |
| **Auditoría** | Cada cambio de estado queda en `historial_estados`. |

### 1.3 Arquitectura lógica (capas)

```mermaid
flowchart TB
    subgraph Presentacion["Capa de presentación"]
        WEB[Thymeleaf + Bootstrap<br/>Dashboard · Registro · Consulta · Detalle]
    end

    subgraph Integracion["Capa de integración"]
        API[REST API<br/>POST /api/actas]
        FILT[ApiKeyFilter<br/>X-API-KEY]
    end

    subgraph Negocio["Capa de negocio"]
        SVC[ActaService<br/>Estados · alertas · búsqueda]
        DOC[DocumentoActaService<br/>Ver / descargar PDF]
    end

    subgraph Datos["Capa de datos"]
        REPO[Spring Data JPA]
        LB[Liquibase]
        PG[(PostgreSQL)]
        UP[Almacén archivos<br/>./uploads]
    end

    WEB --> SVC
    API --> FILT --> SVC
    SVC --> DOC
    SVC --> REPO
    SVC --> UP
    REPO --> PG
    LB --> PG
```

| Capa | Responsabilidad | Ubicación en código |
|------|-----------------|---------------------|
| Presentación | Pantallas web para gestores y supervisores | `web/`, `templates/` |
| Integración | API REST para Power Automate y sistemas externos | `api/`, `config/ApiKeyFilter` |
| Negocio | Reglas de flujo, alertas, historial, documentos | `service/` |
| Datos | Persistencia y migraciones de esquema | `repository/`, `model/`, `db/changelog/` |

### 1.4 Arquitectura física — despliegue

#### Opción A: Servidor interno municipal (producción propuesta)

```mermaid
flowchart TB
    subgraph RedMunicipal["Red municipal"]
        USR[Usuarios<br/>navegador]
        LDAP[(Active Directory / LDAP)]
    end

    subgraph Servidor["Servidor interno"]
        NGINX[Nginx / IIS<br/>HTTPS]
        APP[Spring Boot :8080]
        PG[(PostgreSQL)]
        FS[Carpeta uploads<br/>o almacén documental]
    end

    USR -->|HTTPS| NGINX --> APP
    APP --> PG
    APP --> FS
    APP -.->|Fase producción| LDAP
```

#### Opción B: Demo en nube (implementada — AWS)

```mermaid
flowchart TB
    DNS[Route 53<br/>actas.dominio.com]
    EC2[EC2 Ubuntu]
    NGX[Nginx :443<br/>Let's Encrypt]
    DOCKER[Docker Compose]
    APP[actas-app<br/>Spring Boot]
    PG[(actas-postgres)]
    VOL[Volumen Docker<br/>uploads]

    DNS --> EC2 --> NGX --> APP
    DOCKER --> APP
    DOCKER --> PG
    APP --> VOL
```

| Componente | Tecnología | Notas |
|------------|------------|-------|
| Aplicación | Spring Boot 3.2.5 (JAR embebido) | Perfiles: `demo`, `postgres`, `docker` |
| Base de datos | PostgreSQL 16 | Esquema versionado con Liquibase |
| Archivos | Disco local `./uploads` | PDF/Word; ruta en BD (`ruta_documento`) |
| Proxy / TLS | Nginx + Certbot | Guía: `docs/DESPLIEGUE-AWS.md` |
| Contenedores | Docker Compose | `docker-compose.yml` |

### 1.5 Stack tecnológico

| Capa | Tecnología |
|------|------------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Web | Thymeleaf, Bootstrap 5, Chart.js |
| API | REST JSON, Jakarta Validation |
| Persistencia | Spring Data JPA, Hibernate |
| BD | PostgreSQL (prod) / H2 (demo) |
| Migraciones | Liquibase |
| Build | Maven |
| Automatización externa | Microsoft Power Automate |
| SIG / campo | Esri Survey123, ArcGIS |

### 1.6 Flujo de estados del documento

Cada transición genera un registro en `historial_estados` (quién, cuándo, observación).

```mermaid
stateDiagram-v2
    [*] --> RECIBIDA_CORREO: Registro web o API
    RECIBIDA_CORREO --> POR_FIRMAR: Gestor revisa
    POR_FIRMAR --> FIRMADA: Firma digital
    FIRMADA --> PENDIENTE_CARGA_NUBE: Lista para resguardo
    PENDIENTE_CARGA_NUBE --> EN_NUBE: Subida a nube / JTP
    EN_NUBE --> CERRADA: Expediente completo
    CERRADA --> [*]
```

### 1.7 Modelo de datos (resumen)

```mermaid
erDiagram
    ACTAS ||--o{ HISTORIAL_ESTADOS : tiene

    ACTAS {
        bigint id PK
        varchar consecutivo UK
        varchar empresa
        varchar numero_contrato
        varchar inspector
        date fecha_acta
        varchar estado
        varchar origen
        varchar ruta_documento
        timestamp fecha_registro_sistema
    }

    HISTORIAL_ESTADOS {
        bigint id PK
        bigint acta_id FK
        varchar estado
        timestamp fecha_cambio
        varchar observacion
        varchar usuario
    }
```

---

## 2. Roadmap

Roadmap propuesto en **fases incrementales**, validado con el departamento de Tecnología en cada etapa.

### 2.1 Vista resumida

```mermaid
gantt
    title Roadmap de implementación
    dateFormat YYYY-MM
    axisFormat %b %Y

    section Fase 1 — Base
    Registro web y consulta           :done, f1, 2026-01, 2026-03
    Dashboard KPIs y alertas          :done, f1b, 2026-02, 2026-04
    PostgreSQL + Liquibase            :done, f1c, 2026-02, 2026-04

    section Fase 2 — Integración Microsoft
    API POST /api/actas               :done, f2, 2026-04, 2026-05
    Power Automate correo → registro  :active, f2b, 2026-05, 2026-07
    Adjunto PDF vía API               :f2c, 2026-06, 2026-08

    section Fase 3 — Automatización documental
    PATCH estado (firma / nube)       :f3, 2026-07, 2026-09
    Integración firma digital         :f3b, 2026-08, 2026-11
    Subida automática a nube/JTP      :f3c, 2026-09, 2026-12

    section Fase 4 — Seguridad y producción
    LDAP / Active Directory           :f4, 2026-09, 2026-11
    Roles gestor / supervisor         :f4b, 2026-10, 2026-12
    HTTPS y secretos en Key Vault     :f4c, 2026-10, 2026-12

    section Fase 5 — SIG y datos
    Importación CSV ArcGIS            :f5, 2026-11, 2027-02
    Enlace dashboard SIG              :f5b, 2027-01, 2027-04
    Oracle JDBC (si aplica TI)        :f5c, 2027-02, 2027-05
```

> Las fechas son **orientativas** para la presentación; el calendario real lo define Tecnología según prioridades y recursos.

### 2.2 Detalle por fase

| Fase | Objetivo | Entregables | Estado |
|:----:|----------|-------------|--------|
| **1** | Trazabilidad manual centralizada | Web (dashboard, registro, consulta, detalle), BD, adjuntos, historial | **Implementado** |
| **2** | Automatizar registro desde Outlook | `POST /api/actas`, flujo Power Automate, documentación API | **Parcial** (API lista; flujo PA pendiente TI) |
| **3** | Reducir trabajo manual (firma y nube) | `PATCH` estado, conector firma digital, subida SharePoint/JTP | **Propuesto** |
| **4** | Pasar a producción segura | LDAP/AD, roles, HTTPS obligatorio, rotación de secretos | **Propuesto** |
| **5** | Integración SIG y alineación TI | Import ArcGIS, enlace SIG, migración Oracle si aplica | **Propuesto** |

### 2.3 Fase 1 — Base operativa *(completada)*

| Ítem | Descripción |
|------|-------------|
| Registro de actas | Formulario web con adjunto PDF/Word |
| Consulta | Filtros por empresa, contrato, consecutivo, inspector, estado, fechas |
| Dashboard | KPIs, gráficos (estado, tipo, gestor), alertas de pendientes |
| Detalle | Cambio de estado, historial, ver/descargar documento |
| Persistencia | PostgreSQL + Liquibase; empresa y contrato opcionales |
| Demo | Perfil H2 en memoria para demostraciones sin BD |

### 2.4 Fase 2 — Integración Microsoft 365

| Ítem | Descripción | Dependencia TI |
|------|-------------|----------------|
| Power Automate | Trigger: correo nuevo en carpeta Actas → `POST /api/actas` | Licencia M365, cuenta de servicio |
| Mapeo de campos | Extraer consecutivo, empresa, contrato del cuerpo del correo Survey123 | Plantilla de correo actual |
| API key segura | `APP_API_KEY` en Azure Key Vault o gestor de secretos | Política de secretos municipal |
| Adjunto en API | Extender API para recibir PDF desde el flujo (multipart) | Desarrollo + prueba de flujo |

### 2.5 Fase 3 — Automatización firma y nube

| Paso manual actual | Automatización propuesta |
|--------------------|--------------------------|
| Descargar PDF del correo | Power Automate guarda adjunto o API multipart |
| Carpetas locales | Documento en servidor; vista en navegador |
| Firma digital externa | Flujo envía a proveedor de firma → `PATCH` estado `FIRMADA` |
| Subida manual a nube | Power Automate o servicio sube a SharePoint/JTP → `PATCH` estado `EN_NUBE` |
| Renombrar archivo "SUBIDA" | Estado `EN_NUBE` en BD + historial |

### 2.6 Fase 4 — Seguridad y producción

| Control | Demo actual | Producción |
|---------|-------------|------------|
| Login web | Sin autenticación | LDAP / Active Directory |
| Roles | No implementados | Gestor, supervisor, solo lectura |
| API | `X-API-KEY` | OAuth2 / mTLS + Key Vault |
| Transporte | HTTP (local) | HTTPS obligatorio |
| Documentos | Acceso por URL | Permisos por rol |
| Secretos | Valores demo en config | Variables de entorno / Key Vault |

### 2.7 Fase 5 — SIG y alineación con infraestructura municipal

| Ítem | Descripción |
|------|-------------|
| ArcGIS | Importación CSV o Feature Service para cruzar casos de campo |
| Dashboard SIG | Enlace o API GET para consulta desde mapa institucional |
| Oracle | Migración JDBC si TI centraliza en Oracle (JPA facilita el cambio) |
| OTRS / otros | Campo `origen` ya contempla integraciones adicionales vía REST |

### 2.8 Reducción de trabajo manual — antes y después

```mermaid
flowchart LR
    subgraph Antes["Proceso actual (manual)"]
        A1[Correo Outlook] --> A2[Descargar PDF]
        A2 --> A3[Carpetas locales]
        A3 --> A4[Firma externa]
        A4 --> A5[Subir a nube]
        A5 --> A6[Renombrar SUBIDA]
    end

    subgraph Despues["Con sistema (fases 1–3)"]
        B1[Correo Outlook] --> B2[Registro automático]
        B2 --> B3[Revisión en navegador]
        B3 --> B4[Firma orquestada]
        B4 --> B5[Subida automática]
        B5 --> B6[Estado EN_NUBE en BD]
    end
```

---

## 3. Diagrama de integración

### 3.1 Integración end-to-end (actual + futuro)

```mermaid
flowchart TB
    subgraph Campo["Captura en campo — Esri"]
        S123[Survey123]
        AGOL[ArcGIS Online / Enterprise]
        S123 --> AGOL
    end

    subgraph Microsoft["Microsoft 365 — Municipalidad"]
        OUT[Outlook<br/>Bandeja Actas]
        PA_REG[Power Automate<br/>Flujo registro]
        PA_FIRMA[Power Automate<br/>Flujo firma]
        PA_NUBE[Power Automate<br/>Flujo nube]
        SP[SharePoint / OneDrive / JTP]
        FIRMA[Proveedor firma digital<br/>municipal]
    end

    subgraph Sistema["trazabilidad-actas-ambiental"]
        API[API REST<br/>/api/actas]
        WEB[Interfaz web]
        SVC[ActaService]
        PG[(PostgreSQL)]
        UP[./uploads]
    end

    subgraph Usuarios["Gestión Ambiental"]
        GEST[Gestor]
        SUP[Supervisor]
    end

  subgraph Futuro["Fase 5 — opcional"]
        ORA[(Oracle<br/>si TI lo define)]
        SIG[Dashboard SIG]
    end

    S123 -->|"Correo con PDF/Word"| OUT
    OUT -->|"Trigger: nuevo correo"| PA_REG
    PA_REG -->|"POST JSON + X-API-KEY"| API
    PA_REG -.->|"Fase 2b: adjunto"| UP

    GEST --> WEB
    SUP --> WEB
    WEB --> SVC
    API --> SVC
    SVC --> PG
    SVC --> UP

    GEST -->|"Aprueba para firma"| PA_FIRMA
    PA_FIRMA --> FIRMA
    FIRMA -->|"PDF firmado"| PA_FIRMA
    PA_FIRMA -->|"PATCH estado FIRMADA"| API

    PA_FIRMA --> PA_NUBE
    PA_NUBE -->|"Upload file"| SP
    PA_NUBE -->|"PATCH estado EN_NUBE"| API

    AGOL -.->|"Fase 5: CSV / API"| API
    SIG -.->|"Fase 5: GET consulta"| API
    PG -.->|"Fase 5: JDBC"| ORA
```

### 3.2 Matriz de integración por herramienta

| Herramienta | Rol actual | Integración con el sistema | Mecanismo | Factibilidad |
|-------------|------------|------------------------------|-----------|:------------:|
| **Survey123** | Captura en campo, envía correo | Origen `SURVEY123`; metadatos en registro | Correo → Outlook → Power Automate → API | Alta |
| **ArcGIS** | Almacén espacial de casos | Cruce de referencia / importación | CSV export, Feature Service, REST | Media |
| **Outlook** | Recepción de actas | Disparador de automatización | Power Automate (When email arrives) | Alta |
| **Power Automate** | Automatización M365 | Registro, firma, subida nube | HTTP POST/PATCH + conectores M365 | Alta |
| **SharePoint / JTP** | Resguardo en nube | Destino de PDF firmado | Upload file + actualización de estado | Media–alta |
| **Firma digital** | Validez legal del documento | Orquestación del flujo | API del proveedor municipal | Media–alta |
| **Oracle** | BD corporativa (si aplica) | Persistencia centralizada | JDBC + dialecto Hibernate | Media–alta |
| **Active Directory** | Identidad municipal | Login y roles | Spring Security + LDAP | Alta |

### 3.3 Flujo de integración — Fase 2 (registro automático)

```mermaid
sequenceDiagram
    participant S123 as Survey123
    participant OUT as Outlook
    participant PA as Power Automate
    participant API as API /api/actas
    participant SVC as ActaService
    participant PG as PostgreSQL
    participant GEST as Gestor

    S123->>OUT: Correo con acta (PDF + metadatos)
    OUT->>PA: Trigger — nuevo correo en carpeta Actas
    PA->>PA: Extraer consecutivo, empresa, contrato, inspector
    PA->>API: POST JSON + X-API-KEY
    API->>SVC: Validar y registrar
    SVC->>PG: INSERT acta + historial
    API-->>PA: 201 Created + enlaceDetalle
    PA->>GEST: Notificación opcional con enlace
    GEST->>SVC: Revisa en web /actas/{id}
```

### 3.4 Flujo de integración — Fase 3 (firma y nube)

```mermaid
sequenceDiagram
    participant GEST as Gestor
    participant WEB as Interfaz web
    participant PA as Power Automate
    participant FIRMA as Firma digital
    participant NUBE as SharePoint / JTP
    participant API as API /api/actas
    participant PG as PostgreSQL

    GEST->>WEB: Revisa PDF — estado POR_FIRMAR
    GEST->>PA: Solicita firma (o trigger automático)
    PA->>FIRMA: Enviar documento a firmar
    FIRMA-->>PA: PDF firmado
    PA->>API: PATCH estado FIRMADA
    API->>PG: UPDATE + historial

    PA->>NUBE: Subir PDF firmado
    NUBE-->>PA: Confirmación
    PA->>API: PATCH estado EN_NUBE
    API->>PG: UPDATE + historial
    GEST->>WEB: Consulta — acta en nube, sin renombrar archivos
```

### 3.5 Endpoints API — estado de implementación

| Método | Ruta | Uso | Estado |
|--------|------|-----|--------|
| POST | `/api/actas` | Registrar acta desde Outlook / Power Automate | **Implementado** |
| GET | `/api/actas` | Consulta con filtros | Documentado |
| GET | `/api/actas/{consecutivo}` | Detalle por consecutivo | Documentado |
| PATCH | `/api/actas/{consecutivo}/estado` | Actualizar estado (firma, nube) | Documentado |

Documentación detallada: [`docs/API-POWER-AUTOMATE.md`](API-POWER-AUTOMATE.md)

### Versión PDF (presentación)

Para exportar el documento con diagramas Mermaid en PNG embebidos:

```powershell
cd docs
python -m pip install -r requirements-pdf.txt
python build-pdf.py
```

Genera:

| Archivo | Descripción |
|---------|-------------|
| `ARQUITECTURA-ROADMAP-INTEGRACION.pdf` | Documento listo para imprimir o presentar |
| `diagrams/*.png` | 11 diagramas exportados |
| `ARQUITECTURA-ROADMAP-INTEGRACION.html` | Vista previa en navegador |
---

## Referencias

| Documento | Contenido |
|-----------|-----------|
| [`README.md`](../README.md) | Inicio rápido y visión general |
| [`docs/PROPUESTA-ETAPA2-DISENO.md`](PROPUESTA-ETAPA2-DISENO.md) | Propuesta completa Etapa 2 |
| [`docs/API-POWER-AUTOMATE.md`](API-POWER-AUTOMATE.md) | Contrato API y flujos Power Automate |

---

*Municipalidad de Heredia — Gestión Ambiental · Prototipo 0.1.0-DEMO*
