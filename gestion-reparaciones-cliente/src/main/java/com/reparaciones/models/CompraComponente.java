package com.reparaciones.models;

import java.time.LocalDateTime;

/**
 * Pedido de un componente a un proveedor.
 * <p>Incluye campos calculados por JOIN ({@code tipoComponente},
 * {@code nombreProveedor}) que se rellenan en consulta para evitar
 * consultas secundarias al mostrar la tabla de pedidos.</p>
 * <p>El precio se almacena en la divisa original del pedido
 * ({@code precioUnidadPedido} / {@code divisa}) y convertido
 * automáticamente a euros ({@code precioEur}) usando el tipo de
 * cambio vigente en la fecha del pedido.</p>
 */
public class CompraComponente {

    /**
     * Estado del ciclo de vida del pedido:
     * <ul>
     *   <li>{@code pendiente} — pedido realizado, aún sin recibir nada</li>
     *   <li>{@code parcial} — recibida una parte de la cantidad pedida</li>
     *   <li>{@code recibido} — recibida la totalidad</li>
     *   <li>{@code cancelado} — pedido anulado</li>
     * </ul>
     */
    public enum Estado { pendiente, recibido, parcial, cancelado }

    /** Clave primaria (ID_COMPRA en BD). */
    private int idCompra;

    /** ID del componente pedido. */
    private int idCom;

    /** Descripción del componente — obtenida por JOIN con {@code Componente}. */
    private String tipoComponente;

    /** ID del proveedor al que se realiza el pedido. */
    private int idProv;

    /** Nombre del proveedor — obtenido por JOIN con {@code Proveedor}. */
    private String nombreProveedor;

    /** Número de unidades pedidas. */
    private int cantidad;

    /**
     * Número de unidades ya recibidas, o {@code null} si no se ha recibido
     * ninguna todavía.
     */
    private Integer cantidadRecibida;

    /** {@code true} si el pedido se marcó como urgente. */
    private boolean esUrgente;

    /** Fecha y hora en que se registró el pedido. */
    private LocalDateTime fechaPedido;

    /**
     * Fecha y hora de recepción (total o parcial), o {@code null}
     * si el pedido sigue pendiente.
     */
    private LocalDateTime fechaLlegada;

    /** Precio por unidad en la divisa original del pedido. */
    private double precioUnidadPedido;

    /** Código ISO 4217 de la divisa del pedido (p. ej. {@code "EUR"}, {@code "CNY"}). */
    private String divisa;

    /**
     * Precio por unidad convertido a euros usando el tipo de cambio de la
     * fecha del pedido. Igual a {@code precioUnidadPedido} si {@code divisa="EUR"}.
     */
    private double precioEur;

    /** Estado actual del pedido. */
    private Estado estado;

    /** Última vez que se modificó el registro en BD. */
    private LocalDateTime updatedAt;

    /**
     * Constructor completo — llamado desde {@code CompraDAO}.
     *
     * @param idCompra            clave primaria
     * @param idCom               ID del componente pedido
     * @param tipoComponente      descripción del componente (JOIN)
     * @param idProv              ID del proveedor
     * @param nombreProveedor     nombre del proveedor (JOIN)
     * @param cantidad            unidades pedidas
     * @param cantidadRecibida    unidades recibidas, o {@code null}
     * @param esUrgente           {@code true} si el pedido es urgente
     * @param fechaPedido         fecha de registro del pedido
     * @param fechaLlegada        fecha de recepción, o {@code null}
     * @param precioUnidadPedido  precio por unidad en divisa original
     * @param divisa              código ISO 4217 de la divisa
     * @param precioEur           precio por unidad convertido a EUR
     * @param estado              estado del pedido
     * @param updatedAt           última actualización del registro
     */
    public CompraComponente(int idCompra, int idCom, String tipoComponente,
                             int idProv, String nombreProveedor,
                             int cantidad, Integer cantidadRecibida,
                             boolean esUrgente, LocalDateTime fechaPedido,
                             LocalDateTime fechaLlegada,
                             double precioUnidadPedido, String divisa, double precioEur,
                             Estado estado,
                             LocalDateTime updatedAt) {
        this.idCompra            = idCompra;
        this.idCom               = idCom;
        this.tipoComponente      = tipoComponente;
        this.idProv              = idProv;
        this.nombreProveedor     = nombreProveedor;
        this.cantidad            = cantidad;
        this.cantidadRecibida    = cantidadRecibida;
        this.esUrgente           = esUrgente;
        this.fechaPedido         = fechaPedido;
        this.fechaLlegada        = fechaLlegada;
        this.precioUnidadPedido  = precioUnidadPedido;
        this.divisa              = divisa;
        this.precioEur           = precioEur;
        this.estado              = estado;
        this.updatedAt           = updatedAt;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return clave primaria del pedido */
    public int getIdCompra() { return idCompra; }

    /** @return ID del componente pedido */
    public int getIdCom() { return idCom; }

    /** @return descripción del componente */
    public String getTipoComponente() { return tipoComponente; }

    /** @return ID del proveedor */
    public int getIdProv() { return idProv; }

    /** @return nombre del proveedor */
    public String getNombreProveedor() { return nombreProveedor; }

    /** @return número de unidades pedidas */
    public int getCantidad() { return cantidad; }

    /** @return unidades recibidas, o {@code null} si ninguna todavía */
    public Integer getCantidadRecibida() { return cantidadRecibida; }

    /** @return {@code true} si el pedido se marcó como urgente */
    public boolean isEsUrgente() { return esUrgente; }

    /** @return fecha y hora de registro del pedido */
    public LocalDateTime getFechaPedido() { return fechaPedido; }

    /** @return fecha de recepción, o {@code null} si sigue pendiente */
    public LocalDateTime getFechaLlegada() { return fechaLlegada; }

    /** @return precio por unidad en la divisa original */
    public double getPrecioUnidadPedido() { return precioUnidadPedido; }

    /** @return código ISO 4217 de la divisa del pedido */
    public String getDivisa() { return divisa; }

    /** @return precio por unidad convertido a EUR */
    public double getPrecioEur() { return precioEur; }

    /** @return estado actual del pedido */
    public Estado getEstado() { return estado; }

    /** @return última actualización del registro en BD */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setCantidad(int cantidad)                       { this.cantidad = cantidad; }
    public void setCantidadRecibida(Integer cantidadRecibida)   { this.cantidadRecibida = cantidadRecibida; }
    public void setEsUrgente(boolean esUrgente)                 { this.esUrgente = esUrgente; }
    public void setFechaLlegada(LocalDateTime fechaLlegada)     { this.fechaLlegada = fechaLlegada; }
    public void setPrecioUnidadPedido(double precio)            { this.precioUnidadPedido = precio; }
    public void setDivisa(String divisa)                        { this.divisa = divisa; }
    public void setPrecioEur(double precioEur)                  { this.precioEur = precioEur; }
    public void setEstado(Estado estado)                        { this.estado = estado; }
    public void setIdProv(int idProv)                           { this.idProv = idProv; }
    public void setNombreProveedor(String nombre)               { this.nombreProveedor = nombre; }

    // ── Calculados ────────────────────────────────────────────────────────────

    /**
     * Importe total del pedido en la divisa original.
     *
     * @return {@code precioUnidadPedido × cantidad}
     */
    public double getTotalPedido() { return precioUnidadPedido * cantidad; }

    /**
     * Importe total del pedido convertido a euros.
     *
     * @return {@code precioEur × cantidad}
     */
    public double getTotalEur() { return precioEur * cantidad; }
}
