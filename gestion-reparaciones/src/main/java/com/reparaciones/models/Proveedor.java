package com.reparaciones.models;

public class Proveedor {

    private int     idProv;
    private String  nombre;
    private boolean activo;

    public Proveedor(int idProv, String nombre, boolean activo) {
        this.idProv  = idProv;
        this.nombre  = nombre;
        this.activo  = activo;
    }

    public int     getIdProv()  { return idProv; }
    public String  getNombre()  { return nombre; }
    public boolean isActivo()   { return activo; }

    public void setNombre(String nombre)   { this.nombre = nombre; }
    public void setActivo(boolean activo)  { this.activo = activo; }

    @Override
    public String toString() { return nombre; } // para ComboBox
}
