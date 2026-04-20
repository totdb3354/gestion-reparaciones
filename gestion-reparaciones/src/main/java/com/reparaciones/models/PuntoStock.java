package com.reparaciones.models;

/**
 * Punto de datos para el gráfico de evolución de stock.
 * <p>Representa el nivel de stock estimado de un componente al final de un periodo.
 * Se reconstruye a partir del stock actual retrocediendo por entradas
 * ({@code Compra_componente}) y salidas ({@code Reparacion_componente}) históricas.</p>
 *
 * @see com.reparaciones.dao.ComponenteDAO
 */
public class PuntoStock {

    /** Periodo formateado según la granularidad activa (mismo formato que {@code PuntoEstadistica}). */
    private final String periodo;

    /** Tipo del componente (TIPO en BD). */
    private final String tipoComponente;

    /** Stock estimado al final del periodo. */
    private final int stockEstimado;

    /** Umbral mínimo configurado para este componente. */
    private final int stockMinimo;

    /**
     * @param periodo        periodo formateado
     * @param tipoComponente tipo del componente
     * @param stockEstimado  stock estimado al final del periodo
     * @param stockMinimo    umbral mínimo del componente
     */
    public PuntoStock(String periodo, String tipoComponente, int stockEstimado, int stockMinimo) {
        this.periodo        = periodo;
        this.tipoComponente = tipoComponente;
        this.stockEstimado  = stockEstimado;
        this.stockMinimo    = stockMinimo;
    }

    /** @return periodo formateado */
    public String getPeriodo()        { return periodo; }

    /** @return tipo del componente */
    public String getTipoComponente() { return tipoComponente; }

    /** @return stock estimado al final del periodo */
    public int getStockEstimado()     { return stockEstimado; }

    /** @return umbral mínimo del componente */
    public int getStockMinimo()       { return stockMinimo; }
}
