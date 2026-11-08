# Óptica Backend

API REST interna para el CRM inicial de una óptica de una sola sucursal.

## Requisitos

- Java 21
- Maven 4 o Maven Wrapper
- PostgreSQL para ejecución local

## Configuración local

Crear una base llamada `optica` y configurar, si es necesario:

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/optica'
$env:DB_USERNAME='postgres'
$env:DB_PASSWORD='postgres'
$env:JWT_SECRET='una-clave-larga-y-segura-para-desarrollo'
```

La aplicación ejecuta las migraciones Flyway automáticamente. Para iniciar:

```powershell
mvn spring-boot:run
```

El wrapper incluido puede depender de la configuración local de PowerShell; si falla, usa Maven instalado.

## Primer usuario

El primer usuario se registra una sola vez y queda como `ADMIN`:

```http
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

{
  "name": "Administrador",
  "email": "admin@optica.local",
  "password": "cambia-esta-clave"
}
```

Después se usa `POST /api/v1/auth/login`. El resto de endpoints requiere el token como `Authorization: Bearer <token>`.

## Módulos principales

- `/api/v1/auth`: registro inicial y login.
- `/api/v1/users`: usuarios internos y perfil actual.
- `/api/v1/patients`: pacientes.
- `/api/v1/patients/{id}/consultations`: historial de consultas optométricas.
- `/api/v1/products`: catálogo y existencia simple.
- `/api/v1/sales`: ventas y partidas.

La colección Postman está en `postman/optica-backend.postman_collection.json`.
