package com.reparaciones.dao;

import com.reparaciones.models.ReparacionComponente;
import com.reparaciones.models.SolicitudResumen;
import com.reparaciones.utils.ApiClient;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para la tabla {@code Reparacion_componente} vía API REST.
 * <p>Gestiona la relación entre reparaciones y los componentes utilizados en ellas,
 * incluyendo el ciclo de vida de las solicitudes de reposición de stock.</p>
 */
public class ReparacionComponenteDAO {

    /**
     * Devuelve todos los componentes registrados para una reparación.
     *
     * @param idRep identificador de la reparación ({@code R*} o {@code A*})
     * @return lista de componentes asociados, vacía si no hay ninguno
     * @throws SQLException si falla la llamada al servidor
     */
    public List<ReparacionComponente> getByReparacion(String idRep) throws SQLException {
        return ApiClient.getList("/api/reparacion-componentes/" + idRep, ReparacionComponente.class);
    }

    /**
     * Inserta un componente en la reparación y descuenta stock si corresponde.
     *
     * @param rc      datos del componente a insertar
     * @param cantidad unidades a descontar del stock
     * @throws SQLException si falla la llamada al servidor
     */
    public void insertar(ReparacionComponente rc, int cantidad) throws SQLException {
        Map<String, Object> body = new HashMap<>();
        body.put("idRep",                rc.getIdRep());
        body.put("idCom",                rc.getIdCom());
        body.put("esReutilizado",        rc.isEsReutilizado());
        body.put("esIncidencia",         rc.isEsIncidencia());
        body.put("esResuelto",           rc.isEsResuelto());
        body.put("incidencia",           rc.getIncidencia());
        body.put("observaciones",        rc.getObservaciones());
        body.put("esSolicitud",          rc.isEsSolicitud());
        body.put("descripcionSolicitud", rc.getDescripcionSolicitud());
        body.put("cantidad",             cantidad);
        ApiClient.post("/api/reparacion-componentes", body);
    }

    /**
     * Elimina un componente concreto de una reparación.
     *
     * @param idRep identificador de la reparación
     * @param idCom identificador del componente
     * @throws SQLException si falla la llamada al servidor
     */
    public void eliminar(String idRep, int idCom) throws SQLException {
        ApiClient.delete("/api/reparacion-componentes/" + idRep + "/" + idCom);
    }

    /**
     * Marca todos los componentes de una reparación como incidencia abierta.
     *
     * @param idRep      identificador de la reparación
     * @param comentario descripción de la incidencia
     * @throws SQLException si falla la llamada al servidor
     */
    public void marcarIncidencia(String idRep, String comentario) throws SQLException {
        ApiClient.patch("/api/reparacion-componentes/" + idRep + "/incidencia",
                Map.of("comentario", comentario));
    }

    /**
     * Cancela la incidencia abierta de una reparación.
     *
     * @param idRep identificador de la reparación
     * @throws SQLException si falla la llamada al servidor
     */
    public void borrarIncidencia(String idRep) throws SQLException {
        ApiClient.delete("/api/reparacion-componentes/" + idRep + "/incidencia");
    }

    /**
     * Cuenta las solicitudes de pieza con estado {@code PENDIENTE}.
     *
     * @return número de solicitudes pendientes
     * @throws SQLException si falla la llamada al servidor
     */
    public int contarSolicitudesPendientes() throws SQLException {
        return ApiClient.getInt("/api/solicitudes/count");
    }

    /**
     * Devuelve las solicitudes de pieza filtradas por estado.
     *
     * @param estado {@code "PENDIENTE"}, {@code "GESTIONADA"}, {@code "RECHAZADA"},
     *               o {@code null} para todas
     * @return lista de resúmenes ordenada por fecha descendente
     * @throws SQLException si falla la llamada al servidor
     */
    public List<SolicitudResumen> getSolicitudes(String estado) throws SQLException {
        String path = "/api/solicitudes" + (estado != null ? "?estado=" + estado : "");
        return ApiClient.getList(path, SolicitudResumen.class);
    }

    /**
     * Actualiza el estado de una solicitud de pieza.
     *
     * @param idRc   clave primaria del registro
     * @param estado nuevo estado: {@code "PENDIENTE"}, {@code "GESTIONADA"} o {@code "RECHAZADA"}
     * @throws SQLException si falla la llamada al servidor
     */
    public void actualizarEstadoSolicitud(int idRc, String estado) throws SQLException {
        ApiClient.patch("/api/solicitudes/" + idRc + "/estado", Map.of("estado", estado));
    }

    /**
     * Desactiva una solicitud rechazada eliminándola visualmente del panel de notificaciones.
     *
     * @param idRc clave primaria del registro
     * @throws SQLException si falla la llamada al servidor
     */
    public void limpiarSolicitud(int idRc) throws SQLException {
        ApiClient.patch("/api/solicitudes/" + idRc + "/limpiar", null);
    }
}
