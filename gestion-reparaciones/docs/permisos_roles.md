# Permisos por rol

> Arquitectura de roles actual (2 roles) y futura (3 roles tras la migración).

---

## Arquitectura actual — 2 roles

### TECNICO

#### Reparaciones — sus propias asignaciones
| Acción | Controlador | DAO / método |
|--------|-------------|--------------|
| Ver asignaciones propias | `PendientesTecnicoController.cargar()` | `ReparacionDAO.getAsignacionesPorTecnico(idTec)` |
| Ver solicitudes de pieza | `PendientesTecnicoController` | `ReparacionDAO.getSolicitudesPorAsignacion(idAsig)` |
| Abrir formulario de reparación | `FormularioReparacionController.abrir()` | — |
| Guardar reparación (nueva) | `FormularioReparacionController.guardar()` | `ReparacionDAO.insertar()` + `ComponenteDAO.actualizarStock()` |
| Editar reparación propia | `FormularioReparacionController.initEditar()` | `ReparacionDAO.editarReparacion()` |
| Ver historial propio | `ReparacionControllerTecnico` | `ReparacionDAO.getReparacionesPorTecnico(idTec)` |
| Ver historial de un IMEI | `ReparacionControllerTecnico.abrirHistorialImei()` | `ReparacionDAO.getResumenPorImei(imei)` |

#### Stock — solo lectura en formulario de reparación
| Acción | Controlador | DAO / método |
|--------|-------------|--------------|
| Leer componentes por tipo | `FormularioReparacionController.cargarFilas()` | `ComponenteDAO.getAgrupadosPorTipo()` |
| Leer chasis por color | `FormularioReparacionController` (FilaUI) | `ComponenteDAO.getChasisPorColor(color)` |

#### Estadísticas — solo datos propios
| Acción | Controlador | DAO / método |
|--------|-------------|--------------|
| Ver estadísticas de reparaciones | `EstadisticasController` | `ReparacionDAO.getEstadisticasPorTecnico(...)` (filtrado a idTec) |
| Navegar al historial desde gráfica | `EstadisticasController` (vértice propio) | `Navegable.navegarAReparaciones(desde, hasta, nombreTecnico)` |

---

### ADMIN

Incluye todo lo de TECNICO, más:

#### Reparaciones — gestión global
| Acción | Controlador | DAO / método |
|--------|-------------|--------------|
| Ver todas las asignaciones | `PendientesAdminController.cargar()` | `ReparacionDAO.getAsignaciones()` |
| Asignar reparación (nueva) | `PendientesAdminController.abrirFormularioAsignacion()` | `TelefonoDAO.insertar()` + `ReparacionDAO.insertarAsignacion()` |
| Reasignar técnico | `PendientesAdminController.confirmarCambiosTecnico()` | `ReparacionDAO.actualizarTecnico(idRep, idTec, updatedAt)` |
| Borrar asignación | `PendientesAdminController` (botón borrar) | `ReparacionDAO.eliminarAsignacion(idAsig)` |
| Borrar incidencia al borrar asignación | `PendientesAdminController` | `ReparacionDAO.borrarIncidenciaPorImei(imei)` |
| Ver historial completo (todos) | `ReparacionControllerAdmin` | `ReparacionDAO.getReparacionesResumen()` |
| Ver historial de un IMEI | `ReparacionControllerAdmin.abrirHistorialImei()` | `ReparacionDAO.getResumenPorImei(imei)` |
| Exportar historial CSV | `ReparacionControllerAdmin.exportarCSV()` | `CsvExporter.exportar()` |

#### Stock — gestión completa
| Acción | Controlador | DAO / método |
|--------|-------------|--------------|
| Ver stock con info de pedidos | `StockController.cargarStock()` | `ComponenteDAO.getAllGestionados()` |
| Editar stock manualmente | `StockController.editarStock()` | `ComponenteDAO.actualizar()` |
| Ajustar stock mínimo | `StockController.ajustarMinimo()` | `ComponenteDAO.setStockMinimo(idCom, valor)` |
| Activar / desactivar componente | `StockController.toggleActivarComponente()` | `ComponenteDAO.setActivo(idCom, activo)` |
| Insertar componente | `StockController` | `ComponenteDAO.insertar(componente)` |
| Eliminar componente | `StockController` | `ComponenteDAO.eliminar(idCom)` |
| Ver pedidos pendientes | `StockController.cargarPedidos()` | `CompraComponenteDAO.getPendientes()` |
| Crear pedido | `FormularioCompraController.confirmar()` | `CompraComponenteDAO.insertar(...)` |
| Editar pedido | `FormularioCompraEditarController.guardar()` | `CompraComponenteDAO.editar(...)` |
| Recibir pedido (completo) | `StockController` | `CompraComponenteDAO.confirmarRecibido()` + `ComponenteDAO.actualizarStock()` |
| Recibir pedido (parcial) | `StockController` | `CompraComponenteDAO.confirmarParcial()` + `ComponenteDAO.actualizarStock()` |
| Cancelar pedido | `StockController` | `CompraComponenteDAO.cancelar()` |
| Ver proveedores | `StockController.cargarProveedores()` | `ProveedorDAO.getAll()` |
| Añadir proveedor | `StockController` | `ProveedorDAO.insertar(nombre)` |
| Activar / desactivar proveedor | `StockController` | `ProveedorDAO.setActivo(idProv, activo)` |
| Cambiar divisa proveedor | `StockController.cambiarDivisa()` | `ProveedorDAO.setDivisa(idProv, divisa)` |
| Borrar proveedor | `StockController` | `ProveedorDAO.borrar(idProv)` |
| Exportar stock / pedidos / proveedores CSV | `StockController.exportarCSV()` | `CsvExporter.exportar()` |

#### Estadísticas — datos globales
| Acción | Controlador | DAO / método |
|--------|-------------|--------------|
| Ver estadísticas de todos los técnicos | `EstadisticasController` | `ReparacionDAO.getEstadisticasPorTecnico(...)` |
| Ver evolución de stock | `EstadisticasController` | `ComponenteDAO.getEvolucionStock(granularidad, desde, hasta)` |
| Navegar al historial desde cualquier vértice | `EstadisticasController` | `Navegable.navegarAReparaciones(desde, hasta, tecnico)` |

#### Gestión de usuarios *(pendiente — se implementa en la migración)*
| Acción | Controlador | DAO / método |
|--------|-------------|--------------|
| Ver usuarios | — | `UsuarioDAO.getAll()` *(por implementar)* |
| Crear técnico | — | `UsuarioDAO.registrarTecnico()` *(existe)* |
| Crear admin | — | `UsuarioDAO.registrarAdmin()` *(por implementar)* |
| Activar / desactivar usuario | — | `UsuarioDAO.activarTecnico()` / `desactivarTecnico()` *(existen)* |
| Asignar rol | — | `UsuarioDAO.setRol()` *(por implementar)* |

---

## Arquitectura futura — 3 roles *(tras la migración)*

> Separación entre administración del sistema y gestión operativa del taller.

### TECNICO
- ✅ Sus propias reparaciones y asignaciones
- ✅ Estadísticas propias
- ✅ Stock — solo lectura en formulario de reparación

### SUPERTECNICO
- ✅ Todo lo de TECNICO
- ✅ Todas las asignaciones (asignar, reasignar, borrar)
- ✅ Historial completo (todos los técnicos)
- ✅ Stock — gestión completa (edición, mínimos, activar/desactivar)
- ✅ Pedidos (crear, editar, recibir, cancelar)
- ✅ Proveedores (gestión completa)
- ✅ Estadísticas globales (todos los técnicos)
- ✅ Exportar CSV

### ADMIN
- ✅ Gestión de usuarios (crear, desactivar, asignar roles)
- ✅ Estadísticas globales — solo lectura
- ✅ Stock — solo consulta (sin editar, sin pedidos)
- ✅ Historial de reparaciones — solo lectura (sin asignar ni editar)
- ❌ Asignaciones, pedidos, proveedores
