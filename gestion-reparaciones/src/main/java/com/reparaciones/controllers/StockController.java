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
    @FXML private MenuButton                        menuFiltroStock;
    @FXML private PieChart                          chartEstado;
    @FXML private javafx.scene.chart.BarChart<String, Number>       chartSku;
    @FXML private javafx.scene.chart.CategoryAxis                   chartSkuX;
    @FXML private javafx.scene.chart.NumberAxis                     chartSkuY;
    @FXML private Label                             lblChartSku;
    @FXML private Label                             lblPlaceholder;
    @FXML private javafx.scene.layout.HBox         pnlLeyenda;

    private CheckBox cbOk;
    private CheckBox cbBajo;
    private CheckBox cbSinStock;

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

    private void navegarAComponente(int idCom) {
        cbOk.setSelected(false);
        cbBajo.setSelected(false);
        cbSinStock.setSelected(false);
        txtBuscador.clear();
        mostrarTabStock();
        datosStock.stream()
                .filter(c -> c.getIdCom() == idCom)
                .findFirst()
                .ifPresent(c -> {
                    tablaStock.getSelectionModel().select(c);
                    tablaStock.scrollTo(c);
                });
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
        colEstado.setCellValueFactory(c -> sp(estadoComponente(c.getValue())));
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
                        "-fx-background-color:#3A6186; -fx-text-fill:white;" +
                        "-fx-background-radius:4; -fx-cursor:hand;");
                btnMin.setStyle("-fx-font-size:11px; -fx-padding:4 8 4 8;" +
                        "-fx-background-color:" + com.reparaciones.utils.Colores.AZUL_GRIS + "; -fx-text-fill:white;" +
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

        // Filtros: buscador + estado (MenuButton con CheckBoxes)
        menuFiltroStock.setStyle(
                "-fx-background-color: white; -fx-border-color: #A9A9A9;" +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px;");

        cbOk       = new CheckBox("OK");
        cbBajo     = new CheckBox("Bajo");
        cbSinStock = new CheckBox("Sin stock");
        for (CheckBox cb : new CheckBox[]{cbOk, cbBajo, cbSinStock})
            cb.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");

        FilteredList<Componente> filtrada = new FilteredList<>(datosStock, c -> true);

        Runnable aplicarFiltros = () -> {
            boolean ok  = cbOk.isSelected();
            boolean baj = cbBajo.isSelected();
            boolean sin = cbSinStock.isSelected();
            boolean ninguno = !ok && !baj && !sin;
            actualizarTextoFiltroStock(ok, baj, sin, ninguno);
            filtrada.setPredicate(c -> {
                String texto = txtBuscador.getText();
                boolean coincideTexto = texto == null || texto.isBlank() ||
                        c.getTipo().toLowerCase().contains(texto.toLowerCase().trim());
                String est = estadoComponente(c);
                boolean coincideEstado = ninguno ||
                        (ok && "OK".equals(est)) ||
                        (baj && "Bajo".equals(est)) ||
                        (sin && "Sin stock".equals(est));
                return coincideTexto && coincideEstado;
            });
        };

        for (CheckBox cb : new CheckBox[]{cbOk, cbBajo, cbSinStock})
            cb.selectedProperty().addListener((obs, o, n) -> aplicarFiltros.run());
        txtBuscador.textProperty().addListener((obs, old, val) -> aplicarFiltros.run());

        CustomMenuItem itemOk  = new CustomMenuItem(cbOk,       false);
        CustomMenuItem itemBaj = new CustomMenuItem(cbBajo,     false);
        CustomMenuItem itemSin = new CustomMenuItem(cbSinStock, false);
        for (CustomMenuItem mi : new CustomMenuItem[]{itemOk, itemBaj, itemSin})
            mi.setStyle("-fx-background-color: white;");
        menuFiltroStock.getItems().addAll(itemOk, itemBaj, itemSin);

        tablaStock.setRowFactory(tv -> new TableRow<>() {
            {
                selectedProperty().addListener((obs, o, sel) -> actualizarEstilo());
            }
            private void actualizarEstilo() {
                if (isEmpty() || getItem() == null) { setStyle(""); return; }
                if (isSelected()) {
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.AZUL_MEDIO + ";" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SELECTED_BRD + " transparent;" +
                            "-fx-border-width: 0 0 0.2 0;");
                } else {
                    setStyle("");
                }
            }
            @Override protected void updateItem(Componente item, boolean empty) {
                super.updateItem(item, empty);
                actualizarEstilo();
            }
        });

        tablaStock.setItems(filtrada);

        // Listener selección → gráfica SKU
        tablaStock.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) {
                mostrarPlaceholderSku();
            } else {
                cargarChartSku(sel);
            }
        });
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

        // Leyenda manual
        pnlLeyenda.getChildren().setAll(
                leyendaItem("#3a7d44", "OK (" + ok + ")"),
                leyendaItem("#c77a00", "Bajo (" + bajo + ")"),
                leyendaItem(com.reparaciones.utils.Colores.ROJO_SIN_STOCK, "Sin stock (" + sinStock + ")")
        );
    }

    private javafx.scene.layout.HBox leyendaItem(String color, String texto) {
        javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(10, 10);
        rect.setFill(javafx.scene.paint.Color.web(color));
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #2C3B54;");
        javafx.scene.layout.HBox hb = new javafx.scene.layout.HBox(4, rect, lbl);
        hb.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return hb;
    }

    private void mostrarPlaceholderSku() {
        lblChartSku.setText("Selecciona un componente");
        chartSku.setVisible(false);
        chartSku.setManaged(false);
        lblPlaceholder.setVisible(true);
        lblPlaceholder.setManaged(true);
    }

    private void cargarChartSku(Componente c) {
        int pendiente = 0;
        try {
            pendiente = compraDAO.getCantidadPendientePorComponente(c.getIdCom());
        } catch (SQLException e) {
            mostrarError(e);
            return;
        }
        lblChartSku.setText(c.getTipo());
        lblPlaceholder.setVisible(false);
        lblPlaceholder.setManaged(false);

        javafx.scene.chart.XYChart.Series<String, Number> serie = new javafx.scene.chart.XYChart.Series<>();
        serie.getData().add(new javafx.scene.chart.XYChart.Data<>("Stock", c.getStock()));
        serie.getData().add(new javafx.scene.chart.XYChart.Data<>("Pedido", pendiente));

        int maxVal = Math.max(c.getStock(), pendiente);
        chartSkuY.setAutoRanging(false);
        chartSkuY.setLowerBound(0);
        chartSkuY.setUpperBound(maxVal == 0 ? 1 : maxVal);
        chartSkuY.setTickUnit(Math.max(1, maxVal / 5));

        chartSku.getData().setAll(java.util.List.of(serie));
        chartSku.setVisible(true);
        chartSku.setManaged(true);

        String colorStock = switch (estadoComponente(c)) {
            case "Sin stock" -> com.reparaciones.utils.Colores.ROJO_SIN_STOCK;
            case "Bajo"      -> "#c77a00";
            default          -> "#3a7d44";
        };

        // Los nodos se crean tras el layout — usamos listener para aplicar color y etiqueta
        for (int i = 0; i < serie.getData().size(); i++) {
            final String color = i == 0 ? colorStock : com.reparaciones.utils.Colores.TEXTO_ACCION;
            final int valor = serie.getData().get(i).getYValue().intValue();
            javafx.scene.chart.XYChart.Data<String, Number> dato = serie.getData().get(i);
            Runnable aplicar = () -> {
                javafx.scene.Node nodo = dato.getNode();
                if (nodo == null) return;
                nodo.setStyle("-fx-bar-fill: " + color + ";");
                Label etiqueta = new Label(String.valueOf(valor));
                etiqueta.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2C3B54;");
                javafx.scene.layout.StackPane bar = (javafx.scene.layout.StackPane) nodo;
                bar.getChildren().add(etiqueta);
                javafx.scene.layout.StackPane.setAlignment(etiqueta, javafx.geometry.Pos.TOP_CENTER);
                etiqueta.setTranslateY(-16);
            };
            if (dato.getNode() != null) {
                aplicar.run();
            } else {
                dato.nodeProperty().addListener((obs, o, node) -> { if (node != null) aplicar.run(); });
            }
        }
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
        cpComponente.setCellFactory(col -> new TableCell<>() {
            private final Label lbl = new Label();
            {
                lbl.setStyle("-fx-cursor: hand;");
                lbl.setOnMouseEntered(e -> lbl.setStyle("-fx-cursor: hand; -fx-underline: true;"));
                lbl.setOnMouseExited(e  -> lbl.setStyle("-fx-cursor: hand; -fx-underline: false;"));
                lbl.setOnMouseClicked(e -> {
                    CompraComponente pedido = getTableView().getItems().get(getIndex());
                    navegarAComponente(pedido.getIdCom());
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                lbl.setText(item);
                setGraphic(lbl);
            }
        });
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
            {
                selectedProperty().addListener((obs, o, sel) -> actualizarEstilo());
            }
            private void actualizarEstilo() {
                CompraComponente item = getItem();
                if (isEmpty() || item == null) { setStyle(""); return; }
                if (isSelected()) {
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.AZUL_MEDIO + ";" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SELECTED_BRD + " transparent;" +
                            "-fx-border-width: 0 0 0.2 0;");
                    return;
                }
                String brd = "-fx-border-width: 0 0 0.3 0; -fx-border-color: transparent transparent ";
                setStyle(switch (item.getEstado()) {
                    case pendiente -> item.isEsUrgente()
                            ? "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BG + ";" +
                              brd + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + " transparent;"
                            : "";
                    case recibido  -> "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_RECIBIDO_BG + ";" +
                                     brd + com.reparaciones.utils.Colores.FILA_RECIBIDO_BRD + " transparent;";
                    case alterado  -> "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_ALTERADO_BG + ";" +
                                     brd + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + " transparent;";
                    case cancelado,
                         devuelto  -> "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_CANCELADO_BG + ";" +
                                     brd + com.reparaciones.utils.Colores.FILA_CANCELADO_BRD + " transparent;";
                });
            }
            @Override protected void updateItem(CompraComponente item, boolean empty) {
                super.updateItem(item, empty);
                actualizarEstilo();
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
        tablaProveedores.setRowFactory(tv -> new TableRow<>() {
            {
                selectedProperty().addListener((obs, o, sel) -> actualizarEstilo());
            }
            private void actualizarEstilo() {
                if (isEmpty() || getItem() == null) { setStyle(""); return; }
                setStyle(isSelected()
                        ? "-fx-background-color: " + com.reparaciones.utils.Colores.AZUL_MEDIO + ";" +
                          "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SELECTED_BRD + " transparent;" +
                          "-fx-border-width: 0 0 0.2 0;"
                        : "");
            }
            @Override protected void updateItem(Proveedor item, boolean empty) {
                super.updateItem(item, empty);
                actualizarEstilo();
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

    private static String estadoComponente(Componente c) {
        if (c.getStock() == 0)                    return "Sin stock";
        if (c.getStock() <= c.getStockMinimo())   return "Bajo";
        return "OK";
    }

    private static javafx.beans.property.SimpleStringProperty sp(String v) {
        return new javafx.beans.property.SimpleStringProperty(v);
    }

    private void actualizarTextoFiltroStock(boolean ok, boolean baj, boolean sin, boolean ninguno) {
        int total = (ok ? 1 : 0) + (baj ? 1 : 0) + (sin ? 1 : 0);
        if (ninguno || total == 3) menuFiltroStock.setText("Estado");
        else if (total == 1)      menuFiltroStock.setText(ok ? "OK" : baj ? "Bajo" : "Sin stock");
        else                      menuFiltroStock.setText(total + " estados");
    }

    private void mostrarError(SQLException e) {
        new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
    }
}
