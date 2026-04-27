package com.reparaciones.dao;

import com.reparaciones.utils.ApiClient;

import java.sql.SQLException;

/**
 * Acceso a tipos de cambio de divisas vía API REST.
 * <p>El servidor gestiona la caché diaria en BD y las llamadas a la API
 * externa Frankfurter. El cliente solo consulta la tasa ya resuelta.</p>
 *
 * @role ADMIN
 */
public class TipoCambioDAO {

    /**
     * Devuelve la tasa de conversión divisa→EUR para hoy.
     * <p>Para {@code "EUR"} devuelve {@code 1.0} sin llamada al servidor.</p>
     *
     * @param divisa código ISO 4217 de la divisa origen (p. ej. {@code "CNY"})
     * @return tasa de conversión hacia EUR
     * @throws SQLException si falla la llamada al servidor
     */
    public double getTasa(String divisa) throws SQLException {
        if ("EUR".equalsIgnoreCase(divisa)) return 1.0;
        return ApiClient.getDouble("/api/tipo-cambio/" + divisa);
    }
}
