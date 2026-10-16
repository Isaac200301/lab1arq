# lab1arq — UdeaBank

[![CI/CD Pipeline](https://github.com/Isaac200301/lab1arq/actions/workflows/build.yml/badge.svg)](https://github.com/Isaac200301/lab1arq/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Isaac200301_lab1arq&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Isaac200301_lab1arq)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Isaac200301_lab1arq&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Isaac200301_lab1arq)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Isaac200301_lab1arq&metric=bugs)](https://sonarcloud.io/summary/new_code?id=Isaac200301_lab1arq)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=Isaac200301_lab1arq&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=Isaac200301_lab1arq)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Isaac200301_lab1arq&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=Isaac200301_lab1arq)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Isaac200301_lab1arq&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=Isaac200301_lab1arq)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=Isaac200301_lab1arq&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=Isaac200301_lab1arq)
[![Known Vulnerabilities](https://snyk.io/test/github/Isaac200301/lab1arq/badge.svg)](https://snyk.io/test/github/Isaac200301/lab1arq)

Laboratorio 1 de Arquitectura de Software (Universidad de Antioquia).

API REST de un banco simple construida con Spring Boot y MySQL, con un frontend
en React que consume todas sus rutas.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 4.0.8 · Java 17+ · Spring Data JPA · Hibernate 7 |
| Base de datos | MySQL 8 |
| Mapeo DTO ↔ entidad | MapStruct |
| Frontend | React 19 · Vite 6 · CSS plano |

## Estructura

```
lab1arq/
├── src/main/java/com/udea/lab1arq/
│   ├── config/         configuración de CORS
│   ├── controller/     endpoints REST
│   ├── DTO/            objetos de transferencia
│   ├── entity/         entidades JPA
│   ├── exception/      manejador global de excepciones
│   ├── mapper/         mappers de MapStruct
│   ├── repository/     repositorios de Spring Data
│   └── service/        lógica de negocio
├── src/main/resources/
│   └── application.properties
└── frontend/           aplicación React (ver frontend/README.md)
```

## Puesta en marcha

### 1. Base de datos

Con MySQL corriendo, crea el schema. Las tablas las genera Hibernate al
arrancar (`spring.jpa.hibernate.ddl-auto=update`).

```sql
CREATE DATABASE IF NOT EXISTS udeabank
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 2. Credenciales

`application.properties` no contiene contraseñas: las lee de variables de
entorno. La única obligatoria es `DB_PASSWORD`.

| Variable | Por defecto | Descripción |
|---|---|---|
| `DB_PASSWORD` | — **obligatoria** | contraseña del usuario de MySQL |
| `DB_USERNAME` | `root` | usuario de MySQL |
| `DB_HOST` | `localhost` | host del servidor |
| `DB_PORT` | `3306` | puerto |
| `DB_NAME` | `udeabank` | nombre del schema |

**IntelliJ IDEA** — `Run` → `Edit Configurations…` → selecciona
`Lab1arqApplication` → campo `Environment variables` → escribe
`DB_PASSWORD=tu_password`.

**Windows (CMD)**

```cmd
set DB_PASSWORD=tu_password
mvnw.cmd spring-boot:run
```

**Linux / macOS**

```bash
DB_PASSWORD=tu_password ./mvnw spring-boot:run
```

Si la variable falta, la aplicación no arranca y el log dice
`Could not resolve placeholder 'DB_PASSWORD'`, que señala el problema exacto.

### 3. Backend

```bash
./mvnw spring-boot:run       # Linux / macOS
mvnw.cmd spring-boot:run     # Windows
```

Queda escuchando en `http://localhost:8088`.

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Queda en `http://localhost:5173`. El backend debe estar arriba antes de usarlo.

## API

### Clientes

| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| `GET` | `/api/customers` | — | lista de clientes |
| `GET` | `/api/customers/{id}` | — | un cliente |
| `POST` | `/api/customers` | `{firstName, lastName, accountNumber, balance}` | cliente creado |
| `PUT` | `/api/customers/{id}` | mismos campos (parciales permitidos) | cliente actualizado |
| `DELETE` | `/api/customers/{id}` | — | `204 No Content` |

### Transacciones

| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| `POST` | `/api/transactions` | `{senderAccountNumber, receiverAccountNumber, amount}` | transacción creada |
| `GET` | `/api/transactions/{accountNumber}` | — | movimientos de esa cuenta |

### Diagnóstico

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/version` | `{application, version}` — versión del artefacto desplegado, tomada del `pom.xml` |
| `GET` | `/health` | `{status, database}` — `200` si la base de datos acepta conexiones, `503` si no |

`/health` abre una conexión real a la base de datos en lugar de responder solo
que el proceso está vivo. En la nube distingue un contenedor que arrancó de uno
que además alcanza PostgreSQL.

Ejemplo:

```bash
curl -X POST http://localhost:8088/api/customers \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Isaac","lastName":"Cano","accountNumber":"1001234567","balance":1000000}'
```

## Vistas del frontend

1. **Clientes** — consulta, creación, edición y eliminación.
2. **Transferencias** — envío de dinero entre cuentas, con validación de saldo.
3. **Histórico** — movimientos por cliente, con totales enviados y recibidos.

## Notas de implementación

**Las credenciales salen del código.** Dejar una contraseña en un archivo
versionado la publica en el historial de git para siempre, incluso si se borra
después. Leerla del entorno mantiene el repositorio limpio y permite que cada
máquina use la suya.

**`transferMoney` es `@Transactional`.** Una transferencia son cuatro
escrituras encadenadas (debitar, acreditar, guardar ambos clientes, guardar la
transacción). Sin la anotación, un fallo a mitad de camino descontaría el
dinero del remitente sin abonárselo al receptor.

**La marca de tiempo la asigna el servidor.** Si viniera en el cuerpo de la
petición, el reloj del cliente decidiría cuándo ocurrió una transacción
bancaria.

**El dialecto de Hibernate no se declara.** Hibernate 7 lo deduce de los
metadatos que devuelve el driver JDBC. Declararlo a mano es innecesario y
frágil: `MySQL8Dialect`, el nombre habitual en versiones anteriores, fue
eliminado en Hibernate 7.

**CORS está configurado en `config/WebConfig.java`** para el origen
`http://localhost:5173`. Si cambias el puerto del frontend, hay que agregar el
nuevo origen ahí.

## Pendiente conocido

Los montos usan `Double`, que se mapea a `DOUBLE` en MySQL. La aritmética en
punto flotante binario no representa los decimales de forma exacta, así que los
saldos pueden acumular error. Lo correcto para valores monetarios es
`BigDecimal` con `@Column(precision = 19, scale = 2)`, que genera
`DECIMAL(19,2)`.

## Integración continua y despliegue

El repositorio incluye un pipeline de CI/CD en GitHub Actions
(`.github/workflows/build.yml`) con cinco trabajos encadenados:

| Trabajo | Qué hace |
|---|---|
| `tests` | Ejecuta las pruebas unitarias sobre H2 en memoria y publica el informe de JaCoCo |
| `sonar` | Analiza la calidad del código en SonarCloud e incorpora la cobertura |
| `snyk` | Busca vulnerabilidades en las dependencias declaradas en el `pom.xml` |
| `build` | Empaqueta el JAR y publica la imagen en Docker Hub |
| `deploy` | Publica la imagen en Artifact Registry, despliega en Google Cloud Run y verifica que la API responda |

El trabajo de despliegue solo se ejecuta sobre `main`: un pull request abierto
corre las pruebas y el análisis, pero no modifica el entorno desplegado.

### Ejecutar la imagen publicada

```shell
docker pull rumpeles123/lab1arq:latest
docker run -p 8080:8080   -e SPRING_PROFILES_ACTIVE=cloud   -e DB_HOST=... -e DB_NAME=... -e DB_USERNAME=... -e DB_PASSWORD=...   rumpeles123/lab1arq:latest
```

### Perfiles de configuración

| Perfil | Base de datos | Archivo |
|---|---|---|
| (por defecto) | MySQL local | `application.properties` |
| `cloud` | PostgreSQL (Supabase) | `application-cloud.properties` |
| pruebas | H2 en memoria | `src/test/resources/application.properties` |

Ninguno de esos archivos contiene credenciales: todas se resuelven desde
variables de entorno.
