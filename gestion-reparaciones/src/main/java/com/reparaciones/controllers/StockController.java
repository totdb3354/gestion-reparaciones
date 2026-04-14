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

public class StockController implements com.reparaciones.utils.Recargable {

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
    @FXML private TableColumn<Componente, Integer>  colEnCamino;
    @FXML private TableColumn<Componente, Integer>  colStockMin;
    @FXML private TableColumn<Componente, String>   colUltimoPed;
    @FXML private TableColumn<Componente, String>   colEstado;
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
    @FXML private TableColumn<CompraComponente, Object>   cpCantidad;
    @FXML private TableColumn<CompraComponente, String>   cpFecha;
    @FXML private TableColumn<CompraComponente, String>   cpPrecio;
    @FXML private TableColumn<CompraComponente, String>   cpDivisa;
    @FXML private TableColumn<CompraComponente, String>   cpEur;
    @FXML private TableColumn<CompraComponente, String>   cpEstado;
    @FXML private MenuButton                              menuFiltroEstado;
    private final java.util.List<CheckBox> cbsEstado = new java.util.ArrayList<>();

    // ── Panel Proveedores ─────────────────────────────────────────────────────
    @FXML private TableView<Proveedor>             tablaProveedores;
    @FXML private TableColumn<Proveedor, String>   cpvNombre;
    @FXML private TableColumn<Proveedor, String>   cpvActivo;

    // ── Última actualización ──────────────────────────────────────────────────
    @FXML private Label lblUltimaActStock;
    @FXML private Label lblUltimaActPedidos;
    @FXML private Label lblUltimaActProveedores;

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final ComponenteDAO     componenteDAO = new ComponenteDAO();
    private final CompraComponenteDAO compraDAO   = new CompraComponenteDAO();
    private final ProveedorDAO      proveedorDAO  = new ProveedorDAO();

    // ── Observable lists ──────────────────────────────────────────────────────
    private final ObservableList<Componente>      datosStock      = FXCollections.observableArrayList();
    private final ObservableList<CompraComponente> datosPedidos   = FXCollections.observableArrayList();
    private final ObservableList<Proveedor>        datosProveedores = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    private final java.util.concurrent.ScheduledExecutorService poller =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "poller-stock");
                t.setDaemon(true);
                return t;
            });

    // ─── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configurarTablaStock();
        configurarTablaPedidos();
        configurarTablaProveedores();
        cargarStock();

        poller.scheduleAtFixedRate(
                () -> javafx.application.Platform.runLater(this::recargar),
                60, 60, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void detenerPolling() { poller.shutdownNow(); }

    // ─── Sidebar ──────────────────────────────────────────────────────────────

    @Override
    public void recargar() {
        if (pnlPedidos.isVisible())       cargarPedidos();
        else if (pnlProveedores.isVisible()) cargarProveedores();
        else                               cargarStock();
    }

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
        colEnCamino.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(c.getValue().getEnCamino()).asObject());
        colEnCamino.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : val == 0 ? "—" : String.valueOf(val));
            }
        });
        colStockMin.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(c.getValue().getStockMinimo()).asObject());
        colUltimoPed.setCellValueFactory(c -> {
            java.time.LocalDateTime dt = c.getValue().getUltimoPedido();
            return new javafx.beans.property.SimpleStringProperty(
                    dt != null ? dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—");
        });

        // Estado semáforo
        colEstado.setCellValueFactory(c -> sp(estadoComponente(c.getValue())));
        colEstado.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setGraphic(null); return; }
                String base = "-fx-background-radius: 10; -fx-padding: 2 10 2 10;" +
                              "-fx-font-size: 11px; -fx-font-weight: bold;";
                badge.setText(val);
                badge.setStyle(base + switch (val) {
                    case "OK"   -> "-fx-background-color: #E8EAF0; -fx-text-fill: #586376;";
                    case "Bajo" -> "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BG + "; -fx-text-fill: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + ";";
                    default     -> "-fx-background-color: #F9E0E3; -fx-text-fill: " + com.reparaciones.utils.Colores.ROJO_SIN_STOCK + ";";
                });
                setGraphic(badge);
            }
        });

        // Menú contextual (solo admin)
        if (com.reparaciones.Sesion.esAdmin()) {
            ContextMenu ctxStock = new ContextMenu();
            MenuItem itemPedir = new MenuItem("Pedir");
            MenuItem itemMin   = new MenuItem("Ajustar mínimo");
            ctxStock.getItems().addAll(itemPedir, itemMin);
            tablaStock.setContextMenu(ctxStock);
            tablaStock.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                itemPedir.setDisable(sel == null);
                itemMin  .setDisable(sel == null);
            });
            itemPedir.setOnAction(e -> { Componente sel = tablaStock.getSelectionModel().getSelectedItem(); if (sel != null) pedirComponente(sel); });
            itemMin  .setOnAction(e -> { Componente sel = tablaStock.getSelectionModel().getSelectedItem(); if (sel != null) ajustarMinimo(sel); });
        }

        // Filtros: buscador + estado (MenuButton con CheckBoxes)

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
        menuFiltroStock.getItems().addAll(itemOk, itemBaj, itemSin);

        tablaStock.setRowFactory(tv -> new TableRow<>() {
            {
                selectedProperty().addListener((obs, o, sel) -> actualizarEstilo());
            }
            private void actualizarEstilo() {
                if (isEmpty() || getItem() == null) { setStyle("-fx-border-width: 0 0 0 8; -fx-border-color: transparent;"); return; }
                if (isSelected()) {
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.AZUL_MEDIO + ";" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SELECTED_BRD + " transparent;" +
                            "-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0;");
                } else {
                    setStyle(switch (estadoComponente(getItem())) {
                        case "OK"   -> "-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0; -fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " transparent;";
                        case "Bajo" -> "-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0; -fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + ";";
                        default     -> "-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0; -fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " " + com.reparaciones.utils.Colores.ROJO_SIN_STOCK + ";";
                    });
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
            String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            if (lblUltimaActStock != null) lblUltimaActStock.setText("Actualizado " + hora);
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
        cpCantidad.setCellValueFactory(c -> {
            CompraComponente p = c.getValue();
            String texto = switch (p.getEstado()) {
                case parcial   -> p.getCantidadRecibida() != null
                        ? p.getCantidadRecibida() + "/" + p.getCantidad()
                        : String.valueOf(p.getCantidad());
                case recibido  -> p.getCantidadRecibida() != null
                        ? String.valueOf(p.getCantidadRecibida())
                        : String.valueOf(p.getCantidad());
                default        -> String.valueOf(p.getCantidad());
            };
            return new javafx.beans.property.SimpleObjectProperty<>(texto);
        });
        cpFecha.setCellValueFactory(c ->
                sp(c.getValue().getFechaPedido().format(FMT)));
        cpPrecio.setCellFactory(col -> new TableCell<>() {
            private final Label lbl   = new Label();
            private final Label alert = new Label("!");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4, lbl, alert);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                alert.setStyle("-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD +
                        "; -fx-font-weight: bold; -fx-font-size: 12px;");
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                CompraComponente p = getTableRow().getItem();
                String simbolo = switch (p.getDivisa()) {
                    case "EUR" -> "€";
                    case "USD" -> "$";
                    default    -> p.getDivisa();
                };
                boolean sinPrecio = p.getPrecioUnidadPedido() == 0 && p.getEstado() == Estado.recibido;
                lbl.setText(String.format("%.2f %s", p.getPrecioUnidadPedido(), simbolo));
                lbl.setStyle(sinPrecio ? "-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + "; -fx-font-weight: bold;" : "");
                alert.setVisible(sinPrecio);
                alert.setManaged(sinPrecio);
                setGraphic(box);
            }
        });
        cpPrecio.setCellValueFactory(c -> sp(""));
        cpDivisa.setVisible(false);
        cpEur.setCellFactory(col -> new TableCell<>() {
            private final Label lbl   = new Label();
            private final Label alert = new Label("!");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4, lbl, alert);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                alert.setStyle("-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD +
                        "; -fx-font-weight: bold; -fx-font-size: 12px;");
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                CompraComponente p = getTableRow().getItem();
                int unidades = p.getEstado() == Estado.recibido && p.getCantidadRecibida() != null
                        ? p.getCantidadRecibida() : p.getCantidad();
                double total = unidades * p.getPrecioEur();
                boolean sinPrecio = total == 0 && p.getEstado() == Estado.recibido;
                lbl.setText(String.format("%.2f €", total));
                lbl.setStyle(sinPrecio ? "-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + "; -fx-font-weight: bold;" : "");
                alert.setVisible(sinPrecio);
                alert.setManaged(sinPrecio);
                setGraphic(box);
            }
        });
        cpEur.setCellValueFactory(c -> sp(""));
        cpEstado.setCellValueFactory(c -> sp(c.getValue().getEstado().name()));

        // Color fila por estado
        tablaPedidos.setRowFactory(tv -> new TableRow<>() {
            {
                selectedProperty().addListener((obs, o, sel) -> actualizarEstilo());
            }
            private void actualizarEstilo() {
                CompraComponente item = getItem();
                if (isEmpty() || item == null) { setStyle("-fx-border-width: 0 0 0 8; -fx-border-color: transparent;"); return; }
                if (isSelected()) {
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.AZUL_MEDIO + ";" +
                            "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SELECTED_BRD + " transparent;" +
                            "-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0;");
                    return;
                }
                String sep = com.reparaciones.utils.Colores.FILA_SEP;
                String barraIzq = "-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0; -fx-border-color: transparent transparent " + sep + " ";
                setStyle(switch (item.getEstado()) {
                    case pendiente -> item.isEsUrgente()
                            ? barraIzq + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + ";"
                            : "-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0; -fx-border-color: transparent transparent " + sep + " transparent;";
                    case recibido  -> barraIzq + com.reparaciones.utils.Colores.FILA_RECIBIDO_BRD + ";";
                    case parcial   -> barraIzq + com.reparaciones.utils.Colores.FILA_PARCIAL_BRD + ";";
                    case cancelado -> "-fx-opacity: 0.45; -fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0; -fx-border-color: transparent transparent " + sep + " transparent;";
                });
            }
            @Override protected void updateItem(CompraComponente item, boolean empty) {
                super.updateItem(item, empty);
                actualizarEstilo();
            }
        });

        // Badge de estado en columna Estado
        cpEstado.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            private final Label alerta = new Label("⚠");
            private final javafx.scene.layout.HBox contenedor = new javafx.scene.layout.HBox(6, badge, alerta);
            {
                contenedor.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                alerta.setStyle("-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + "; -fx-font-size: 13px;");
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                CompraComponente p = getTableRow().getItem();
                boolean urgente = p.isEsUrgente();
                boolean cancelado = p.getEstado() != Estado.pendiente && p.getEstado() != Estado.parcial;
                String bg, txt;
                switch (p.getEstado()) {
                    case pendiente -> { bg = urgente ? com.reparaciones.utils.Colores.FILA_SOLICITUD_BG  : "#E8EAF0";
                                        txt = urgente ? com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD : "#586376"; }
                    case recibido  -> { bg = com.reparaciones.utils.Colores.FILA_RECIBIDO_BG;  txt = com.reparaciones.utils.Colores.FILA_RECIBIDO_BRD; }
                    case parcial   -> { bg = com.reparaciones.utils.Colores.FILA_PARCIAL_BG;   txt = com.reparaciones.utils.Colores.FILA_PARCIAL_BRD; }
                    case cancelado  -> { bg = com.reparaciones.utils.Colores.FILA_CANCELADO_BG; txt = com.reparaciones.utils.Colores.FILA_CANCELADO_TEXT; }
                    default        -> { bg = "#E8EAF0"; txt = "#586376"; }
                }
                badge.setText(val);
                badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + txt + ";" +
                        "-fx-background-radius: 12; -fx-padding: 3 10 3 10; -fx-font-size: 11px; -fx-font-weight: bold;");
                alerta.setVisible(urgente && !cancelado);
                alerta.setManaged(urgente && !cancelado);
                setGraphic(contenedor);
            }
        });

        // Filtro por estado — multiselector
        FilteredList<CompraComponente> filtrada = new FilteredList<>(datosPedidos, p -> true);
        for (String estado : new String[]{"pendiente", "parcial", "recibido", "cancelado"}) {
            CheckBox cb = new CheckBox(estado);
            cb.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
            cb.selectedProperty().addListener((obs, o, n) -> {
                actualizarTextoFiltroEstado();
                java.util.List<String> sel = cbsEstado.stream()
                        .filter(CheckBox::isSelected).map(CheckBox::getText)
                        .collect(java.util.stream.Collectors.toList());
                filtrada.setPredicate(p -> sel.isEmpty() || sel.contains(p.getEstado().name()));
            });
            cbsEstado.add(cb);
            menuFiltroEstado.getItems().add(new CustomMenuItem(cb, false));
        }
        tablaPedidos.setItems(filtrada);

        // Menú contextual según estado de la fila
        ContextMenu ctx = new ContextMenu();
        tablaPedidos.setContextMenu(ctx);
        tablaPedidos.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> construirMenuContextual(ctx, sel));
    }

    private void construirMenuContextual(ContextMenu ctx, CompraComponente sel) {
        ctx.getItems().clear();
        if (sel == null) return;
        switch (sel.getEstado()) {
            case pendiente -> {
                MenuItem confirmar = new MenuItem("Confirmar recibido");
                MenuItem parcial   = new MenuItem("Recepción parcial");
                MenuItem editar    = new MenuItem("Editar");
                MenuItem cancelar  = new MenuItem("Cancelar pedido");
                confirmar.setOnAction(e -> confirmarRecibido());
                parcial  .setOnAction(e -> confirmarParcial());
                editar   .setOnAction(e -> editarPedido());
                cancelar .setOnAction(e -> cancelarPedido());
                ctx.getItems().addAll(parcial, confirmar, new SeparatorMenuItem(), editar, cancelar);
            }
            case parcial -> {
                MenuItem resto    = new MenuItem("Recibir resto");
                MenuItem alterado = new MenuItem("Cerrar sin resto");
                resto   .setOnAction(e -> recibirResto());
                alterado.setOnAction(e -> confirmarAlterado());
                ctx.getItems().addAll(resto, alterado);
            }
            case recibido -> {
                MenuItem editar = new MenuItem("Editar");
                editar.setOnAction(e -> editarPedido());
                ctx.getItems().add(editar);
            }
            default -> { /* cancelado: sin acciones */ }
        }
    }

    private void cargarPedidos() {
        try {
            datosPedidos.setAll(compraDAO.getAll());
            String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            if (lblUltimaActPedidos != null) lblUltimaActPedidos.setText("Actualizado " + hora);
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
        try {
            compraDAO.confirmarRecibido(sel);
            cargarPedidos();
            cargarStock();
        } catch (com.reparaciones.utils.StaleDataException e) {
            mostrarConflicto(); cargarPedidos();
        } catch (SQLException e) { mostrarError(e); }
    }

    @FXML private void confirmarAlterado() {
        CompraComponente sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            compraDAO.confirmarAlterado(sel);
            cargarPedidos();
            cargarStock();
        } catch (com.reparaciones.utils.StaleDataException e) {
            mostrarConflicto(); cargarPedidos();
        } catch (SQLException e) { mostrarError(e); }
    }

    
    @FXML private void confirmarParcial() {
        CompraComponente sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        TextInputDialog dCant = new TextInputDialog();
        dCant.setTitle("Recepción parcial");
        dCant.setHeaderText("Pedido #" + sel.getIdCompra() + " — " + sel.getTipoComponente()
                + " (" + sel.getCantidad() + " pedidas)");
        dCant.setContentText("Cantidad recibida ahora:");
        Optional<String> rCant = dCant.showAndWait();
        if (rCant.isEmpty()) return;
        int cant;
        try {
            cant = Integer.parseInt(rCant.get().trim());
            if (cant <= 0 || cant >= sel.getCantidad()) {
                new Alert(Alert.AlertType.WARNING,
                        "La cantidad debe ser mayor que 0 y menor que " + sel.getCantidad() + ".").showAndWait();
                return;
            }
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Cantidad no válida.").showAndWait();
            return;
        }

        try {
            compraDAO.confirmarParcial(sel, cant);
            cargarPedidos();
            cargarStock();
        } catch (com.reparaciones.utils.StaleDataException e) {
            mostrarConflicto(); cargarPedidos();
        } catch (SQLException e) { mostrarError(e); }
    }

    @FXML private void recibirResto() {
        CompraComponente sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        int restante = sel.getCantidad() - (sel.getCantidadRecibida() != null ? sel.getCantidadRecibida() : 0);

        TextInputDialog dCant = new TextInputDialog(String.valueOf(restante));
        dCant.setTitle("Recibir unidades");
        dCant.setHeaderText("Pedido #" + sel.getIdCompra() + " — " + sel.getTipoComponente()
                + " (recibidas: " + sel.getCantidadRecibida() + "/" + sel.getCantidad() + ")"
                + "\nSi introduces " + restante + " o más, el pedido se cerrará como recibido.");
        dCant.setContentText("Cantidad que llega ahora:");
        Optional<String> rCant = dCant.showAndWait();
        if (rCant.isEmpty()) return;
        int cant;
        try {
            cant = Integer.parseInt(rCant.get().trim());
            if (cant <= 0) {
                new Alert(Alert.AlertType.WARNING, "La cantidad debe ser mayor que 0.").showAndWait();
                return;
            }
            int yaRecibidas = sel.getCantidadRecibida() != null ? sel.getCantidadRecibida() : 0;
            if (yaRecibidas + cant > sel.getCantidad()) {
                new Alert(Alert.AlertType.WARNING,
                        "No puedes recibir más de lo pedido. Faltan " + restante + " unidad(es).").showAndWait();
                return;
            }
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Cantidad no válida.").showAndWait();
            return;
        }

        try {
            compraDAO.recibirResto(sel, cant);
            cargarPedidos();
            cargarStock();
        } catch (com.reparaciones.utils.StaleDataException e) {
            mostrarConflicto(); cargarPedidos();
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
                        compraDAO.cancelar(sel);
                        cargarPedidos();
                    } catch (com.reparaciones.utils.StaleDataException e) {
                        mostrarConflicto(); cargarPedidos();
                    } catch (SQLException e) { mostrarError(e); }
                });
    }

    // ─── Configurar tabla Proveedores ─────────────────────────────────────────

    private void configurarTablaProveedores() {
        cpvNombre.setCellValueFactory(c -> sp(c.getValue().getNombre()));
        cpvActivo.setCellValueFactory(c -> sp(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        cpvActivo.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setGraphic(null); return; }
                String base = "-fx-background-radius: 10; -fx-padding: 2 10 2 10;" +
                              "-fx-font-size: 11px; -fx-font-weight: bold;";
                if ("Activo".equals(val)) {
                    badge.setText("Activo");
                    badge.setStyle(base +
                        "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_REPARADO_BG + ";" +
                        "-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_REPARADO_ICO + ";");
                } else {
                    badge.setText("Inactivo");
                    badge.setStyle(base +
                        "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_CANCELADO_BG + ";" +
                        "-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_CANCELADO_TEXT + ";");
                }
                setGraphic(badge);
            }
        });
        tablaProveedores.setRowFactory(tv -> new TableRow<>() {
            {
                selectedProperty().addListener((obs, o, sel) -> actualizarEstilo());
            }
            private void actualizarEstilo() {
                if (isEmpty() || getItem() == null) { setStyle("-fx-border-width: 0 0 0 8; -fx-border-color: transparent;"); return; }
                if (isSelected()) {
                    setStyle("-fx-background-color: " + com.reparaciones.utils.Colores.AZUL_MEDIO + ";" +
                             "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SELECTED_BRD + " transparent;" +
                             "-fx-border-width: 0 0 0.2 0;");
                } else if (getItem().isActivo()) {
                    setStyle("-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0;" +
                             "-fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " " + com.reparaciones.utils.Colores.FILA_REPARADO_BRD + ";");
                } else {
                    setStyle("-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0; -fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " transparent;");
                }
            }
            @Override protected void updateItem(Proveedor item, boolean empty) {
                super.updateItem(item, empty);
                actualizarEstilo();
            }
        });
        tablaProveedores.setItems(datosProveedores);

        ContextMenu ctxProv = new ContextMenu();
        tablaProveedores.setContextMenu(ctxProv);
        tablaProveedores.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            ctxProv.getItems().clear();
            if (sel == null) return;
            MenuItem itemToggle = new MenuItem(sel.isActivo() ? "Desactivar" : "Activar");
            itemToggle.setOnAction(e -> activarProveedor());
            ctxProv.getItems().add(itemToggle);
            try {
                if (!proveedorDAO.tienePedidos(sel.getIdProv())) {
                    MenuItem itemBorrar = new MenuItem("Borrar");
                    itemBorrar.setOnAction(e -> borrarProveedor());
                    ctxProv.getItems().add(itemBorrar);
                }
            } catch (SQLException e) { mostrarError(e); }
        });
    }

    private void cargarProveedores() {
        try {
            datosProveedores.setAll(proveedorDAO.getAll());
            String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            if (lblUltimaActProveedores != null) lblUltimaActProveedores.setText("Actualizado " + hora);
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

    private void actualizarTextoFiltroEstado() {
        java.util.List<String> sel = cbsEstado.stream()
                .filter(CheckBox::isSelected).map(CheckBox::getText)
                .collect(java.util.stream.Collectors.toList());
        if      (sel.isEmpty())    menuFiltroEstado.setText("Estado");
        else if (sel.size() == 1)  menuFiltroEstado.setText(sel.get(0));
        else                       menuFiltroEstado.setText(sel.size() + " estados");
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

    private void mostrarConflicto() {
        new Alert(Alert.AlertType.WARNING,
                "Este pedido fue modificado por otro usuario. Los datos se han recargado.")
                .showAndWait();
    }
}
