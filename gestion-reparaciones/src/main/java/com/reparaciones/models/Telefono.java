package com.reparaciones.models;

public class Telefono {

    private String imei;

    public Telefono(String imei) {
        this.imei = imei;
    }

    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
}
