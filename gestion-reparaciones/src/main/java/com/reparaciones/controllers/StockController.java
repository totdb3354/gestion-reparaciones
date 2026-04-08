package com.reparaciones.controllers;

import com.reparaciones.dao.CompraComponenteDAO;
import com.reparaciones.dao.ComponenteDAO;
import com.reparaciones.dao.ProveedorDAO;
import com.reparaciones.models.CompraComponente;
import com.reparaciones.models.CompraComponente.Estado;
import com.reparaciones.models.Componente;
import com.reparaciones.models.Proveedor;
import com.reparaciones.utils.ConfirmDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class StockController {

    // ── Sidebar ──────────────────────────────────────────────────────────────
    @FXML private Button btnTabStock;
    @FXML private Button btnTabPedidos;
    @FXML private Button btnTabProveedores;

    // ── Panels ────────────────────────────────────────────────────────────────
    @FXML private VBox pnlStock;
    @FXML private VBox pnlPedidos;
    @FXML private VBox pnlProveedores;

    // ── Panel Stock actual ────────────────────────────────────────────────────
    @FXML private TableView<Componente>             tablaStock;
    @FXML private TableColumn<Componente, String>   colTipo;
    @FXML private TableColumn<Componente, Integer>  colStockVal;
    @FXML private TableColumn<Componente, String>   colEstado;
    @FXML private TableColumn<Componente, Void>     colAccion;
    @FXML private TextField                         txtBuscador;
    @FXML private PieChart                          chartEstado;

    // ── Panel Pedidos ─────────────────────────────────────────────────────────
    @FXML private TableView<CompraComponente>             tablaPedidos;
    @FXML private TableColumn<CompraComponente, Integer>  cpId;
    @FXML private TableColumn<CompraComponente, String>   cpComponente;
    @FXML private TableColumn<CompraComponente, String>   cpProveedor;
    @FXML private TableColumn<CompraComponente, Integer>  cpCantidad;
    @FXML private TableColumn<CompraComponente, String>   cpUrgente;
    @FXML private TableColumn<CompraComponente, String>   cpFecha;
    @FXML private TableColumn<CompraComponente, String>   cpPrecio;
    @FXML private TableColumn<CompraComponente, String>   cpDivisa;
    @FXML private TableColumn<CompraComponente, String>   cpEur;
    @FXML private TableColumn<CompraComponente, String>   cpEstado;
    @FXML private ComboBox<String>                        cmbFiltroEstado;
    @FXML private Button btnConfirmarRecibido;
    @FXML private Button btnConfirmarAlterado;
    @FXML private Button btnEditarPedido;
    @FXML private Button btnCancelarPedido;
    @FXML private Button btnDevolverPedido;

    // ── Panel Proveedores ─────────────────────────────────────────────────────
    @FXML private TableView<Proveedor>             tablaProveedores;
    @FXML private TableColumn<Proveedor, String>   cpvNombre;
    @FXML private TableColumn<Proveedor, String>   cpvActivo;
    @FXML private Button btnActivarProveedor;
    @FXML private Button btnBorrarProveedor;

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final ComponenteDAO     componenteDAO = new ComponenteDAO();
    private final CompraComponenteDAO compraDAO   = new CompraComponenteDAO();
    private final ProveedorDAO      proveedorDAO  = new ProveedorDAO();

    // ── Observable lists ──────────────────────────────────────────────────────
    private final ObservableList<Componente>      datosStock      = FXCollections.observableArrayList();
    private final ObservableList<CompraComponente> datosPedidos   = FXCollections.observableArrayList();
    private final ObservableList<Proveedor>        datosProveedores = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    // ─── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configurarTablaStock();
        configurarTablaPedidos();
        configurarTablaProveedores();
        cargarStock();
    }

    // ─── Sidebar ──────────────────────────────────────────────────────────────

    @FXML private void mostrarTabStock() {
        mostrarPanel(pnlStock, btnTabStock);
        cargarStock();
    }

    @FXML private void mostrarTabPedidos() {
        mostrarPanel(pnlPedidos, btnTabPedidos);
        cargarPedidos();
    }

    @FXML private void mostrarTabProveedores() {
        mostrarPanel(pnlProveedores, btnTabProveedores);
        cargarProveedores();
    }

    private void mostrarPanel(VBox panel, Button btnActivo) {
        pnlStock.setVisible(false);       pnlStock.setManaged(false);
        pnlPedidos.setVisible(false);     pnlPedidos.setManaged(false);
        pnlProveedores.setVisible(false); pnlProveedores.setManaged(false);
        panel.setVisible(true);           panel.setManaged(true);
        setSidebarActivo(btnActivo);
    }

    private void setSidebarActivo(Button activo) {
        for (Button b : new Button[]{btnTabStock, btnTabPedidos, btnTabProveedores}) {
            b.getStyleClass().removeAll("stock-sidebar-btn-active", "stock-sidebar-btn");
            b.getStyleClass().add(b == activo ? "stock-sidebar-btn-active" : "stock-sidebar-btn");
        }
    }

    // ─── Configurar tabla Stock ───────────────────────────────────────────────

    private void configurarTablaStock() {
        colTipo.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getTipo()));
        colStockVal.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(c.getValue().getStock()).asObject());

        // Estado semáforo
        colEstado.setCellValueFactory(c -> {
            Componente comp = c.getValue();
            if (comp.getStock() == 0)                     return sp("Sin stock");
            if (comp.getStock() <= comp.getStockMinimo()) return sp("Bajo");
            return sp("OK");
        });
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                setStyle(switch (val) {
                    case "OK"   -> "-fx-text-fill:#3a7d44; -fx-font-weight:bold;";
                    case "Bajo" -> "-fx-text-fill:#c77a00; -fx-font-weight:bold;";
                    default     -> "-fx-text-fill:" + com.reparaciones.utils.Colores.ROJO_SIN_STOCK + "; -fx-font-weight:bold;";
                });
            }
        });

        // Botones "Pedir" + "Mín." (solo admin)
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnPedir = new Button("Pedir");
            private final Button btnMin   = new Button("Mín.");
            private final javafx.scene.layout.HBox box;
            {
                btnPedir.setStyle("-fx-font-size:11px; -fx-padding:4 10 4 10;" +
                        "-fx-background-color:#2C3B54; -fx-text-fill:white;" +
                        "-fx-background-radius:4; -fx-cursor:hand;");
                btnMin.setStyle("-fx-font-size:11px; -fx-padding:4 8 4 8;" +
                        "-fx-background-color:#586376; -fx-text-fill:white;" +
                        "-fx-background-radius:4; -fx-cursor:hand;");
                btnPedir.setOnAction(e -> pedirComponente(getTableView().getItems().get(getIndex())));
                btnMin  .setOnAction(e -> ajustarMinimo(getTableView().getItems().get(getIndex())));
                box = new javafx.scene.layout.HBox(4, btnPedir, btnMin);
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty || !com.reparaciones.Sesion.esAdmin() ? null : box);
            }
        });

        // Filtro buscador
        FilteredList<Componente> filtrada = new FilteredList<>(datosStock, c -> true);
        txtBuscador.textProperty().addListener((obs, old, text) ->
                filtrada.setPredicate(c -> text.isBlank() ||
                        c.getTipo().toLowerCase().contains(text.toLowerCase().trim())));
        tablaStock.setItems(filtrada);
    }

    private void cargarStock() {
        try {
            datosStock.setAll(componenteDAO.getAllGestionados());
            actualizarChart();
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    private void actualizarChart() {
        long ok       = datosStock.stream().filter(c -> c.getStock() > c.getStockMinimo()).count();
        long bajo     = datosStock.stream().filter(c -> c.getStock() > 0 && c.getStock() <= c.getStockMinimo()).count();
        long sinStock = datosStock.stream().filter(c -> c.getStock() == 0).count();

        chartEstado.getData().setAll(
                new PieChart.Data("OK (" + ok + ")",             ok),
                new PieChart.Data("Bajo (" + bajo + ")",         bajo),
                new PieChart.Data("Sin stock (" + sinStock + ")", sinStock)
        );

        // CHART_COLOR_N afecta tanto el sector como el símbolo de la leyenda
        // El orden coincide con el orden de inserción: 1=OK, 2=Bajo, 3=Sin stock
        chartEstado.setStyle(
                "CHART_COLOR_1: #3a7d44; " +
                "CHART_COLOR_2: #c77a00; " +
                "CHART_COLOR_3: " + com.reparaciones.utils.Colores.ROJO_SIN_STOCK + ";"
        );
    }

    private void pedirComponente(Componente c) {
        FormularioCompraController.abrir(c, () -> {
            cargarStock();
            if (pnlPedidos.isVisible()) cargarPedidos();
        });
    }

    private void ajustarMinimo(Componente c) {
        TextInputDialog d = new TextInputDialog(String.valueOf(c.getStockMinimo()));
        d.setTitle("Stock mínimo");
        d.setHeaderText(c.getTipo());
        d.setContentText("Nuevo stock mínimo:");
        d.showAndWait().ifPresent(val -> {
            try {
                int min = Integer.parseInt(val.trim());
                if (min < 0) throw new NumberFormatException();
                componenteDAO.setStockMinimo(c.getIdCom(), min);
                cargarStock();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Valor no válido (debe ser ≥ 0).").showAndWait();
            } catch (SQLException e) { mostrarError(e); }
        });
    }

    // ─── Configurar tabla Pedidos ─────────────────────────────────────────────

    private void configurarTablaPedidos() {
        cpId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(c.getValue().getIdCompra()).asObject());
        cpComponente.setCellValueFactory(c -> sp(c.getValue().getTipoComponente()));
        cpProveedor.setCellValueFactory(c ->  sp(c.getValue().getNombreProveedor()));
        cpCantidad.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(c.getValue().getCantidad()).asObject());
        cpUrgente.setCellValueFactory(c ->  sp(c.getValue().isEsUrgente() ? "Sí" : "—"));
        cpFecha.setCellValueFactory(c ->
                sp(c.getValue().getFechaPedido().format(FMT)));
        cpPrecio.setCellValueFactory(c ->
                sp(String.format("%.2f", c.getValue().getPrecioUnidadPedido())));
        cpDivisa.setCellValueFactory(c ->  sp(c.getValue().getDivisa()));
        cpEur.setCellValueFactory(c ->
                sp(String.format("%.2f", c.getValue().getPrecioEur())));
        cpEstado.setCellValueFactory(c ->  sp(c.getValue().getEstado().name()));

        // Color fila por estado
        tablaPedidos.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(CompraComponente item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                setStyle(switch (item.getEstado()) {
                    case pendiente  -> item.isEsUrgente()
                            ? "-fx-background-color: rgba(241,195,86,0.18);"
                            : "";
                    case recibido   -> "-fx-background-color: rgba(58,125,68,0.10);";
                    case alterado   -> "-fx-background-color: rgba(199,122,0,0.10);";
                    case cancelado,
                         devuelto   -> "-fx-background-color: rgba(150,150,150,0.10);";
                });
            }
        });

        // Filtro por estado
        cmbFiltroEstado.getItems().setAll(
                "Todos", "pendiente", "recibido", "alterado", "devuelto", "cancelado");
        cmbFiltroEstado.setValue("Todos");
        FilteredList<CompraComponente> filtrada = new FilteredList<>(datosPedidos, p -> true);
        cmbFiltroEstado.valueProperty().addListener((obs, old, val) ->
                filtrada.setPredicate(p -> "Todos".equals(val) || p.getEstado().name().equals(val)));
        tablaPedidos.setItems(filtrada);

        // Botones según selección
        tablaPedidos.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> actualizarBotonesPedido(sel));
    }

    private void actualizarBotonesPedido(CompraComponente sel) {
        boolean esPendiente = sel != null && sel.getEstado() == Estado.pendiente;
        boolean esRecibido  = sel != null &&
                (sel.getEstado() == Estado.recibido || sel.getEstado() == Estado.alterado);
        btnConfirmarRecibido.setDisable(!esPendiente);
        btnConfirmarAlterado.setDisable(!esPendiente);
        btnEditarPedido     .setDisable(!esPendiente);
        btnCancelarPedido   .setDisable(!esPendiente);
        btnDevolverPedido   .setDisable(!esRecibido);
    }

    private void cargarPedidos() {
        try {
            datosPedidos.setAll(compraDAO.getAll());
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    // ── Acciones pedidos ──────────────────────────────────────────────────────

    @FXML private void nuevoPedido() {
        FormularioCompraController.abrir(null, () -> {
            cargarPedidos();
            cargarStock();
        });
    }

    @FXML private void editarPedido() {
        CompraComponente sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        FormularioCompraController.abrirEditar(sel, () -> {
            cargarPedidos();
            cargarStock();
        });
    }

    @FXML private void confirmarRecibido() {
        CompraComponente sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Confirmar llegada");
        d.setHeaderText("Pedido #" + sel.getIdCompra() + " — " + sel.getTipoComponente());
        d.setContentText("Observación (opcional):");
        d.showAndWait().ifPresent(obs -> {
            try {
                compraDAO.confirmarRecibido(sel.getIdCompra(), obs.isBlank() ? null : obs);
                cargarPedidos();
                cargarStock();
            } catch (SQLException e) { mostrarError(e); }
        });
    }

    @FXML private void confirmarAlterado() {
        CompraComponente sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        // Diálogo simple: cantidad recibida
        TextInputDialog dCant = new TextInputDialog(String.valueOf(sel.getCantidad()));
        dCant.setTitle("Cantidad alterada");
        dCant.setHeaderText("Pedido #" + sel.getIdCompra() + " — ¿Cuántas unidades llegaron?");
        dCant.setContentText("Cantidad recibida:");
        Optional<String> rCant = dCant.showAndWait();
        if (rCant.isEmpty()) return;
        int cant;
        try { cant = Integer.parseInt(rCant.get().trim()); }
        catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Cantidad no válida.").showAndWait();
            return;
        }

        TextInputDialog dObs = new TextInputDialog();
        dObs.setTitle("Observación");
        dObs.setContentText("Observación (opcional):");
        Optional<String> rObs = dObs.showAndWait();
        if (rObs.isEmpty()) return;

        try {
            compraDAO.confirmarAlterado(sel.getIdCompra(), cant,
                    rObs.get().isBlank() ? null : rObs.get());
            cargarPedidos();
            cargarStock();
        } catch (SQLException e) { mostrarError(e); }
    }

    @FXML private void cancelarPedido() {
        CompraComponente sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        ConfirmDialog.mostrar(
                "Cancelar pedido",
                "¿Cancelar el pedido #" + sel.getIdCompra() + " de " + sel.getTipoComponente() + "?",
                "Cancelar pedido",
                () -> {
                    try {
                        compraDAO.cancelar(sel.getIdCompra());
                        cargarPedidos();
                    } catch (SQLException e) { mostrarError(e); }
                });
    }

    @FXML private void devolverPedido() {
        CompraComponente sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        TextInputDialog d = new TextInputDialog(
                sel.getCantidadRecibida() != null
                        ? String.valueOf(sel.getCantidadRecibida())
                        : String.valueOf(sel.getCantidad()));
        d.setTitle("Devolver pedido");
        d.setHeaderText("Pedido #" + sel.getIdCompra() + " — ¿Cuántas unidades se devuelven?");
        d.setContentText("Cantidad devuelta:");
        d.showAndWait().ifPresent(s -> {
            try {
                int cant = Integer.parseInt(s.trim());
                compraDAO.devolver(sel.getIdCompra(), cant);
                cargarPedidos();
                cargarStock();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Cantidad no válida.").showAndWait();
            } catch (SQLException e) { mostrarError(e); }
        });
    }

    // ─── Configurar tabla Proveedores ─────────────────────────────────────────

    private void configurarTablaProveedores() {
        cpvNombre.setCellValueFactory(c -> sp(c.getValue().getNombre()));
        cpvActivo.setCellValueFactory(c -> sp(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        cpvActivo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                setStyle("Activo".equals(val)
                        ? "-fx-text-fill:#3a7d44; -fx-font-weight:bold;"
                        : "-fx-text-fill:#9e9e9e;");
            }
        });
        tablaProveedores.setItems(datosProveedores);
        tablaProveedores.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            btnActivarProveedor.setDisable(sel == null);
            btnBorrarProveedor .setDisable(sel == null);
        });
    }

    private void cargarProveedores() {
        try {
            datosProveedores.setAll(proveedorDAO.getAll());
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    // ── Acciones proveedores ──────────────────────────────────────────────────

    @FXML private void nuevoProveedor() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Nuevo proveedor");
        d.setHeaderText(null);
        d.setContentText("Nombre del proveedor:");
        d.showAndWait().ifPresent(nombre -> {
            if (nombre.isBlank()) return;
            try {
                proveedorDAO.insertar(nombre.trim());
                cargarProveedores();
            } catch (SQLException e) { mostrarError(e); }
        });
    }

    @FXML private void activarProveedor() {
        Proveedor sel = tablaProveedores.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            proveedorDAO.setActivo(sel.getIdProv(), !sel.isActivo());
            cargarProveedores();
        } catch (SQLException e) { mostrarError(e); }
    }

    @FXML private void borrarProveedor() {
        Proveedor sel = tablaProveedores.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            if (proveedorDAO.tienePedidos(sel.getIdProv())) {
                new Alert(Alert.AlertType.WARNING,
                        "El proveedor tiene pedidos asociados. Desactívalo en lugar de borrarlo.")
                        .showAndWait();
                return;
            }
            ConfirmDialog.mostrar(
                    "Borrar proveedor",
                    "¿Eliminar el proveedor \"" + sel.getNombre() + "\"?",
                    "Borrar",
                    () -> {
                        try {
                            proveedorDAO.borrar(sel.getIdProv());
                            cargarProveedores();
                        } catch (SQLException e) { mostrarError(e); }
                    });
        } catch (SQLException e) { mostrarError(e); }
    }

    // ─── Util ─────────────────────────────────────────────────────────────────

    private static javafx.beans.property.SimpleStringProperty sp(String v) {
        return new javafx.beans.property.SimpleStringProperty(v);
    }

    private void mostrarError(SQLException e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
    }
}
