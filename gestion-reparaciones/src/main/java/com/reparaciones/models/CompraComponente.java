package com.reparaciones.models;

import java.time.LocalDateTime;

public class CompraComponente {

    public enum Estado { pendiente, recibido, alterado, parcial, devuelto, cancelado }

    private int           idCompra;
    private int           idCom;
    private String        tipoComponente;   // join con Componente — para mostrar en tabla
    private int           idProv;
    private String        nombreProveedor;  // join con Proveedor — para mostrar en tabla
    private int           cantidad;
    private Integer       cantidadRecibida; // nullable
    private boolean       esUrgente;
    private LocalDateTime fechaPedido;
    private LocalDateTime fechaLlegada;     // nullable
    private double        precioUnidadPedido;
    private String        divisa;
    private double        precioEur;
    private Estado        estado;
    private LocalDateTime updatedAt;

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

    // ── Getters y setters ─────────────────────────────────────────────────────

    public int           getIdCompra()             { return idCompra; }
    public int           getIdCom()                { return idCom; }
    public String        getTipoComponente()        { return tipoComponente; }
    public int           getIdProv()               { return idProv; }
    public String        getNombreProveedor()       { return nombreProveedor; }
    public int           getCantidad()             { return cantidad; }
    public Integer       getCantidadRecibida()     { return cantidadRecibida; }
    public boolean       isEsUrgente()             { return esUrgente; }
    public LocalDateTime getFechaPedido()          { return fechaPedido; }
    public LocalDateTime getFechaLlegada()         { return fechaLlegada; }
    public double        getPrecioUnidadPedido()   { return precioUnidadPedido; }
    public String        getDivisa()               { return divisa; }
    public double        getPrecioEur()            { return precioEur; }
    public Estado        getEstado()               { return estado; }

    public void setCantidad(int cantidad)                      { this.cantidad = cantidad; }
    public void setCantidadRecibida(Integer cantidadRecibida)  { this.cantidadRecibida = cantidadRecibida; }
    public void setEsUrgente(boolean esUrgente)                { this.esUrgente = esUrgente; }
    public void setFechaLlegada(LocalDateTime fechaLlegada)    { this.fechaLlegada = fechaLlegada; }
    public void setPrecioUnidadPedido(double precio)           { this.precioUnidadPedido = precio; }
    public void setDivisa(String divisa)                       { this.divisa = divisa; }
    public void setPrecioEur(double precioEur)                 { this.precioEur = precioEur; }
    public void setEstado(Estado estado)                       { this.estado = estado; }
    public void setIdProv(int idProv)                          { this.idProv = idProv; }
    public void setNombreProveedor(String nombre)              { this.nombreProveedor = nombre; }
    public LocalDateTime getUpdatedAt()                        { return updatedAt; }

    // ── Calculados ────────────────────────────────────────────────────────────

    public double getTotalPedido()  { return precioUnidadPedido * cantidad; }
    public double getTotalEur()     { return precioEur * cantidad; }
}
