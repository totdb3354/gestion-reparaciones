package com.reparaciones.models;

import java.time.LocalDate;

public class TipoCambio {

    private String    divisa;
    private LocalDate fecha;
    private double    tasa;

    public TipoCambio(String divisa, LocalDate fecha, double tasa) {
        this.divisa = divisa;
        this.fecha  = fecha;
        this.tasa   = tasa;
    }

    public String    getDivisa() { return divisa; }
    public LocalDate getFecha()  { return fecha; }
    public double    getTasa()   { return tasa; }
}
