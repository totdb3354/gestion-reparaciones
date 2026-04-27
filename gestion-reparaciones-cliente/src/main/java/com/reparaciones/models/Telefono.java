package com.reparaciones.models;

/**
 * Teléfono identificado por su IMEI.
 * <p>Actúa como clave foránea en la tabla Reparacion; el IMEI es el
 * identificador único del dispositivo que entra a reparar.</p>
 */
public class Telefono {

    /** IMEI del dispositivo (15 dígitos numéricos). */
    private String imei;

    /**
     * @param imei IMEI del dispositivo
     */
    public Telefono(String imei) {
        this.imei = imei;
    }

    /** @return IMEI del dispositivo */
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
}
