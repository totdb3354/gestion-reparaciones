package com.reparaciones.models;

import java.time.LocalDateTime;

public class Componente {

    private int idCom;
    private String tipo;
    private LocalDateTime fechaRegistro;
    private int stock;
    private int stockMinimo;
    private LocalDateTime updatedAt;
    private int enCamino;                  // unidades en pedidos pendientes/parciales
    private LocalDateTime ultimoPedido;    // fecha del último pedido, nullable

    public Componente(int idCom, String tipo, LocalDateTime fechaRegistro,
                      int stock, int stockMinimo, LocalDateTime updatedAt) {
        this.idCom = idCom;
        this.tipo = tipo;
        this.fechaRegistro = fechaRegistro;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.updatedAt = updatedAt;
    }

    public int getIdCom() { return idCom; }
    public String getTipo() { return tipo; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getEnCamino() { return enCamino; }
    public void setEnCamino(int enCamino) { this.enCamino = enCamino; }
    public LocalDateTime getUltimoPedido() { return ultimoPedido; }
    public void setUltimoPedido(LocalDateTime ultimoPedido) { this.ultimoPedido = ultimoPedido; }

    @Override
    public String toString() { return tipo; }
}
