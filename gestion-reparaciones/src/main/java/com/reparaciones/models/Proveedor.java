package com.reparaciones.models;

public class Proveedor {

    private int     idProv;
    private String  nombre;
    private boolean activo;
    private int     numIncidencias;

    public Proveedor(int idProv, String nombre, boolean activo, int numIncidencias) {
        this.idProv         = idProv;
        this.nombre         = nombre;
        this.activo         = activo;
        this.numIncidencias = numIncidencias;
    }

    public int     getIdProv()         { return idProv; }
    public String  getNombre()         { return nombre; }
    public boolean isActivo()          { return activo; }
    public int     getNumIncidencias() { return numIncidencias; }

    public void setNombre(String nombre)   { this.nombre = nombre; }
    public void setActivo(boolean activo)  { this.activo = activo; }

    @Override
    public String toString() { return nombre; } // para ComboBox
}
