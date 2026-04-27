# Contrato REST API — gestion-reparaciones-servidor

Mapeo completo de los métodos DAO del cliente JavaFX a los endpoints del backend Spring Boot.

**Base URL:** `/api`  
**Auth:** JWT en header `Authorization: Bearer <token>`, excepto `POST /api/auth/login`.  
**Concurrencia optimista:** los endpoints que la usan reciben `updatedAt` en el body y devuelven `409 Conflict` si hay conflicto.  
Los IDs de reparación (`R*`, `A*`) los genera el servidor internamente — no se exponen como endpoints.

---

## Auth

| Método | URL | Body | DAO |
|--------|-----|------|-----|
| `POST` | `/api/auth/login` | `{usuario, password}` | `UsuarioDAO.login` |

Respuesta: `{idUsu, nombreUsuario, rol, idTec, token}`.

---

## Usuarios / Técnicos — gestión de cuentas (solo ADMIN)

| Método | URL | Body / Params | DAO |
|--------|-----|---------------|-----|
| `GET` | `/api/usuarios/tecnicos` | — | `UsuarioDAO.getUsuariosTecnicos` |
| `POST` | `/api/usuarios/tecnicos` | `{nombreTecnico, nombreUsuario, password}` | `UsuarioDAO.registrarTecnico` |
| `PATCH` | `/api/usuarios/tecnicos/{idTec}/activar` | — | `UsuarioDAO.activarTecnico` |
| `PATCH` | `/api/usuarios/tecnicos/{idTec}/desactivar` | — | `UsuarioDAO.desactivarTecnico` |
| `GET` | `/api/usuarios/tecnicos/{idTec}/tiene-reparaciones` | — | `UsuarioDAO.tieneReparaciones` → `{result: bool}` |
| `DELETE` | `/api/usuarios/tecnicos/{idTec}?idUsu={idUsu}` | — | `UsuarioDAO.eliminarTecnico` |

---

## Técnicos — listas para dropdowns (ADMIN y TECNICO)

| Método | URL | Body / Params | DAO |
|--------|-----|---------------|-----|
| `GET` | `/api/tecnicos` | — | `TecnicoDAO.getAll` |
| `GET` | `/api/tecnicos/activos` | — | `TecnicoDAO.getAllActivos` |

---

## Componentes

| Método | URL | Body / Params | DAO |
|--------|-----|---------------|-----|
| `GET` | `/api/componentes` | — | `ComponenteDAO.getAll` |
| `GET` | `/api/componentes/gestionados` | — | `ComponenteDAO.getAllGestionados` |
| `GET` | `/api/componentes/stock-bajo` | — | `ComponenteDAO.getStockBajo` |
| `GET` | `/api/componentes/agrupados` | — | `ComponenteDAO.getAgrupadosPorTipo` |
| `GET` | `/api/componentes/chasis?color={color}` | — | `ComponenteDAO.getChasisPorColor` |
| `GET` | `/api/componentes/evolucion-stock?granularidad=mes&desde=2026-01-01&hasta=2026-04-30` | — | `ComponenteDAO.getEvolucionStock` |
| `POST` | `/api/componentes` | `{tipo, stock, stockMinimo}` | `ComponenteDAO.insertar` |
| `PUT` | `/api/componentes/{idCom}` | `{tipo, stock, stockMinimo, updatedAt}` | `ComponenteDAO.actualizar` (optimista) |
| `PATCH` | `/api/componentes/{idCom}/stock-minimo` | `{stockMinimo}` | `ComponenteDAO.setStockMinimo` |
| `PATCH` | `/api/componentes/{idCom}/stock` | `{delta}` | `ComponenteDAO.actualizarStock` |
| `PATCH` | `/api/componentes/{idCom}/activo` | `{activo}` | `ComponenteDAO.setActivo` |
| `DELETE` | `/api/componentes/{idCom}` | — | `ComponenteDAO.eliminar` |

---

## Proveedores

| Método | URL | Body / Params | DAO |
|--------|-----|---------------|-----|
| `GET` | `/api/proveedores` | — | `ProveedorDAO.getAll` |
| `GET` | `/api/proveedores/activos` | — | `ProveedorDAO.getActivos` |
| `GET` | `/api/proveedores/{idProv}/tiene-pedidos` | — | `ProveedorDAO.tienePedidos` → `{result: bool}` |
| `POST` | `/api/proveedores` | `{nombre}` | `ProveedorDAO.insertar` |
| `PATCH` | `/api/proveedores/{idProv}/activo` | `{activo}` | `ProveedorDAO.setActivo` |
| `PATCH` | `/api/proveedores/{idProv}/divisa` | `{divisa}` | `ProveedorDAO.setDivisa` |
| `DELETE` | `/api/proveedores/{idProv}` | — | `ProveedorDAO.borrar` |

---

## Teléfonos

| Método | URL | Body / Params | DAO |
|--------|-----|---------------|-----|
| `GET` | `/api/telefonos` | — | `TelefonoDAO.getAll` |
| `GET` | `/api/telefonos/{imei}/exists` | — | `TelefonoDAO.exists` → `{result: bool}` |
| `POST` | `/api/telefonos` | `{imei}` | `TelefonoDAO.insertar` |
| `DELETE` | `/api/telefonos/{imei}` | — | `TelefonoDAO.eliminar` |

---

## Tipo de Cambio

| Método | URL | Body / Params | DAO |
|--------|-----|---------------|-----|
| `GET` | `/api/tipo-cambio/{divisa}` | — | `TipoCambioDAO.getTasa` → `{tasa: double}` |

El servidor hace la llamada a la API Frankfurter y cachea el resultado en BD. El cliente nunca llama a la API externa directamente.

---

## Pedidos (Compras de componentes)

| Método | URL | Body / Params | DAO |
|--------|-----|---------------|-----|
| `GET` | `/api/compras` | — | `CompraComponenteDAO.getAll` |
| `GET` | `/api/compras/pendientes` | — | `CompraComponenteDAO.getPendientes` |
| `GET` | `/api/compras/cantidad-pendiente/{idCom}` | — | `CompraComponenteDAO.getCantidadPendientePorComponente` → `{cantidad: int}` |
| `POST` | `/api/compras` | `{idCom, idProv, cantidad, esUrgente, precioUnidad, divisa, precioEur}` | `CompraComponenteDAO.insertar` |
| `PUT` | `/api/compras/{idCompra}` | `{idProv, cantidad, esUrgente, precioUnidad, divisa, precioEur, updatedAt}` | `CompraComponenteDAO.editar` (optimista) |
| `PATCH` | `/api/compras/{idCompra}/confirmar-recibido` | `{updatedAt}` | `CompraComponenteDAO.confirmarRecibido` |
| `PATCH` | `/api/compras/{idCompra}/confirmar-parcial` | `{cantidadRecibida, updatedAt}` | `CompraComponenteDAO.confirmarParcial` |
| `PATCH` | `/api/compras/{idCompra}/recibir-resto` | `{cantidadExtra, updatedAt}` | `CompraComponenteDAO.recibirResto` |
| `PATCH` | `/api/compras/{idCompra}/confirmar-alterado` | `{updatedAt}` | `CompraComponenteDAO.confirmarAlterado` |
| `PATCH` | `/api/compras/{idCompra}/cancelar` | `{updatedAt}` | `CompraComponenteDAO.cancelar` |

---

## Reparaciones

| Método | URL | Body / Params | DAO |
|--------|-----|---------------|-----|
| `GET` | `/api/reparaciones/historial` | — | `ReparacionDAO.getReparacionesResumen` |
| `GET` | `/api/reparaciones/historial?tecnico={idTec}` | — | `ReparacionDAO.getReparacionesPorTecnico` |
| `GET` | `/api/reparaciones/historial/imei/{imei}` | — | `ReparacionDAO.getResumenPorImei` |
| `GET` | `/api/reparaciones/asignaciones` | — | `ReparacionDAO.getAsignaciones` |
| `GET` | `/api/reparaciones/asignaciones/{idRep}` | — | `ReparacionDAO.getAsignacionById` |
| `GET` | `/api/reparaciones/asignaciones?tecnico={idTec}` | — | `ReparacionDAO.getAsignacionesPorTecnico` |
| `GET` | `/api/reparaciones/asignaciones/{idRep}/solicitudes` | — | `ReparacionDAO.getSolicitudesPorAsignacion` |
| `GET` | `/api/reparaciones/imei/{imei}` | — | `ReparacionDAO.getByImei` |
| `GET` | `/api/reparaciones/imei/{imei}/count` | — | `ReparacionDAO.countByImei` → `{count: int}` |
| `GET` | `/api/reparaciones/imei/{imei}/incidencia-activa` | — | `ReparacionDAO.getIncidenciaActivaPorImei` → `{idRep}` o `null` |
| `GET` | `/api/reparaciones/imei/{imei}/tiene-asignacion?tecnico={idTec}` | — | `ReparacionDAO.existeAsignacionParaTecnico` → `{result: bool}` |
| `GET` | `/api/reparaciones/imei/{imei}/ya-reparados?excluir={idRep}` | — | `ReparacionDAO.getIdComsYaReparados` → `[idCom, ...]` |
| `GET` | `/api/reparaciones/{idRep}/detalle-edicion` | — | `ReparacionDAO.getDetalleEdicion` |
| `GET` | `/api/reparaciones/{idRep}/referenciadora` | — | `ReparacionDAO.getReferenciadora` → `{idRep}` o `null` |
| `GET` | `/api/reparaciones/estadisticas?granularidad=mes&desde=...&hasta=...` | — | `ReparacionDAO.getEstadisticasPorTecnico` |
| `POST` | `/api/reparaciones/asignaciones` | `{imei, idTec}` | `ReparacionDAO.insertarAsignacion` → `{idRep}` |
| `POST` | `/api/reparaciones/completa` | `{filas, imei, idTec, idRepAnterior, idAsignacion}` | `ReparacionDAO.insertarCompleta` |
| `PATCH` | `/api/reparaciones/{idRep}/completar` | — | `ReparacionDAO.completar` |
| `PATCH` | `/api/reparaciones/asignaciones/{idRep}/tecnico` | `{idTec, updatedAt}` | `ReparacionDAO.actualizarTecnico` (optimista) |
| `PUT` | `/api/reparaciones/{idRep}` | `{idComNuevo, esReutilizadoNuevo, observacionNueva, piezaViejaRota, nNuevas}` | `ReparacionDAO.editarReparacion` |
| `POST` | `/api/reparaciones/{idRep}/incidencia` | `{comentario, imei, idTec}` | `ReparacionDAO.marcarIncidenciaYAsignar` |
| `DELETE` | `/api/reparaciones/imei/{imei}/incidencia-activa` | — | `ReparacionDAO.borrarIncidenciaPorImei` |
| `DELETE` | `/api/reparaciones/asignaciones/{idAsig}` | — | `ReparacionDAO.eliminarAsignacion` |
| `DELETE` | `/api/reparaciones/{idRep}` | — | `ReparacionDAO.eliminar` |

---

## Solicitudes de stock (Reparacion_componente)

| Método | URL | Body / Params | DAO |
|--------|-----|---------------|-----|
| `GET` | `/api/solicitudes/count` | — | `ReparacionComponenteDAO.contarSolicitudesPendientes` → `{count: int}` |
| `GET` | `/api/solicitudes?estado=PENDIENTE` | — | `ReparacionComponenteDAO.getSolicitudes` |
| `PATCH` | `/api/solicitudes/{idRc}/estado` | `{estado}` | `ReparacionComponenteDAO.actualizarEstadoSolicitud` |
| `PATCH` | `/api/solicitudes/{idRc}/limpiar` | — | `ReparacionComponenteDAO.limpiarSolicitud` |
| `GET` | `/api/reparacion-componentes/{idRep}` | — | `ReparacionComponenteDAO.getByReparacion` |
| `POST` | `/api/reparacion-componentes` | `{idRep, idCom, esReutilizado, esIncidencia, esResuelto, incidencia, observaciones, esSolicitud, descripcionSolicitud, cantidad}` | `ReparacionComponenteDAO.insertar` |
| `DELETE` | `/api/reparacion-componentes/{idRep}/{idCom}` | — | `ReparacionComponenteDAO.eliminar` |
| `PATCH` | `/api/reparacion-componentes/{idRep}/incidencia` | `{comentario}` | `ReparacionComponenteDAO.marcarIncidencia` |
| `DELETE` | `/api/reparacion-componentes/{idRep}/incidencia` | — | `ReparacionComponenteDAO.borrarIncidencia` |

---

## Resumen

| Recurso | Endpoints |
|---------|-----------|
| Auth | 1 |
| Usuarios/Técnicos (gestión) | 6 |
| Técnicos (listas) | 2 |
| Componentes | 12 |
| Proveedores | 7 |
| Teléfonos | 4 |
| Tipo de Cambio | 1 |
| Pedidos | 10 |
| Reparaciones | 25 |
| Solicitudes / Reparacion_componente | 9 |
| **Total** | **77** |
