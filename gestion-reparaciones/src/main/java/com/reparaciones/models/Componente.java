package com.reparaciones.models;

import java.time.LocalDateTime;

/**
 * Componente de reparación gestionado en el stock.
 * <p>Además de los campos persistidos en BD, incluye {@code enCamino}
 * y {@code ultimoPedido} que se calculan en tiempo de consulta a partir
 * de los pedidos activos (estado {@code pendiente} o {@code parcial}).</p>
 */
public class Componente {

    /** Clave primaria (ID_COM en BD). */
    private int idCom;

    /** Tipo o descripción del componente (p. ej. "Pantalla iPhone 13"). */
    private String tipo;

    /** Fecha y hora en que se registró el componente por primera vez. */
    private LocalDateTime fechaRegistro;

    /** Unidades disponibles en almacén en este momento. */
    private int stock;

    /** Umbral mínimo de stock; por debajo se considera que hay rotura. */
    private int stockMinimo;

    /** {@code false} si el componente está desactivado y se excluye de conteos y alertas. */
    private boolean activo;

    /** Última vez que se actualizó el registro en BD. */
    private LocalDateTime updatedAt;

    /**
     * Unidades en camino: suma de cantidades pendientes de llegar
     * (pedidos en estado {@code pendiente} o {@code parcial}).
     * Se rellena en consulta, no se persiste como campo propio.
     */
    private int enCamino;

    /**
     * Fecha del pedido más reciente asociado a este componente, o {@code null}
     * si no hay pedidos. Se rellena en consulta, no se persiste como campo propio.
     */
    private LocalDateTime ultimoPedido;

    /**
     * @param idCom          clave primaria del componente
     * @param tipo           descripción del componente
     * @param fechaRegistro  fecha de alta en el sistema
     * @param stock          unidades disponibles en almacén
     * @param stockMinimo    umbral mínimo de stock
     * @param updatedAt      última actualización del registro
     */
    public Componente(int idCom, String tipo, LocalDateTime fechaRegistro,
                      int stock, int stockMinimo, boolean activo, LocalDateTime updatedAt) {
        this.idCom = idCom;
        this.tipo = tipo;
        this.fechaRegistro = fechaRegistro;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.activo = activo;
        this.updatedAt = updatedAt;
    }

    /** @return clave primaria del componente */
    public int getIdCom() { return idCom; }

    /** @return descripción del componente */
    public String getTipo() { return tipo; }

    /** @return fecha de alta en el sistema */
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }

    /** @return unidades disponibles en almacén */
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    /** @return umbral mínimo de stock */
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    /** @return última actualización del registro en BD */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** @return unidades en camino desde pedidos pendientes/parciales */
    public int getEnCamino() { return enCamino; }
    public void setEnCamino(int enCamino) { this.enCamino = enCamino; }

    /** @return fecha del pedido más reciente, o {@code null} si no hay pedidos */
    public LocalDateTime getUltimoPedido() { return ultimoPedido; }
    public void setUltimoPedido(LocalDateTime ultimoPedido) { this.ultimoPedido = ultimoPedido; }

    /** Devuelve el tipo para uso en ComboBox. */
    @Override
    public String toString() { return tipo; }
}
