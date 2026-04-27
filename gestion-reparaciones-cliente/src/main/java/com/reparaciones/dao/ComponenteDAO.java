package com.reparaciones.dao;

import com.google.gson.reflect.TypeToken;
import com.reparaciones.models.Componente;
import com.reparaciones.models.PuntoStock;
import com.reparaciones.utils.ApiClient;
import com.reparaciones.utils.StaleDataException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de la tabla {@code Componente} vía API REST.
 * <p>Gestiona el stock de componentes de reparación. Los componentes de tipo
 * {@code "otro*"} no se gestionan desde el programa: se excluyen de las consultas
 * de stock pero siguen referenciables en reparaciones.</p>
 *
 * @role ADMIN (gestión completa); TECNICO (lectura de stock en formulario de reparación)
 */
public class ComponenteDAO {

    /**
     * Devuelve todos los componentes sin ningún filtro.
     *
     * @return lista completa de componentes
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Componente> getAll() throws SQLException {
        return ApiClient.getList("/api/componentes", Componente.class);
    }

    /**
     * Devuelve los componentes gestionados (excluye tipos {@code "otro*"}) con
     * información calculada de unidades en camino y último pedido.
     *
     * @return lista de componentes gestionados
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Componente> getAllGestionados() throws SQLException {
        return ApiClient.getList("/api/componentes/gestionados", Componente.class);
    }

    /**
     * Devuelve los componentes cuyo stock está en o por debajo del mínimo.
     *
     * @return lista de componentes con stock bajo
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Componente> getStockBajo() throws SQLException {
        return ApiClient.getList("/api/componentes/stock-bajo", Componente.class);
    }

    /**
     * Devuelve los chasis disponibles (stock {@literal >} 0) del color dado.
     *
     * @param color sufijo de color del chasis (p. ej. {@code "negro"}, {@code "blanco"})
     * @return lista de chasis con stock disponible del color indicado
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Componente> getChasisPorColor(String color) throws SQLException {
        return ApiClient.getList("/api/componentes/chasis?color=" + color, Componente.class);
    }

    /**
     * Devuelve todos los componentes agrupados por prefijo de tipo.
     *
     * @return mapa ordenado: prefijo → lista de componentes
     * @throws SQLException si falla la llamada al servidor
     */
    public java.util.Map<String, List<Componente>> getAgrupadosPorTipo() throws SQLException {
        return ApiClient.get("/api/componentes/agrupados",
                new TypeToken<java.util.Map<String, List<Componente>>>(){}.getType());
    }

    /**
     * Devuelve la evolución de stock por componente y periodo.
     *
     * @param granularidad {@code "dia"}, {@code "semana"}, {@code "mes"} o {@code "ano"}
     * @param desde        inicio del rango de fechas
     * @param hasta        fin del rango de fechas
     * @return lista de puntos ordenados por componente y periodo
     * @throws SQLException si falla la llamada al servidor
     */
    public List<PuntoStock> getEvolucionStock(String granularidad,
                                               LocalDate desde,
                                               LocalDate hasta) throws SQLException {
        String path = "/api/componentes/evolucion-stock?granularidad=" + granularidad
                + "&desde=" + desde + "&hasta=" + hasta;
        return ApiClient.getList(path, PuntoStock.class);
    }

    /**
     * Inserta un nuevo componente.
     *
     * @param c componente a insertar
     * @throws SQLException si falla la llamada al servidor
     */
    public void insertar(Componente c) throws SQLException {
        ApiClient.post("/api/componentes", Map.of(
                "tipo",        c.getTipo(),
                "stock",       c.getStock(),
                "stockMinimo", c.getStockMinimo()));
    }

    /**
     * Actualiza tipo, stock y stock mínimo con control de concurrencia optimista.
     *
     * @param c componente con los nuevos valores y el {@code updatedAt} leído previamente
     * @throws SQLException       si falla la llamada al servidor
     * @throws StaleDataException si el registro fue modificado por otro usuario
     */
    public void actualizar(Componente c) throws SQLException, StaleDataException {
        ApiClient.put("/api/componentes/" + c.getIdCom(), Map.of(
                "tipo",        c.getTipo(),
                "stock",       c.getStock(),
                "stockMinimo", c.getStockMinimo(),
                "updatedAt",   c.getUpdatedAt()));
    }

    /**
     * Actualiza únicamente el stock mínimo de un componente.
     *
     * @param idCom       ID del componente
     * @param stockMinimo nuevo valor del umbral mínimo
     * @throws SQLException si falla la llamada al servidor
     */
    public void setStockMinimo(int idCom, int stockMinimo) throws SQLException {
        ApiClient.patch("/api/componentes/" + idCom + "/stock-minimo",
                Map.of("stockMinimo", stockMinimo));
    }

    /**
     * Incrementa (o decrementa si negativo) el stock de un componente.
     *
     * @param idCom   ID del componente
     * @param cantidad delta a aplicar (positivo suma, negativo resta)
     * @throws SQLException si falla la llamada al servidor
     */
    public void actualizarStock(int idCom, int cantidad) throws SQLException {
        ApiClient.patch("/api/componentes/" + idCom + "/stock", Map.of("delta", cantidad));
    }

    /**
     * Activa o desactiva un componente.
     *
     * @param idCom  ID del componente
     * @param activo {@code true} para activar, {@code false} para desactivar
     * @throws SQLException si falla la llamada al servidor
     */
    public void setActivo(int idCom, boolean activo) throws SQLException {
        ApiClient.patch("/api/componentes/" + idCom + "/activo", Map.of("activo", activo));
    }

    /**
     * Elimina físicamente el componente.
     *
     * @param idCom ID del componente a eliminar
     * @throws SQLException si falla la llamada al servidor
     */
    public void eliminar(int idCom) throws SQLException {
        ApiClient.delete("/api/componentes/" + idCom);
    }
}
