# Soporte multiusuario — rama `dev_multiusuario`

## 1. Contexto y motivación

La aplicación fue diseñada inicialmente asumiendo un único usuario activo. Al gestionarse por varios técnicos y administradores en paralelo aparecen dos categorías de problemas:

- **Conflictos de datos:** dos usuarios modifican el mismo registro a la vez. El último en guardar sobreescribe al primero sin que ninguno reciba aviso.
- **Datos desactualizados:** un usuario ve información que otro ya modificó. Las decisiones se toman sobre una foto del sistema que ya no es real.

Esta rama introduce los mecanismos necesarios para detectar ambas situaciones, informar al usuario de forma comprensible y mantener los datos sincronizados de forma transparente.

---

## 2. Cambios implementados

### 2.1 Columna `UPDATED_AT` y optimistic locking

**Qué es:** técnica para detectar conflictos de escritura sin bloquear la base de datos. En lugar de reservar una fila mientras un usuario la edita, se compara una marca de tiempo antes de escribir.

**Tablas modificadas:** `Componente`, `Reparacion`, `Compra_componente`, `Reparacion_componente`.

**Script de migración:**
```sql
ALTER TABLE Componente
    ADD COLUMN UPDATED_AT TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE Reparacion
    ADD COLUMN UPDATED_AT TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE Compra_componente
    ADD COLUMN UPDATED_AT TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE Reparacion_componente
    ADD COLUMN UPDATED_AT TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
```

> **Compatibilidad PostgreSQL:** `ON UPDATE CURRENT_TIMESTAMP` no existe en Postgres. Habría que sustituirlo por un trigger `BEFORE UPDATE` que ejecute `NEW.updated_at = NOW()`.

**Cómo funciona:**
1. Al cargar un registro se guarda su `UPDATED_AT`.
2. Al escribir (`UPDATE`) se añade `AND UPDATED_AT = ?` a la query.
3. Si `executeUpdate()` devuelve 0 filas afectadas, otro usuario modificó el registro entre la carga y el guardado → se lanza `StaleDataException`.

**Archivos afectados:**

| Archivo | Cambio |
|---|---|
| `crear_bd.sql` | Columnas `UPDATED_AT` en 4 tablas |
| `Componente.java` | Campo `updatedAt` + getter |
| `CompraComponente.java` | Campo `updatedAt` + getter |
| `Reparacion.java` | Campo `updatedAt` + getter |
| `ReparacionResumen.java` | Campo `updatedAt` + getter |
| `ComponenteDAO.java` | `actualizar()` valida `UPDATED_AT`, lanza `StaleDataException` |
| `CompraComponenteDAO.java` | Todas las operaciones de escritura validan `UPDATED_AT` |
| `ReparacionDAO.java` | `actualizarTecnico()` valida `UPDATED_AT` |

**Excepción personalizada:**
```java
// StaleDataException.java
package com.reparaciones.utils;
public class StaleDataException extends Exception {
    public StaleDataException(String mensaje) { super(mensaje); }
}
```

---

### 2.2 Interfaz `Recargable`

Contrato común que implementan todos los controladores de primer nivel. Permite que el sistema ordene una recarga sin conocer el tipo concreto del controlador activo.

```java
// Recargable.java
package com.reparaciones.utils;
public interface Recargable {
    void recargar();
    void detenerPolling();
}
```

Implementado por: `ReparacionControllerAdmin`, `ReparacionControllerTecnico`, `StockController`.

`recargar()` es inteligente: sabe qué panel está visible y recarga solo lo necesario:

```java
// ReparacionControllerAdmin
@Override
public void recargar() {
    if (pnlPendientes.isVisible())         pendientesAdminController.cargar();
    else if (pnlMisPendientes.isVisible()) misPendientesController.cargar();
    else                                   cargarDatos();
}
```

---

### 2.3 Polling automático cada 60 segundos

Un hilo daemon ejecuta `recargar()` en background cada 60 segundos, manteniendo los datos frescos sin que el usuario tenga que hacer nada.

```java
private final ScheduledExecutorService poller =
    Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "poller-reparaciones-admin");
        t.setDaemon(true); // no bloquea el cierre de la JVM
        return t;
    });

// En initialize():
poller.scheduleAtFixedRate(
    () -> Platform.runLater(this::recargar),
    60, 60, TimeUnit.SECONDS);
```

- `Platform.runLater` garantiza que la actualización ocurre en el hilo JavaFX.
- Al cambiar de vista o cerrar sesión, `detenerPolling()` llama `poller.shutdownNow()` para no dejar hilos huérfanos.

---

### 2.4 Recarga al recuperar el foco de la ventana

Cuando el usuario vuelve a la ventana tras consultar otra cosa, los datos se recargan automáticamente.

```java
// MainController.java — en initialize()
contenedor.sceneProperty().addListener((obs, oldScene, newScene) -> {
    if (newScene == null) return;
    newScene.windowProperty().addListener((obs2, oldWin, win) -> {
        if (win == null) return;
        win.focusedProperty().addListener((obs3, wasFocused, isFocused) -> {
            if (isFocused && controladorActivo != null)
                controladorActivo.recargar();
        });
    });
});
```

La cadena de tres listeners es necesaria porque en JavaFX, `Scene` y `Window` no están disponibles en el momento de `initialize()`.

---

### 2.5 Indicador "Actualizado HH:mm"

Un label pequeño debajo de cada tabla muestra la hora de la última carga. El usuario puede saber cuándo fue la última sincronización sin ambigüedad.

**Vistas:**

| Vista | Campo FXML |
|---|---|
| `ReparacionViewAdmin.fxml` | `lblUltimaActualizacion` |
| `ReparacionViewTecnico.fxml` | `lblUltimaActualizacion` |
| `StockView.fxml` (panel stock) | `lblUltimaActStock` |
| `StockView.fxml` (panel pedidos) | `lblUltimaActPedidos` |
| `StockView.fxml` (panel proveedores) | `lblUltimaActProveedores` |

Se actualiza al final de cada método `cargar*()`:
```java
String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
lblUltimaActualizacion.setText("Actualizado " + hora);
```

---

### 2.6 Mensajes de conflicto específicos

Al confirmar cambios de técnico en la tabla de pendientes, si se detecta un `StaleDataException`, el sistema relanza una consulta a BD para determinar el motivo exacto del conflicto y mostrar un mensaje específico por cada asignación afectada.

**Nuevo método en `ReparacionDAO`:**
```java
public Optional<ReparacionResumen> getAsignacionById(String idRep) throws SQLException {
    String sql = SQL_ASIGNACIONES + " AND r.ID_REP = ?";
    try (Connection con = Conexion.getConexion();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, idRep);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return Optional.of(mapearAsignacion(rs));
    }
    return Optional.empty();
}
```

**Lógica de diagnóstico en `PendientesAdminController`:**

| Estado actual en BD | Mensaje al usuario |
|---|---|
| La fila ya no existe como asignación | `• A250410_3: ya no está pendiente (fue completada por otro usuario).` |
| Existe pero con otro técnico | `• A250410_3: fue reasignada a Carlos por otro usuario.` |
| Existe con el mismo técnico | `• A250410_3: fue modificada por otro usuario.` |

Si hay varios conflictos en el mismo lote, el alert los lista todos antes de recargar. Los cambios guardados correctamente no se mencionan.

---

### 2.7 Borrado de asignación con recarga inmediata

Al confirmar un borrado, en lugar de quitar el elemento del `ObservableList` local, se llama `cargar()` directamente.

**Por qué:** con cell factories complejas (ComboBox embebido), `datos.remove(rep)` puede no repintar correctamente todos los componentes de la fila. Además, si el poller hubiera recargado durante el countdown del diálogo de confirmación (5 segundos), la referencia local al objeto estaría obsoleta y el remove no encontraría nada.

---

### 2.8 FormularioCompra — datos frescos al abrir

`FormularioCompraController.init()` llama `componenteDAO.getAll()` y `proveedorDAO.getActivos()` cada vez que se abre el formulario, garantizando que los combos reflejan el estado actual de la BD y no el estado en que estaba la tabla cuando el usuario abrió la vista de stock.

---

## 3. Casos de conflicto y respuesta del sistema

### Caso A — Dos admins asignan técnicos a la vez

Admin A y Admin B tienen abierta la pantalla de Pendientes al mismo tiempo. Ambos ven `REP-042` sin técnico. Admin A la asigna a Carlos y confirma. Medio segundo después, Admin B la asigna a Marta y confirma.

**Sin protección:** la asignación de Carlos se sobreescribe silenciosamente.

**Con optimistic locking:** la query de B incluye `AND UPDATED_AT = ?` con el timestamp que cargó. Como A ya modificó la fila, el `UPDATED_AT` cambió → `executeUpdate() == 0` → `StaleDataException`.

**Lo que ve Admin B:**
> *"Los siguientes cambios no se pudieron guardar:*
> *• A250410_3: fue reasignada a Carlos por otro usuario.*
> *Los datos se han recargado."*

La tabla se recarga mostrando el estado actual. Admin B puede decidir si reasignar o no.

---

### Caso B — Reparación completada mientras se gestionan pendientes

Admin A tiene la lista de pendientes cargada. El técnico asignado completa la reparación, que pasa de asignación (`A...`) a reparación terminada (`R...`). Admin A intenta cambiar el técnico de esa fila y confirma.

**Lo que ve Admin A:**
> *"• A250410_3: ya no está pendiente (fue completada por otro usuario).*
> *Los datos se han recargado."*

La fila desaparece al recargar porque ya no existe como asignación activa.

---

### Caso C — Pedido modificado mientras se edita el formulario

Admin A abre el formulario de edición de un pedido. Mientras rellena campos, Admin B confirma la recepción de ese mismo pedido desde otra sesión, cambiando su estado a `RECIBIDO`.

Al guardar, la query detecta que `UPDATED_AT` cambió → `StaleDataException`.

**Lo que ve Admin A:**
> *"El pedido fue modificado por otro usuario. Cierra y recarga los datos."*

El formulario se cierra. La tabla de pedidos muestra el estado real.

---

### Caso D — Datos desactualizados sin conflicto activo (el más frecuente)

Un técnico lleva 10 minutos sin interactuar. Otro técnico cerró 3 reparaciones en ese tiempo. El primero sigue viendo la lista antigua.

No hay conflicto de escritura, pero la información es obsoleta. El sistema lo resuelve de forma completamente silenciosa:

- **Polling 60s:** la tabla se recarga sola. En el peor caso, los datos tienen 60 segundos de retraso.
- **Recarga al recuperar foco:** si el usuario vuelve desde otra ventana, los datos se actualizan al instante en el momento más natural.
- **Indicador HH:mm:** si el usuario duda de la frescura de los datos, tiene una referencia objetiva sin hacer nada.

---

### Caso E — Borrado de una asignación ya procesada

Admin A borra una asignación. Al mismo tiempo, Admin B acaba de modificar esa misma asignación desde otra sesión.

Aunque el borrado en BD tenga éxito, el `cargar()` posterior garantiza que la tabla refleja el estado real. No queda ninguna fila fantasma en pantalla.

---

## 4. Señales visuales al usuario

| Situación | Lo que ve el usuario |
|---|---|
| Conflicto al confirmar asignaciones | Alert con lista de motivos específicos + recarga automática |
| Conflicto al editar/confirmar pedido | Alert + cierre del formulario + recarga |
| Componente con stock modificado | Alert al guardar + recarga |
| Datos con más de 60s de antigüedad | Recarga silenciosa en background (sin interrupción) |
| Vuelta a la app tras ausencia | Recarga silenciosa al recuperar el foco |
| Duda sobre frescura de los datos | Label "Actualizado HH:mm" siempre visible bajo la tabla |
| Borrado de una fila | Recarga inmediata de la tabla al confirmar |

**Principio de diseño:** los mensajes están redactados en lenguaje de usuario, no técnico. No aparece ninguna mención a `UPDATED_AT`, timestamps ni excepciones. El sistema asume que el usuario no sabe que hay otros usuarios activos — simplemente le informa de que algo cambió y le muestra el estado actual para que pueda continuar. Las recargas silenciosas son completamente transparentes.

---

## 5. Impacto en rendimiento

### Coste actual (taller pequeño, 5–15 usuarios)

| Mecanismo | Coste |
|---|---|
| Polling 60s | 1 query ligera cada 60s por usuario activo — insignificante |
| Recarga al recuperar foco | 1 recarga puntual al volver a la app — imperceptible |
| `UPDATED_AT` en queries | 1 parámetro extra en el `WHERE` — sin coste práctico |
| Hilos de polling | 1 daemon thread por vista activa, se para al cambiar de vista |
| Mensajes de conflicto | 1 query adicional solo cuando hay conflicto real — rarísimo |

El enfoque es **optimista por diseño**: no hay locks de BD, no hay transacciones largas. El coste solo aparece en el caso de conflicto real.

---

### Escalabilidad — cuándo empezaría a ser un problema

#### Polling

Cada usuario activo lanza una query cada 60 segundos:

| Usuarios activos | Queries/minuto |
|---|---|
| 5 | ~5 |
| 20 | ~20 |
| 50 | ~50 |
| 100 | ~100 |

Hasta **~30–40 usuarios simultáneos** el impacto es nulo en MySQL con un servidor modesto. A partir de **~80–100** el polling acumulado empieza a competir con las operaciones reales. El cuello de botella no sería la query en sí sino las conexiones JDBC abiertas simultáneamente — JDBC sin pool de conexiones tiene un límite práctico de **~50–150 conexiones** antes de degradarse.

#### Recarga al recuperar foco

Si varios usuarios cambian de ventana al mismo tiempo (por ejemplo al final de una pausa), se produce un pico de queries simultáneas. Con **30+ usuarios** esto podría generar ráfagas de 20–30 queries en un segundo, provocando latencia perceptible (>500ms) en servidores compartidos o con poca RAM.

#### Conflictos de locking

El coste de `AND UPDATED_AT = ?` es mínimo. El problema real a escala no es el rendimiento sino la **frecuencia de conflictos**: con muchos admins asignando técnicos a la vez, los `StaleDataException` dejarían de ser rareza y se volverían frecuentes y frustrantes.

#### Umbrales estimados

| Escenario | Umbral | Problema principal |
|---|---|---|
| Polling empieza a notarse | ~80 usuarios simultáneos | Conexiones JDBC acumuladas |
| Picos de recarga al recuperar foco | ~30 usuarios actuando a la vez | Ráfagas de queries |
| Conflictos de locking frecuentes | ~15–20 admins editando simultáneamente | UX degradada |
| Sistema inestable sin cambios arquitectónicos | ~150+ usuarios totales | Todo lo anterior combinado |

Para el contexto real de un taller de reparaciones, estos números son muy altos. Se necesitaría una **cadena regional grande o un servicio técnico de fabricante** para llegar a estos límites.

---

### Qué habría que cambiar en ese escenario

- **Pool de conexiones (`HikariCP`):** reutilizar un pool de 10–20 conexiones para toda la app en lugar de abrir una conexión JDBC por operación. Es el cambio de mayor impacto con menor esfuerzo.
- **WebSockets o Server-Sent Events:** el servidor notifica a los clientes cuando hay cambios, eliminando completamente el coste del polling y los picos de foco. Requiere pasar a una arquitectura API REST + cliente.
- **Caché de lectura (`Redis`):** las queries de historial (las más pesadas) se podrían cachear con TTL corto (15–30s), reduciendo drásticamente las lecturas repetidas.
- **Locking pesimista selectivo:** bloquear la fila con `SELECT ... FOR UPDATE` durante la edición. Solo tiene sentido si los conflictos son tan frecuentes que el optimistic locking genera demasiados reintentos.

---

## 6. Qué se dejó fuera intencionalmente

| Mejora | Motivo |
|---|---|
| Locking pesimista | Solo útil con >50 admins editando simultáneamente |
| WebSockets / push del servidor | Requiere reescribir la arquitectura como API REST |
| Pool de conexiones JDBC | No necesario hasta ~80 usuarios concurrentes |
| Caché de lectura | Complejidad desproporcionada para el tamaño del taller |

Estas mejoras quedan documentadas aquí para una futura migración si el sistema escala significativamente.

---

## 7. Validaciones pendientes para el escenario post-migración (API REST + MariaDB)

Al migrar a una arquitectura API REST con múltiples clientes simultáneos, el optimistic locking actual no cubre todos los vectores de concurrencia. Las siguientes validaciones deberán implementarse:

---

### 7.1 Stock negativo

**Problema:** dos técnicos abren el formulario de reparación a la vez con el mismo componente con stock = 1. Ambos lo consumen. Sin protección en BD, el stock queda en -1.

**Solución:**
- Restricción `CHECK (STOCK >= 0)` en la tabla `Componente` de MariaDB.
- El endpoint de la API que descuenta stock atrapa el error de constraint y devuelve **409 Conflict**.
- El cliente JavaFX lo presenta al usuario: *"Stock insuficiente. Otro usuario agotó las existencias."*

---

### 7.2 Asignaciones duplicadas (mismo IMEI asignado dos veces)

**Problema:** dos admins asignan técnico al mismo IMEI al mismo tiempo. Sin control, se crean dos reparaciones activas para el mismo dispositivo.

**Solución (una de las dos, o combinadas):**
- Restricción `UNIQUE (IMEI)` en la tabla de asignaciones activas — el segundo INSERT falla a nivel de BD.
- O bien: el endpoint de asignación ejecuta `SELECT ... FOR UPDATE` + comprueba si ya existe una asignación activa para ese IMEI en la misma transacción, antes de insertar. Si existe, devuelve **409 Conflict**.

---

### 7.3 Recepción simultánea de pedidos

**Problema:** dos admins marcan el mismo pedido como `RECIBIDO` al mismo tiempo. El stock se incrementa dos veces.

**Problema adicional:** el `UPDATED_AT` ya cubre el pedido, pero el descuento/incremento de stock ocurre en una operación separada. Si el pedido y la actualización de stock no van en la misma transacción de BD, se puede romper la consistencia aunque el optimistic locking del pedido funcione.

**Solución:**
- El endpoint de recepción envuelve en una única transacción: `UPDATE Pedido SET estado = RECIBIDO ... AND UPDATED_AT = ?` + `UPDATE Componente SET STOCK = STOCK + cantidad`.
- Si el pedido ya fue recibido (UPDATED_AT cambió), rollback completo → **409 Conflict**.

---

### 7.4 Vistas desactualizadas en el cliente

**Problema:** con la migración a API REST, el polling actual (60s) sigue funcionando, pero el cliente podría tomar decisiones basadas en datos con hasta 60 segundos de antigüedad sobre tablas críticas (stock, asignaciones).

**Solución:**
- Reducir el intervalo de polling a **30 segundos** para `Componente` (stock) y `Reparacion` (asignaciones activas).
- A largo plazo: implementar **Server-Sent Events (SSE)** desde la API REST para notificaciones push. El servidor emite un evento cuando cambia stock o una asignación, eliminando el coste del polling y la latencia de 30–60s.

---

### 7.5 Expiración de sesión JWT

**Problema:** con JWT, la sesión tiene un tiempo de vida fijo (p.ej. 8 horas). Si expira mientras el usuario trabaja, las llamadas a la API devuelven **401 Unauthorized**. Sin manejo centralizado, cada controlador fallará silenciosamente o lanzará una excepción sin contexto.

**Solución:**
- Interceptor central en `ApiClient` (la clase que envuelve `HttpClient`): si cualquier respuesta es 401, ejecuta `Sesion.cerrar()` y navega a la pantalla de login mostrando: *"Tu sesión ha expirado. Inicia sesión de nuevo."*
- Esto garantiza que ningún controlador tenga que gestionar el 401 individualmente.

```java
// ApiClient.java — interceptor de respuesta
if (response.statusCode() == 401) {
    Platform.runLater(() -> {
        Sesion.cerrar();
        MainController.navegarA("LoginView.fxml");
        Alertas.info("Sesión expirada", "Tu sesión ha expirado. Inicia sesión de nuevo.");
    });
    return; // no procesar el resultado
}
```

---

### Resumen de validaciones pendientes

| Validación | Mecanismo | Capa |
|---|---|---|
| Stock negativo | `CHECK (STOCK >= 0)` + 409 en API | BD + API |
| Asignaciones duplicadas | `UNIQUE (IMEI)` o `SELECT FOR UPDATE` + 409 | BD + API |
| Recepción simultánea de pedidos | Transacción única pedido + stock + 409 | API |
| Vistas desactualizadas | Polling 30s + SSE a largo plazo | Cliente + API |
| Expiración JWT | Interceptor central en `ApiClient` | Cliente |