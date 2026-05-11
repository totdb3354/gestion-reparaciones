package com.reparaciones.dao;

import com.reparaciones.models.CompraComponente;
import com.reparaciones.utils.ApiClient;
import com.reparaciones.utils.StaleDataException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de la tabla {@code Compra_componente} vía API REST.
 * <p>Gestiona el ciclo de vida completo de los pedidos de componentes:
 * creación, edición, recepción total/parcial y cancelación.</p>
 * <p>Las operaciones de edición y confirmación usan control de concurrencia optimista
 * ({@link StaleDataException}) comparando {@code UPDATED_AT}.</p>
 *
 * @role ADMIN
 */
public class CompraComponenteDAO {

    /**
     * Devuelve todos los pedidos ordenados por fecha descendente.
     *
     * @return lista completa de pedidos
     * @throws SQLException si falla la llamada al servidor
     */
    public List<CompraComponente> getAll() throws SQLException {
        return ApiClient.getList("/api/compras", CompraComponente.class);
    }

    /**
     * Devuelve los pedidos en estado {@code pendiente}.
     *
     * @return lista de pedidos pendientes (urgentes primero, luego por fecha ascendente)
     * @throws SQLException si falla la llamada al servidor
     */
    public List<CompraComponente> getPendientes() throws SQLException {
        return ApiClient.getList("/api/compras/pendientes", CompraComponente.class);
    }

    /**
     * Suma las unidades pendientes de recibir para un componente concreto.
     *
     * @param idCom ID del componente
     * @return total de unidades en estado {@code pendiente} para este componente
     * @throws SQLException si falla la llamada al servidor
     */
    public int getCantidadPendientePorComponente(int idCom) throws SQLException {
        return ApiClient.getInt("/api/compras/cantidad-pendiente/" + idCom);
    }

    /**
     * Inserta un nuevo pedido en estado {@code pendiente}.
     *
     * @param idCom        ID del componente pedido
     * @param idProv       ID del proveedor
     * @param cantidad     número de unidades a pedir
     * @param esUrgente    {@code true} si el pedido es urgente
     * @param precioUnidad precio por unidad en la divisa elegida
     * @param divisa       código ISO 4217 de la divisa
     * @param precioEur    precio por unidad convertido a EUR
     * @throws SQLException si falla la llamada al servidor
     */
    public void insertar(int idCom, int idProv, int cantidad, boolean esUrgente,
                         double precioUnidad, String divisa, double precioEur) throws SQLException {
        ApiClient.post("/api/compras", Map.of(
                "idCom",        idCom,
                "idProv",       idProv,
                "cantidad",     cantidad,
                "esUrgente",    esUrgente,
                "precioUnidad", precioUnidad,
                "divisa",       divisa,
                "precioEur",    precioEur));
    }

    /**
     * Edita los campos principales de un pedido con control de concurrencia optimista.
     *
     * @param pedido       pedido original con {@code updatedAt} para la comparación optimista
     * @param idProv       nuevo proveedor
     * @param cantidad     nueva cantidad
     * @param esUrgente    nuevo flag de urgencia
     * @param precioUnidad nuevo precio por unidad
     * @param divisa       nuevo código de divisa
     * @param precioEur    nuevo precio en EUR
     * @throws SQLException       si falla la llamada al servidor
     * @throws StaleDataException si otro usuario modificó el pedido antes que este
     */
    public void editar(CompraComponente pedido, int idProv, int cantidad, boolean esUrgente,
                       double precioUnidad, String divisa, double precioEur)
            throws SQLException, StaleDataException {
        ApiClient.put("/api/compras/" + pedido.getIdCompra(), Map.of(
                "idProv",       idProv,
                "cantidad",     cantidad,
                "esUrgente",    esUrgente,
                "precioUnidad", precioUnidad,
                "divisa",       divisa,
                "precioEur",    precioEur,
                "updatedAt",    pedido.getUpdatedAt()));
    }

    /**
     * Marca el pedido como {@code recibido} e incrementa el stock con toda la cantidad pedida.
     *
     * @param pedido pedido a confirmar
     * @throws SQLException       si falla la llamada al servidor
     * @throws StaleDataException si otro usuario modificó el pedido antes que este
     */
    public void confirmarRecibido(CompraComponente pedido) throws SQLException, StaleDataException {
        ApiClient.patch("/api/compras/" + pedido.getIdCompra() + "/confirmar-recibido",
                Map.of("updatedAt", pedido.getUpdatedAt()));
    }

    /**
     * Registra la llegada parcial de un pedido y cambia el estado a {@code parcial}.
     *
     * @param pedido           pedido del que llega la parte
     * @param cantidadRecibida unidades que han llegado en esta entrega
     * @throws SQLException       si falla la llamada al servidor
     * @throws StaleDataException si otro usuario modificó el pedido antes que este
     */
    public void confirmarParcial(CompraComponente pedido, int cantidadRecibida)
            throws SQLException, StaleDataException {
        ApiClient.patch("/api/compras/" + pedido.getIdCompra() + "/confirmar-parcial", Map.of(
                "cantidadRecibida", cantidadRecibida,
                "updatedAt",        pedido.getUpdatedAt()));
    }

    /**
     * Añade más unidades a un pedido {@code parcial}.
     *
     * @param pedido        pedido parcial al que llegan más unidades
     * @param cantidadExtra número de unidades adicionales recibidas
     * @throws SQLException       si falla la llamada al servidor
     * @throws StaleDataException si otro usuario modificó el pedido antes que este
     */
    public void recibirResto(CompraComponente pedido, int cantidadExtra)
            throws SQLException, StaleDataException {
        ApiClient.patch("/api/compras/" + pedido.getIdCompra() + "/recibir-resto", Map.of(
                "cantidadExtra", cantidadExtra,
                "updatedAt",     pedido.getUpdatedAt()));
    }

    /**
     * Cierra un pedido {@code parcial} marcándolo como {@code recibido}.
     *
     * @param pedido pedido parcial a cerrar
     * @throws SQLException       si falla la llamada al servidor
     * @throws StaleDataException si otro usuario modificó el pedido antes que este
     */
    public void confirmarAlterado(CompraComponente pedido) throws SQLException, StaleDataException {
        ApiClient.patch("/api/compras/" + pedido.getIdCompra() + "/confirmar-alterado",
                Map.of("updatedAt", pedido.getUpdatedAt()));
    }

    /**
     * Cancela un pedido en estado {@code pendiente}.
     *
     * @param pedido pedido a cancelar
     * @throws SQLException       si falla la llamada al servidor
     * @throws StaleDataException si otro usuario modificó el pedido antes que este
     */
    public void cancelar(CompraComponente pedido) throws SQLException, StaleDataException {
        ApiClient.patch("/api/compras/" + pedido.getIdCompra() + "/cancelar",
                Map.of("updatedAt", pedido.getUpdatedAt()));
    }

    /**
     * Revierte un pedido {@code recibido} a {@code pendiente} y descuenta el stock añadido.
     * El servidor rechaza la operación si el stock actual es insuficiente.
     *
     * @param pedido pedido a revertir
     * @throws SQLException       si falla la llamada al servidor
     * @throws StaleDataException si otro usuario modificó el pedido antes que este
     */
    public void desrecibir(CompraComponente pedido) throws SQLException, StaleDataException {
        ApiClient.patch("/api/compras/" + pedido.getIdCompra() + "/desrecibir",
                Map.of("updatedAt", pedido.getUpdatedAt()));
    }
}
