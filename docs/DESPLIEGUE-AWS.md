# Despliegue AWS (presentación demo)

**Sistema de Trazabilidad y Control Documental de Actas Ambientales**  
Guía paso a paso para publicar el prototipo en internet con dominio propio, HTTPS y costo aproximado **USD 15–25/mes**.

---

## Resumen de la arquitectura

```
Usuario / Jurado
       │
       ▼
Route 53  (actas.tu-dominio.com)
       │
       ▼
Elastic IP  ──►  EC2 t3.micro (Ubuntu)
                      │
                      ├── Nginx :443 (HTTPS, Let's Encrypt)
                      │        └── proxy → localhost:8080
                      │
                      └── Docker Compose
                               ├── app (Spring Boot)
                               └── postgres (PostgreSQL)
```

| Componente | Servicio AWS | Costo aprox. |
|------------|--------------|--------------|
| Dominio `.com` | Route 53 Registrar | ~USD 13–15/año |
| DNS | Route 53 Hosted Zone | ~USD 0.50/mes |
| Servidor | EC2 t3.micro | USD 0–8/mes (free tier 12 meses) |
| IP fija | Elastic IP (asociada a EC2) | **USD 0** |
| HTTPS | Certbot + Let's Encrypt | **USD 0** |
| Base de datos | PostgreSQL en Docker (mismo EC2) | **USD 0** extra |
| **Total demo** | | **~USD 5–15/mes** |

---

## Requisitos previos

- Cuenta AWS activa con tarjeta de crédito/débito.
- Proyecto compilando localmente (`mvn package`).
- Par de claves SSH (AWS lo genera al crear la EC2).
- Dominio deseado (ej. `trazabilidad-actas-demo.com`).

---

## Parte 1 — Registrar el dominio en Route 53

### Paso 1.1 — Buscar y registrar dominio

1. Inicie sesión en [AWS Console](https://console.aws.amazon.com/).
2. Busque **Route 53** en la barra superior.
3. Menú izquierdo → **Registered domains** → **Register domains**.
4. Escriba el nombre deseado, por ejemplo:
   - `trazabilidad-actas-demo.com`
   - `actas-ambiental-demo.com`
5. Revise disponibilidad y precio (~USD 13–15/año para `.com`).
6. Complete contacto (puede usar datos personales para demo académica).
7. Active **Auto-renew** si desea que no expire.
8. Confirme compra.

> **Tiempo:** el dominio puede tardar **5–30 minutos** en quedar activo.

### Paso 1.2 — Verificar Hosted Zone

Al registrar en Route 53, AWS crea automáticamente una **Hosted Zone** con registros NS y SOA.

1. Route 53 → **Hosted zones**.
2. Debe aparecer su dominio (ej. `trazabilidad-actas-demo.com`).
3. Anote el dominio; usará un **subdominio** para la app:
   - `actas.trazabilidad-actas-demo.com`

> **Tip presentación:** un subdominio `actas.` se ve profesional y deja el dominio raíz libre.

---

## Parte 2 — Crear el servidor EC2

### Paso 2.1 — Lanzar instancia

1. Busque **EC2** en AWS Console → **Launch instance**.

2. Configure:

   | Campo | Valor recomendado |
   |-------|-------------------|
   | Name | `actas-demo-server` |
   | AMI | **Ubuntu Server 22.04 LTS** (64-bit x86) |
   | Instance type | **t3.micro** (1 vCPU, 1 GB RAM) |
   | Key pair | **Create new** → descargue el `.pem` y guárdelo seguro |
   | Network | Default VPC, **Auto-assign public IP: Enable** |

3. **Storage:** 20–30 GB gp3 (suficiente para demo).

4. **Security group** — cree reglas:

   | Tipo | Puerto | Origen | Motivo |
   |------|--------|--------|--------|
   | SSH | 22 | **Mi IP** | Solo usted administra |
   | HTTP | 80 | 0.0.0.0/0 | Certbot + redirección HTTPS |
   | HTTPS | 443 | 0.0.0.0/0 | Acceso público seguro |
   | Custom TCP | 8080 | — | **NO abrir** (Nginx hace proxy local) |

5. **Launch instance**.

6. Espere estado **Running**.

### Paso 2.2 — Conectarse por SSH

En PowerShell (Windows), desde la carpeta del `.pem`:

```powershell
# Permisos (primera vez en Windows puede omitirse)
ssh -i "actas-demo-key.pem" ubuntu@IP_PUBLICA_EC2
```

Reemplace `IP_PUBLICA_EC2` por la IP que muestra la consola EC2 (temporal, antes del Elastic IP).

Si conecta correctamente, verá el prompt: `ubuntu@ip-xxx:~$`

---

## Parte 3 — Elastic IP (IP fija gratuita)

> La Elastic IP es **gratis mientras esté asociada a una EC2 en ejecución**. Si la deja sin asociar, AWS cobra ~USD 0.005/h.

### Paso 3.1 — Asignar Elastic IP

1. EC2 → **Elastic IPs** (menú Network & Security).
2. **Allocate Elastic IP address** → **Allocate**.
3. Seleccione la IP → **Actions** → **Associate Elastic IP address**.
4. Instance: `actas-demo-server` → **Associate**.

### Paso 3.2 — Anotar la IP

Ejemplo: `54.123.45.67` — la usará en Route 53.

> A partir de ahora, conéctese por SSH usando la **Elastic IP**, no la IP temporal anterior.

---

## Parte 4 — Configurar DNS en Route 53

### Paso 4.1 — Registro A para subdominio

1. Route 53 → **Hosted zones** → su dominio.
2. **Create record**:

   | Campo | Valor |
   |-------|-------|
   | Record name | `actas` |
   | Record type | **A** |
   | Value | **Elastic IP** (ej. `54.123.45.67`) |
   | TTL | 300 |
   | Routing policy | Simple |

3. **Create records**.

Resultado: `actas.trazabilidad-actas-demo.com` → su EC2.

### Paso 4.2 — Verificar propagación DNS

Desde su PC (puede tardar 1–10 minutos):

```powershell
nslookup actas.trazabilidad-actas-demo.com
```

Debe resolver a su Elastic IP.

También puede probar en el navegador `http://actas.tu-dominio.com` — aún sin app verá error de Nginx o conexión rechazada hasta completar Parte 5.

---

## Parte 5 — Preparar el servidor

Conectado por SSH a la EC2:

### Paso 5.1 — Instalar Docker y Nginx

```bash
# Clonar o subir el proyecto (opción A: git)
sudo apt-get update -y
sudo apt-get install -y git

git clone https://github.com/TU_USUARIO/trazabilidad-actas-ambiental.git
cd trazabilidad-actas-ambiental

# Ejecutar script de instalación
chmod +x deploy/install-server.sh
./deploy/install-server.sh
```

> Si el repo es privado, suba el proyecto con `scp` desde su PC (ver Anexo A).

**Importante:** si el script agregó su usuario al grupo `docker`, cierre SSH y vuelva a entrar:

```bash
exit
ssh -i "actas-demo-key.pem" ubuntu@ELASTIC_IP
```

### Paso 5.2 — Configurar variables de entorno

```bash
cd ~/trazabilidad-actas-ambiental
cp deploy/env.example .env
nano .env
```

Edite con sus valores reales:

```bash
DOMAIN=actas.trazabilidad-actas-demo.com
APP_PUBLIC_BASE_URL=https://actas.trazabilidad-actas-demo.com
POSTGRES_PASSWORD=UnaPasswordSegura2026!
APP_API_KEY=clave-secreta-para-api-demo
SPRING_PROFILES_ACTIVE=docker
```

Guarde (`Ctrl+O`, Enter, `Ctrl+X`).

### Paso 5.3 — Levantar la aplicación con Docker

```bash
docker compose up -d --build
```

Verifique:

```bash
docker compose ps
docker compose logs -f app
```

Espere ver: `Started TrazabilidadActasApplication`.

Prueba local en el servidor:

```bash
curl -s http://127.0.0.1:8080/ping
# Debe responder: OK
```

---

## Parte 6 — Nginx como proxy inverso

### Paso 6.1 — Configurar sitio

```bash
chmod +x deploy/setup-nginx.sh
./deploy/setup-nginx.sh
```

El script:
1. Crea `/etc/nginx/sites-available/actas` con su dominio.
2. Habilita el sitio y recarga Nginx.
3. Pregunta si desea ejecutar Certbot (HTTPS).

### Paso 6.2 — Probar HTTP antes de HTTPS

Desde su navegador:

```
http://actas.trazabilidad-actas-demo.com/dashboard
```

Debe cargar el dashboard (aún sin candado SSL).

---

## Parte 7 — HTTPS con Let's Encrypt (Certbot)

### Paso 7.1 — Obtener certificado

Si no lo hizo en el script anterior:

```bash
sudo certbot --nginx -d actas.trazabilidad-actas-demo.com
```

Responda:
- Email: su correo (avisos de renovación).
- Términos: **Agree**.
- Redirección HTTP→HTTPS: **Yes (2)**.

### Paso 7.2 — Verificar HTTPS

Abra en el navegador:

```
https://actas.trazabilidad-actas-demo.com/dashboard
```

Debe mostrar candado 🔒 y el dashboard.

### Paso 7.3 — Renovación automática

Certbot instala un timer systemd. Verifique:

```bash
sudo certbot renew --dry-run
```

Los certificados Let's Encrypt duran 90 días y se renuevan solos.

---

## Parte 8 — Verificación final para la presentación

### Checklist

- [ ] `https://actas.TU-DOMINIO/dashboard` carga correctamente
- [ ] `https://actas.TU-DOMINIO/ping` responde `OK`
- [ ] Registrar una acta de prueba desde `/actas/nueva`
- [ ] Consulta en `/actas/consulta` funciona
- [ ] API POST (opcional):

```powershell
$body = @{
  consecutivo = "2026-DEMO-01"
  empresa = "EcoHeredia S.A."
  numeroContrato = "CT-2024-018"
  inspector = "Demo Presentación"
  fechaActa = "2026-06-15"
  origen = "SURVEY123"
  estado = "RECIBIDA_CORREO"
} | ConvertTo-Json

Invoke-RestMethod -Uri "https://actas.TU-DOMINIO/api/actas" -Method POST `
  -Headers @{ "X-API-KEY" = "clave-secreta-para-api-demo"; "Content-Type" = "application/json" } `
  -Body $body
```

---

## Parte 9 — Operación y mantenimiento

### Ver logs

```bash
cd ~/trazabilidad-actas-ambiental
docker compose logs -f app
docker compose logs -f postgres
```

### Reiniciar servicios

```bash
docker compose restart
sudo systemctl reload nginx
```

### Actualizar la app (nueva versión)

```bash
cd ~/trazabilidad-actas-ambiental
git pull
docker compose up -d --build
```

### Respaldar base de datos

```bash
docker exec actas-postgres pg_dump -U actas_user actas_ambiental > backup_$(date +%Y%m%d).sql
```

### Detener todo (ahorrar si no presenta por un tiempo)

```bash
docker compose down
# EC2 sigue corriendo (~USD 8/mes). Para ahorrar más: Stop instance en consola AWS.
```

> **Elastic IP:** si detiene la EC2, la IP se mantiene asociada (gratis). Si **termina** la instancia, desasocie la Elastic IP o AWS puede cobrar.

---

## Parte 10 — Seguridad mínima para demo

| Medida | Estado en Opción A |
|--------|-------------------|
| HTTPS | ✅ Let's Encrypt |
| SSH solo desde su IP | ✅ Security Group |
| App no expuesta en :8080 público | ✅ Solo localhost |
| API con X-API-KEY | ✅ Configurable en `.env` |
| Login web / RBAC | ⏳ Fase siguiente (Spring Security) |
| Firewall UFW | ✅ Script install-server.sh |

**Recomendación:** no use contraseñas débiles en `.env`. No suba `.env` a Git.

---

## Anexo A — Subir proyecto sin Git (desde Windows)

En PowerShell, en la carpeta del proyecto:

```powershell
# Crear zip sin target/
Compress-Archive -Path "C:\Users\susan\Documents\trazabilidad-actas-ambiental\*" `
  -DestinationPath "$env:TEMP\actas.zip" -Force

scp -i "C:\ruta\actas-demo-key.pem" "$env:TEMP\actas.zip" ubuntu@ELASTIC_IP:~/

# En el servidor EC2:
ssh -i "actas-demo-key.pem" ubuntu@ELASTIC_IP
sudo apt-get install -y unzip
unzip actas.zip -d trazabilidad-actas-ambiental
cd trazabilidad-actas-ambiental
```

---

## Anexo B — Solución de problemas

| Problema | Causa probable | Solución |
|----------|----------------|----------|
| DNS no resuelve | Propagación | Esperar 10 min; verificar registro A en Route 53 |
| SSH timeout | Security Group | Verificar puerto 22 desde su IP actual |
| `502 Bad Gateway` | App no corre | `docker compose ps`; `docker compose logs app` |
| Certbot falla | DNS no apunta a EC2 | `nslookup actas.tu-dominio.com` debe ser Elastic IP |
| App no conecta a BD | Password incorrecto | Revisar `.env` y `docker compose logs postgres` |
| Elastic IP cobro | IP sin EC2 | Asociar IP a instancia running |
| Build Docker lento | t3.micro poco RAM | Normal 5–10 min; usar swap si falla (ver abajo) |

### Swap opcional (si el build falla por memoria)

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

---

## Anexo C — Costos detallados (referencia 2026)

| Servicio | Detalle | USD/mes |
|----------|---------|---------|
| Route 53 Hosted Zone | 1 zona | 0.50 |
| Route 53 queries | Demo bajo tráfico | < 0.10 |
| EC2 t3.micro | On-Demand us-east-1 | ~7.50 |
| EC2 t3.micro | Free tier (12 meses) | 0.00 |
| Elastic IP | Asociada a EC2 running | 0.00 |
| EBS 30 GB | Almacenamiento | ~2.40 |
| Transferencia datos | Demo < 10 GB | ~0.00 |
| Dominio .com | /12 meses | ~1.15 |
| **Total sin free tier** | | **~10–11** |
| **Total con free tier (año 1)** | | **~2–4** |

> Precios orientativos; verifique en [AWS Pricing Calculator](https://calculator.aws/).

---

## Anexo D — Próximo paso: RBAC (login)

Para la presentación con **control de acceso por roles**, el siguiente incremento es:

1. Agregar `spring-boot-starter-security`.
2. Pantalla `/login` con 2 usuarios demo (`gestor.demo`, `supervisor.demo`).
3. Roles: **GESTOR** (escritura) y **SUPERVISOR** (solo lectura).

Eso se puede desplegar sobre la misma EC2 con `docker compose up -d --build` sin cambiar Route 53 ni HTTPS.

---

## Referencia rápida de archivos en el repo

| Archivo | Propósito |
|---------|-----------|
| `Dockerfile` | Imagen de la app Spring Boot |
| `docker-compose.yml` | App + PostgreSQL |
| `deploy/env.example` | Plantilla de variables |
| `deploy/install-server.sh` | Instala Docker, Nginx, Certbot |
| `deploy/setup-nginx.sh` | Configura Nginx + HTTPS |
| `deploy/nginx/actas.conf.template` | Proxy inverso |

---

*Guía para despliegue demo — Municipalidad de Heredia, Gestión Ambiental — Etapa 2.*
