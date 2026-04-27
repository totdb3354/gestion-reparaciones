package com.reparaciones.dao;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.reparaciones.models.FilaReparacion;
import com.reparaciones.models.PuntoEstadistica;
import com.reparaciones.models.Reparacion;
import com.reparaciones.models.ReparacionResumen;
import com.reparaciones.utils.ApiClient;
import com.reparaciones.utils.StaleDataException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Acceso a datos de la tabla {@code Reparacion} vía API REST.
 * <p>Gestiona el ciclo de vida completo de las reparaciones: asignaciones ({@code A*}),
 * finalizaciones ({@code R*}), ediciones, eliminaciones e incidencias.</p>
 *
 * @role ADMIN (operaciones completas); TECNICO (lectura y finalización de sus propias asignaciones)
 */
public class ReparacionDAO {

    /**
     * DTO inmutable con los datos de una reparación finalizada necesarios para
     * abrir el formulario de edición.
     */
    public static class DetalleEdicion {
        public final String imei;
        public final int    idTec;
        public final int    idCom;
        public final boolean esReutilizado;
        public final String  observacion;
        public DetalleEdicion(String imei, int idTec, int idCom,
                              boolean esReutilizado, String observacion) {
            this.imei = imei; this.idTec = idTec; this.idCom = idCom;
            this.esReutilizado = esReutilizado; this.observacion = observacion;
        }
    }

    // ── Lectura: historial ────────────────────────────────────────────────────

    /**
     * Devuelve todas las reparaciones finalizadas ({@code ID_REP LIKE 'R%'}) como resúmenes.
     *
     * @return lista de reparaciones ordenadas por ID
     * @throws SQLException si falla la llamada al servidor
     */
    public List<ReparacionResumen> getReparacionesResumen() throws SQLException {
        return ApiClient.getList("/api/reparaciones/historial", ReparacionResumen.class);
    }

    /**
     * Devuelve las reparaciones finalizadas de un técnico concreto.
     *
     * @param idTec ID del técnico
     * @return lista de reparaciones del técnico
     * @throws SQLException si falla la llamada al servidor
     */
    public List<ReparacionResumen> getReparacionesPorTecnico(int idTec) throws SQLException {
        return ApiClient.getList("/api/reparaciones/historial?tecnico=" + idTec, ReparacionResumen.class);
    }

    /**
     * Devuelve las reparaciones finalizadas de un IMEI concreto.
     *
     * @param imei IMEI del dispositivo
     * @return lista de reparaciones para ese IMEI
     * @throws SQLException si falla la llamada al servidor
     */
    public List<ReparacionResumen> getResumenPorImei(String imei) throws SQLException {
        return ApiClient.getList("/api/reparaciones/historial/imei/" + imei, ReparacionResumen.class);
    }

    // ── Lectura: asignaciones ─────────────────────────────────────────────────

    /**
     * Devuelve todas las asignaciones pendientes ({@code ID_REP LIKE 'A%'}).
     *
     * @return lista de asignaciones ordenadas por fecha ascendente
     * @throws SQLException si falla la llamada al servidor
     */
    public List<ReparacionResumen> getAsignaciones() throws SQLException {
        return ApiClient.getList("/api/reparaciones/asignaciones", ReparacionResumen.class);
    }

    /**
     * Busca una asignación por ID.
     *
     * @param idRep ID de la asignación
     * @return la asignación envuelta en {@code Optional}, o vacío si no existe
     * @throws SQLException si falla la llamada al servidor
     */
    public Optional<ReparacionResumen> getAsignacionById(String idRep) throws SQLException {
        try {
            ReparacionResumen r = ApiClient.get(
                    "/api/reparaciones/asignaciones/" + idRep, ReparacionResumen.class);
            return Optional.ofNullable(r);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Recurso no encontrado")) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Devuelve las asignaciones pendientes de un técnico concreto.
     *
     * @param idTec ID del técnico
     * @return lista de asignaciones del técnico
     * @throws SQLException si falla la llamada al servidor
     */
    public List<ReparacionResumen> getAsignacionesPorTecnico(int idTec) throws SQLException {
        return ApiClient.getList("/api/reparaciones/asignaciones?tecnico=" + idTec, ReparacionResumen.class);
    }

    /**
     * Devuelve las solicitudes de componente pendientes de una asignación.
     *
     * @param idAsignacion ID de la asignación ({@code A*})
     * @return lista de filas con ID de componente y descripción de solicitud
     * @throws SQLException si falla la llamada al servidor
     */
    public List<FilaReparacion> getSolicitudesPorAsignacion(String idAsignacion) throws SQLException {
        return ApiClient.getList(
                "/api/reparaciones/asignaciones/" + idAsignacion + "/solicitudes", FilaReparacion.class);
    }

    // ── Lectura: raw ──────────────────────────────────────────────────────────

    /**
     * Devuelve todas las reparaciones sin ningún filtro.
     *
     * @return lista completa de reparaciones
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Reparacion> getAll() throws SQLException {
        return ApiClient.getList("/api/reparaciones", Reparacion.class);
    }

    /**
     * Devuelve todas las reparaciones de un IMEI.
     *
     * @param imei IMEI del dispositivo
     * @return lista de reparaciones para ese IMEI
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Reparacion> getByImei(String imei) throws SQLException {
        return ApiClient.getList("/api/reparaciones/imei/" + imei, Reparacion.class);
    }

    /**
     * Cuenta el total de registros en {@code Reparacion} para un IMEI.
     *
     * @param imei IMEI del dispositivo
     * @return número de reparaciones con ese IMEI
     * @throws SQLException si falla la llamada al servidor
     */
    public int countByImei(String imei) throws SQLException {
        return ApiClient.getInt("/api/reparaciones/imei/" + imei + "/count");
    }

    // ── Lectura: auxiliares ───────────────────────────────────────────────────

    /**
     * Carga los datos de edición de una reparación finalizada.
     *
     * @param idRep ID de la reparación
     * @return datos para el formulario de edición, o {@code null} si no existe
     * @throws SQLException si falla la llamada al servidor
     */
    public DetalleEdicion getDetalleEdicion(String idRep) throws SQLException {
        return ApiClient.get("/api/reparaciones/" + idRep + "/detalle-edicion", DetalleEdicion.class);
    }

    /**
     * Busca si existe una reparación que tenga como {@code ID_REP_ANTERIOR} el ID dado.
     *
     * @param idRep ID de la reparación anterior
     * @return ID de la reparación que la referencia, o {@code null} si no existe
     * @throws SQLException si falla la llamada al servidor
     */
    public String getReferenciadora(String idRep) throws SQLException {
        return ApiClient.getString("/api/reparaciones/" + idRep + "/referenciadora");
    }

    /**
     * Devuelve los IDs de componentes ya usados en reparaciones de un IMEI.
     *
     * @param imei          IMEI del dispositivo
     * @param idRepExcluir  ID de la reparación en edición (se excluye)
     * @return conjunto de IDs de componentes ya utilizados para ese IMEI
     * @throws SQLException si falla la llamada al servidor
     */
    public Set<Integer> getIdComsYaReparados(String imei, String idRepExcluir) throws SQLException {
        String path = "/api/reparaciones/imei/" + imei + "/ya-reparados?excluir=" + idRepExcluir;
        return ApiClient.get(path, new TypeToken<Set<Integer>>(){}.getType());
    }

    /**
     * Busca si existe una incidencia activa (no resuelta) para el IMEI dado.
     *
     * @param imei IMEI del dispositivo
     * @return ID de la reparación con la incidencia activa, o {@code null} si no hay ninguna
     * @throws SQLException si falla la llamada al servidor
     */
    public String getIncidenciaActivaPorImei(String imei) throws SQLException {
        return ApiClient.getString("/api/reparaciones/imei/" + imei + "/incidencia-activa");
    }

    /**
     * Comprueba si ya existe una asignación pendiente del IMEI para el técnico dado.
     *
     * @param imei  IMEI del dispositivo
     * @param idTec ID del técnico
     * @return {@code true} si ya existe una asignación activa
     * @throws SQLException si falla la llamada al servidor
     */
    public boolean existeAsignacionParaTecnico(String imei, int idTec) throws SQLException {
        return ApiClient.getBoolean(
                "/api/reparaciones/imei/" + imei + "/tiene-asignacion?tecnico=" + idTec);
    }

    /**
     * Devuelve el conteo de reparaciones finalizadas agrupadas por técnico y periodo.
     *
     * @param granularidad {@code "dia"}, {@code "semana"}, {@code "mes"} o {@code "ano"}
     * @param desde        fecha de inicio del rango
     * @param hasta        fecha de fin del rango
     * @return lista de puntos de estadística ordenados por periodo y técnico
     * @throws SQLException si falla la llamada al servidor
     */
    public List<PuntoEstadistica> getEstadisticasPorTecnico(
            String granularidad, LocalDate desde, LocalDate hasta) throws SQLException {
        String path = "/api/reparaciones/estadisticas?granularidad=" + granularidad
                + "&desde=" + desde + "&hasta=" + hasta;
        return ApiClient.getList(path, PuntoEstadistica.class);
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    /**
     * Inserta una nueva reparación y devuelve el ID generado.
     *
     * @param r reparación a insertar
     * @return ID asignado con formato {@code R[yyyyMMdd]_N}
     * @throws SQLException si falla la llamada al servidor
     */
    public String insertar(Reparacion r) throws SQLException {
        Map<String, Object> body = new HashMap<>();
        body.put("imei",      r.getImei());
        body.put("idTec",     r.getIdTec());
        body.put("fechaAsig", r.getFechaAsig());
        body.put("fechaFin",  r.getFechaFin());
        JsonObject resp = ApiClient.post("/api/reparaciones", body, JsonObject.class);
        return resp != null ? resp.get("value").getAsString() : null;
    }

    /**
     * Crea una nueva asignación pendiente y devuelve su ID.
     *
     * @param imei  IMEI del dispositivo
     * @param idTec ID del técnico asignado
     * @return ID de la asignación con formato {@code A[yyyyMMdd]_N}
     * @throws SQLException si falla la llamada al servidor
     */
    public String insertarAsignacion(String imei, int idTec) throws SQLException {
        JsonObject resp = ApiClient.post("/api/reparaciones/asignaciones",
                Map.of("imei", imei, "idTec", idTec), JsonObject.class);
        return resp != null ? resp.get("value").getAsString() : null;
    }

    /**
     * Finaliza una reparación (o varias) a partir de las filas del formulario.
     *
     * @param filas         filas del formulario (normales y/o solicitudes)
     * @param imei          IMEI del dispositivo
     * @param idTec         ID del técnico que finaliza
     * @param idRepAnterior ID de la reparación anterior si es reincidencia, o {@code null}
     * @param idAsignacion  ID de la asignación origen ({@code A*}), o {@code null}
     * @throws SQLException si falla la llamada al servidor
     */
    public void insertarCompleta(List<FilaReparacion> filas, String imei, int idTec,
            String idRepAnterior, String idAsignacion) throws SQLException {
        Map<String, Object> body = new HashMap<>();
        body.put("filas",         filas);
        body.put("imei",          imei);
        body.put("idTec",         idTec);
        body.put("idRepAnterior", idRepAnterior);
        body.put("idAsignacion",  idAsignacion);
        ApiClient.post("/api/reparaciones/completa", body);
    }

    /**
     * Registra la fecha de finalización de una reparación.
     *
     * @param idRep ID de la reparación a completar
     * @throws SQLException si falla la llamada al servidor
     */
    public void completar(String idRep) throws SQLException {
        ApiClient.patch("/api/reparaciones/" + idRep + "/completar", null);
    }

    /**
     * Reasigna una asignación a otro técnico con control de concurrencia optimista.
     *
     * @param idRep     ID de la asignación
     * @param idTec     nuevo ID del técnico
     * @param updatedAt timestamp leído en la última carga
     * @throws SQLException       si falla la llamada al servidor
     * @throws StaleDataException si otro usuario modificó la asignación
     */
    public void actualizarTecnico(String idRep, int idTec, LocalDateTime updatedAt)
            throws SQLException, StaleDataException {
        ApiClient.patch("/api/reparaciones/asignaciones/" + idRep + "/tecnico",
                Map.of("idTec", idTec, "updatedAt", updatedAt));
    }

    /**
     * Edita el componente y observaciones de una reparación finalizada.
     *
     * @param idRep              ID de la reparación a editar
     * @param idComNuevo         nuevo ID del componente
     * @param esReutilizadoNuevo {@code true} si la nueva pieza no resta stock
     * @param observacionNueva   nuevas notas del técnico
     * @param piezaViejaRota     {@code true} si la pieza anterior no se devuelve al stock
     * @param nNuevas            unidades de la nueva pieza a descontar
     * @throws SQLException si falla la llamada al servidor
     */
    public void editarReparacion(String idRep, int idComNuevo, boolean esReutilizadoNuevo,
            String observacionNueva, boolean piezaViejaRota, int nNuevas) throws SQLException {
        Map<String, Object> body = new HashMap<>();
        body.put("idComNuevo",         idComNuevo);
        body.put("esReutilizadoNuevo", esReutilizadoNuevo);
        body.put("observacionNueva",   observacionNueva);
        body.put("piezaViejaRota",     piezaViejaRota);
        body.put("nNuevas",            nNuevas);
        ApiClient.put("/api/reparaciones/" + idRep, body);
    }

    /**
     * Marca una incidencia en la reparación y crea automáticamente una nueva asignación.
     *
     * @param idRep      ID de la reparación con la incidencia
     * @param comentario descripción de la incidencia
     * @param imei       IMEI del dispositivo
     * @param idTec      ID del técnico al que se reasigna
     * @throws SQLException si falla la llamada al servidor
     */
    public void marcarIncidenciaYAsignar(String idRep, String comentario,
            String imei, int idTec) throws SQLException {
        ApiClient.post("/api/reparaciones/" + idRep + "/incidencia",
                Map.of("comentario", comentario, "imei", imei, "idTec", idTec));
    }

    /**
     * Cancela la incidencia activa (no resuelta) de un IMEI.
     *
     * @param imei IMEI del dispositivo
     * @throws SQLException si falla la llamada al servidor
     */
    public void borrarIncidenciaPorImei(String imei) throws SQLException {
        ApiClient.delete("/api/reparaciones/imei/" + imei + "/incidencia-activa");
    }

    /**
     * Elimina una asignación pendiente y, si era el último registro del IMEI,
     * también elimina el {@code Telefono} asociado.
     *
     * @param idAsig ID de la asignación a eliminar
     * @throws SQLException si falla la llamada al servidor
     */
    public void eliminarAsignacion(String idAsig) throws SQLException {
        ApiClient.delete("/api/reparaciones/asignaciones/" + idAsig);
    }

    /**
     * Elimina una reparación finalizada y realiza todas las compensaciones necesarias.
     *
     * @param idRep ID de la reparación a eliminar
     * @throws SQLException si falla la llamada al servidor
     */
    public void eliminar(String idRep) throws SQLException {
        ApiClient.delete("/api/reparaciones/" + idRep);
    }
}
