package com.reparaciones.models;

import java.time.LocalDateTime;

public class Componente {

    private int idCom;
    private String tipo;
    private LocalDateTime fechaRegistro;
    private int stock;
    private int stockMinimo;

    public Componente(int idCom, String tipo, LocalDateTime fechaRegistro,
                      int stock, int stockMinimo) {
        this.idCom = idCom;
        this.tipo = tipo;
        this.fechaRegistro = fechaRegistro;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
    }

    public int getIdCom() { return idCom; }
    public String getTipo() { return tipo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    @Override
    public String toString() { return tipo; }
}