package com.reparaciones.models;

import java.time.LocalDateTime;

/**
 * Entidad principal de una reparación.
 * <p>Una reparación está pendiente mientras {@code fechaFin} sea {@code null}.
 * Al completarla, el técnico registra la fecha de finalización y el componente
 * utilizado queda vinculado mediante la tabla {@code Reparacion_componente}.</p>
 *
 * <p>El identificador {@code idRep} sigue el formato {@code REP-yyyyMMdd-NNN}
 * generado en BD al insertar.</p>
 */
public class Reparacion {

    /** Clave primaria con formato {@code REP-yyyyMMdd-NNN}. */
    private String idRep;

    /** Fecha y hora en que se asignó la reparación al técnico. */
    private LocalDateTime fechaAsig;

    /**
     * Fecha y hora de finalización, o {@code null} si la reparación
     * está todavía pendiente.
     */
    private LocalDateTime fechaFin;

    /** IMEI del teléfono que entra a reparar. */
    private String imei;

    /** ID del técnico asignado. */
    private int idTec;

    /** Última vez que se modificó el registro en BD. */
    private LocalDateTime updatedAt;

    /**
     * @param idRep      clave primaria
     * @param fechaAsig  fecha de asignación
     * @param fechaFin   fecha de finalización, o {@code null} si pendiente
     * @param imei       IMEI del dispositivo
     * @param idTec      ID del técnico asignado
     * @param updatedAt  última actualización del registro
     */
    public Reparacion(String idRep, LocalDateTime fechaAsig, LocalDateTime fechaFin,
                      String imei, int idTec, LocalDateTime updatedAt) {
        this.idRep     = idRep;
        this.fechaAsig = fechaAsig;
        this.fechaFin  = fechaFin;
        this.imei      = imei;
        this.idTec     = idTec;
        this.updatedAt = updatedAt;
    }

    /** @return clave primaria con formato {@code REP-yyyyMMdd-NNN} */
    public String getIdRep() { return idRep; }
    public void setIdRep(String idRep) { this.idRep = idRep; }

    /** @return fecha y hora de asignación */
    public LocalDateTime getFechaAsig() { return fechaAsig; }
    public void setFechaAsig(LocalDateTime fechaAsig) { this.fechaAsig = fechaAsig; }

    /** @return fecha de finalización, o {@code null} si está pendiente */
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    /** @return IMEI del dispositivo */
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }

    /** @return ID del técnico asignado */
    public int getIdTec() { return idTec; }
    public void setIdTec(int idTec) { this.idTec = idTec; }

    /** @return última actualización del registro en BD */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
