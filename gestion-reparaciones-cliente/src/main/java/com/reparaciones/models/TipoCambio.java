package com.reparaciones.models;

import java.time.LocalDate;

/**
 * Tipo de cambio de una divisa extranjera frente al euro en una fecha concreta.
 * <p>Se usa para convertir el precio de compra de componentes pedidos
 * en divisas distintas al euro (p. ej. CNY, USD) al equivalente en EUR.</p>
 */
public class TipoCambio {

    /** Código ISO 4217 de la divisa (p. ej. {@code "CNY"}, {@code "USD"}). */
    private String divisa;

    /** Fecha a la que corresponde el tipo de cambio. */
    private LocalDate fecha;

    /**
     * Unidades de divisa por 1 EUR.
     * Ejemplo: {@code tasa = 7.5} significa 1 EUR = 7.5 CNY.
     */
    private double tasa;

    /**
     * @param divisa código ISO 4217 de la divisa
     * @param fecha  fecha del tipo de cambio
     * @param tasa   unidades de divisa por 1 EUR
     */
    public TipoCambio(String divisa, LocalDate fecha, double tasa) {
        this.divisa = divisa;
        this.fecha  = fecha;
        this.tasa   = tasa;
    }

    /** @return código ISO 4217 de la divisa */
    public String getDivisa() { return divisa; }

    /** @return fecha del tipo de cambio */
    public LocalDate getFecha() { return fecha; }

    /** @return unidades de divisa por 1 EUR */
    public double getTasa() { return tasa; }
}
