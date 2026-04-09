package com.reparaciones.controllers;

import com.reparaciones.dao.CompraComponenteDAO;
import com.reparaciones.dao.ComponenteDAO;
import com.reparaciones.dao.ProveedorDAO;
import com.reparaciones.dao.TipoCambioDAO;
import com.reparaciones.models.CompraComponente;
import com.reparaciones.models.Componente;
import com.reparaciones.models.Proveedor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class FormularioCompraController {

    @FXML private Label      lblTitulo;
    @FXML private ComboBox<Componente> cmbComponente;
    @FXML private ComboBox<Proveedor>  cmbProveedor;
    @FXML private TextField  txtCantidad;
    @FXML private CheckBox   chkUrgente;
    @FXML private TextField  txtPrecio;
    @FXML private ComboBox<String> cmbDivisa;
    @FXML private Label      lblTotalEur;

    private final ComponenteDAO     componenteDAO = new ComponenteDAO();
    private final ProveedorDAO      proveedorDAO  = new ProveedorDAO();
    private final TipoCambioDAO     tipoCambioDAO = new TipoCambioDAO();
    private final CompraComponenteDAO compraDAO   = new CompraComponenteDAO();

    private Runnable        onGuardado;
    private CompraComponente pedidoEditar; // null = nuevo

    private volatile double tasaActual = 1.0;

    // ─── Init ─────────────────────────────────────────────────────────────────

    public void init(Componente preselect, CompraComponente editar, Runnable onGuardado) {
        this.pedidoEditar = editar;
        this.onGuardado   = onGuardado;

        // Cargar combos
        try {
            cmbComponente.getItems().setAll(componenteDAO.getAll());
            cmbProveedor .getItems().setAll(proveedorDAO.getActivos());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cmbDivisa.getItems().setAll(List.of("EUR", "USD"));

        // Listeners (antes de setear valores para que calcularTotal se dispare)
        txtPrecio   .textProperty().addListener((obs, o, n) -> calcularTotal());
        txtCantidad .textProperty().addListener((obs, o, n) -> calcularTotal());
        cmbDivisa.valueProperty().addListener((obs, o, n) -> { if (n != null) fetchTasaAsync(n); });

        // Valores iniciales
        if (editar != null) {
            lblTitulo.setText("Editar pedido #" + editar.getIdCompra());
            cmbComponente.setDisable(true);
            cmbComponente.getItems().stream()
                    .filter(c -> c.getIdCom() == editar.getIdCom())
                    .findFirst().ifPresent(cmbComponente::setValue);
            cmbProveedor.getItems().stream()
                    .filter(p -> p.getIdProv() == editar.getIdProv())
                    .findFirst().ifPresent(cmbProveedor::setValue);
            txtCantidad.setText(String.valueOf(editar.getCantidad()));
            chkUrgente.setSelected(editar.isEsUrgente());
            txtPrecio.setText(String.format("%.2f", editar.getPrecioUnidadPedido()));
            cmbDivisa.setValue(editar.getDivisa()); // dispara fetchTasaAsync
        } else {
            cmbDivisa.setValue("EUR"); // dispara calcularTotal con tasa 1.0
            if (preselect != null) cmbComponente.setValue(preselect);
        }
    }

    // ─── Cálculo EUR en vivo ──────────────────────────────────────────────────

    private void fetchTasaAsync(String divisa) {
        if ("EUR".equalsIgnoreCase(divisa)) {
            tasaActual = 1.0;
            calcularTotal();
            return;
        }
        lblTotalEur.setText("Obteniendo tasa…");
        new Thread(() -> {
            try {
                double t = tipoCambioDAO.getTasa(divisa);
                Platform.runLater(() -> { tasaActual = t; calcularTotal(); });
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> lblTotalEur.setText("Error al obtener tasa"));
            }
        }, "tasa-fetch").start();
    }

    private void calcularTotal() {
        String divisa = cmbDivisa.getValue() != null ? cmbDivisa.getValue() : "EUR";
        boolean esEur = "EUR".equalsIgnoreCase(divisa);
        String infoTasa = esEur ? "" : String.format("  (1 %s = %.4f €)", divisa, tasaActual);
        try {
            double precio = Double.parseDouble(txtPrecio.getText().trim().replace(",", "."));
            int    cant   = Integer.parseInt(txtCantidad.getText().trim());
            double total  = precio * tasaActual * cant;
            lblTotalEur.setText(String.format("%.2f €%s", total, infoTasa));
        } catch (NumberFormatException e) {
            lblTotalEur.setText(esEur ? "—" : infoTasa.trim());
        }
    }

    // ─── Acciones ────────────────────────────────────────────────────────────

    @FXML
    private void confirmar() {
        Componente comp = cmbComponente.getValue();
        Proveedor  prov = cmbProveedor.getValue();
        if (comp == null || prov == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona componente y proveedor.").showAndWait();
            return;
        }
        int cant;
        try {
            cant = Integer.parseInt(txtCantidad.getText().trim());
            if (cant <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Cantidad no válida (debe ser > 0).").showAndWait();
            return;
        }
        double precio;
        try {
            precio = Double.parseDouble(txtPrecio.getText().trim().replace(",", "."));
            if (precio < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Precio no válido.").showAndWait();
            return;
        }
        String divisa = cmbDivisa.getValue();

        try {
            // Obtener tasa final (puede ir a API si no está en caché)
            double tasa     = tipoCambioDAO.getTasa(divisa);
            double precioEur = precio * tasa;

            if (pedidoEditar == null) {
                compraDAO.insertar(comp.getIdCom(), prov.getIdProv(), cant,
                        chkUrgente.isSelected(), precio, divisa, precioEur);
            } else {
                compraDAO.editar(pedidoEditar.getIdCompra(), prov.getIdProv(), cant,
                        chkUrgente.isSelected(), precio, divisa, precioEur);
            }
            onGuardado.run();
            cerrarVentana();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        ((Stage) cmbComponente.getScene().getWindow()).close();
    }

    // ─── Apertura estática ────────────────────────────────────────────────────

    public static void abrir(Componente preselect, Runnable onGuardado) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        FormularioCompraController.class.getResource("/views/FormularioCompraView.fxml"));
                Parent root = loader.load();
                FormularioCompraController ctrl = loader.getController();

                Stage stage = new Stage();
                stage.setTitle(preselect == null
                        ? "Nuevo pedido"
                        : "Pedir: " + preselect.getTipo());
                stage.setScene(new Scene(root));
                stage.setResizable(false);
                stage.initModality(Modality.APPLICATION_MODAL);

                ctrl.init(preselect, null, onGuardado);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void abrirEditar(CompraComponente pedido, Runnable onGuardado) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        FormularioCompraController.class.getResource("/views/FormularioCompraView.fxml"));
                Parent root = loader.load();
                FormularioCompraController ctrl = loader.getController();

                Stage stage = new Stage();
                stage.setTitle("Editar pedido #" + pedido.getIdCompra());
                stage.setScene(new Scene(root));
                stage.setResizable(false);
                stage.initModality(Modality.APPLICATION_MODAL);

                ctrl.init(null, pedido, onGuardado);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
