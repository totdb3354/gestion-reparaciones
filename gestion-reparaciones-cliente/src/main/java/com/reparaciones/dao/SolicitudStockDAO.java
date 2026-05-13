package com.reparaciones.dao;

import com.reparaciones.models.SolicitudStock;
import com.reparaciones.utils.ApiClient;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolicitudStockDAO {

    public List<SolicitudStock> getSolicitudes(String estado) throws SQLException {
        String path = "/api/solicitudes-stock" + (estado != null ? "?estado=" + estado : "");
        return ApiClient.getList(path, SolicitudStock.class);
    }

    public int contarPendientes() throws SQLException {
        return ApiClient.getInt("/api/solicitudes-stock/count");
    }

    public void insertar(int idCom, String descripcion) throws SQLException {
        Map<String, Object> body = new HashMap<>();
        body.put("idCom", idCom);
        body.put("descripcion", descripcion);
        ApiClient.post("/api/solicitudes-stock", body);
    }

    public void actualizarEstado(int idSol, String estado) throws SQLException {
        ApiClient.patch("/api/solicitudes-stock/" + idSol + "/estado",
                Map.of("estado", estado));
    }

    public void borrar(int idSol) throws SQLException {
        ApiClient.delete("/api/solicitudes-stock/" + idSol);
    }
}
