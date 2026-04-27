# Plan de migración — Backend Spring Boot + Cliente JavaFX

Stack objetivo: **Spring Boot** (API REST) + **MariaDB** (VM en la nube) + cliente **JavaFX** adaptado.

---

## Orden de implementación recomendado

```
1º Seguridad/Auth  →  2º Entidades JPA  →  3º Repositories  →  4º DTOs
→  5º Services  →  6º Controllers  →  7º Manejo de errores
(secciones 4 → 2 → 3 → 5 → 6 → 7 → 8 del documento)
```

Sin autenticación funcionando no se puede probar ningún otro endpoint.

---

## Backend — Spring Boot

### 1. Setup del proyecto
- Spring Initializr: `Spring Web`, `Spring Security`, `Spring Data JPA`, driver MariaDB, `jjwt`
- Estructura de paquetes: `model`, `repository`, `service`, `controller`, `security`, `dto`, `exception`
- `application.properties` apuntando a la MariaDB de la VM
- Credenciales de BD en variables de entorno, nunca en el fichero de configuración

### 2. Entidades JPA *(2º en el orden)*
- Una entidad por tabla: `Componente`, `Reparacion`, `Tecnico`, `Proveedor`, `CompraComponente`, `ReparacionComponente`
- Columna `UPDATED_AT` gestionada con `@PreUpdate` en lugar de trigger SQL
- Constraints de concurrencia: `CHECK (stock >= 0)`, `UNIQUE (imei)` donde aplique

### 3. Repositories *(3º en el orden)*
- Un `JpaRepository<Entidad, ID>` por entidad — CRUD básico gratis
- Queries personalizadas con `@Query` para historial con filtros y estadísticas

### 4. Seguridad y autenticación *(1º en el orden)*
- `JwtUtil`: generar y validar tokens con tiempo de expiración configurable
- `JwtAuthenticationFilter`: intercepta cada request, valida el token, carga el usuario en el contexto
- `SecurityConfig`: reglas por rol — qué endpoints puede llamar `TECNICO`, `SUPERTECNICO`, `ADMIN`
- Endpoint `POST /api/auth/login`: recibe usuario+contraseña, devuelve JWT

### 5. DTOs *(4º en el orden)*
- Clases de request/response separadas de las entidades JPA
- Evita exponer campos internos y desacopla la API del esquema de BD

### 6. Services (lógica de negocio) *(5º en el orden)*
- Extraer la lógica actual de los DAOs a la capa service
- `@Transactional` en operaciones que toquen varias tablas (recepción de pedido → actualización de stock)
- Validaciones de concurrencia: verificar `UPDATED_AT` antes de escribir, lanzar excepción si hay conflicto → 409

### 7. Controllers REST *(6º en el orden)*

| Endpoint base | Función |
|---|---|
| `POST /api/auth/login` | Autenticación, devuelve JWT |
| `/api/tecnicos` | CRUD técnicos (solo ADMIN/SUPERTECNICO) |
| `/api/componentes` | Consulta y actualización de stock |
| `/api/reparaciones` | Historial, asignaciones, formulario de reparación |
| `/api/pedidos` | Gestión de compras y recepción |
| `/api/proveedores` | CRUD proveedores |
| `/api/estadisticas` | Datos para gráficas (filtrados por rol) |

### 8. Manejo global de errores *(7º en el orden)*
- `@ControllerAdvice` que mapea excepciones a HTTP:
  - `StaleDataException` → 409 Conflict
  - `AccessDeniedException` → 403 Forbidden
  - Errores de validación → 400 Bad Request
  - `UsernameNotFoundException` → 401 Unauthorized

### 9. Tests
- Tests de integración sobre los endpoints críticos: auth, stock, asignaciones
- Base de datos H2 en memoria para el entorno de tests

### 10. Despliegue en la VM
- Empaquetar como JAR ejecutable (`mvn package`)
- Configurar como servicio `systemd` para arranque automático
- Variables de entorno para credenciales (no en `application.properties`)

---

## Cliente JavaFX — adaptación

### 11. `ApiClient` (reemplaza las conexiones JDBC directas)
- `HttpClient` de Java 11 como base
- Interceptor central: adjunta el JWT en cada request, maneja 401 (sesión expirada → login) y 409 (conflicto → alert al usuario)
- Un método por tipo de operación: `get()`, `post()`, `put()`, `delete()`

### 12. Migración de DAOs
- Cada `DAO.metodo()` pasa a ser una llamada HTTP a través de `ApiClient`
- Los controladores no notan el cambio — misma interfaz, diferente transporte
- Migrar DAO por DAO, probando cada uno antes de continuar

### 13. Gestión de sesión con JWT
- `Sesion` guarda el token en memoria (no en disco ni en fichero)
- Al expirar el token, el interceptor de `ApiClient` navega al login automáticamente
- Los roles siguen en `Sesion` para decisiones de UI, pero la autorización real la aplica Spring Security en el servidor

---

## Validaciones de concurrencia a implementar (post-migración)

Documentadas en detalle en `multiusuario.md` — sección 7.

| Validación | Mecanismo | Capa |
|---|---|---|
| Stock negativo | `CHECK (stock >= 0)` + 409 | BD + API |
| Asignaciones duplicadas | `UNIQUE (imei)` o `SELECT FOR UPDATE` + 409 | BD + API |
| Recepción simultánea de pedidos | Transacción única pedido + stock + 409 | API |
| Vistas desactualizadas | Polling 30s + SSE a largo plazo | Cliente + API |
| Expiración JWT | Interceptor central en `ApiClient` | Cliente |
