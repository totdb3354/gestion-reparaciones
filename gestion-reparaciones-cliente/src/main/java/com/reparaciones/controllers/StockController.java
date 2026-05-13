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

import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controlador de la vista de stock y pedidos.
 * <p>Presenta dos secciones accesibles desde el sidebar:</p>
 * <ul>
 *   <li><b>Stock</b> — tabla de componentes con stock actual, mínimo y unidades en camino.
 *       Permite editar umbrales, hacer pedidos y ver el histórico de pedidos.</li>
 *   <li><b>Pedidos</b> — tabla de pedidos con acciones de confirmación (total/parcial/resto),
 *       cancelación y edición.</li>
 * </ul>
 * <p>Implementa {@link com.reparaciones.utils.Recargable} y
 * {@link com.reparaciones.utils.Exportable}.</p>
 *
 * @role ADMIN
 */
public class StockController implements com.reparaciones.utils.Recargable, com.reparaciones.utils.Exportable {

    // ── Sidebar ──────────────────────────────────────────────────────────────
    @FXML private Button btnTabStock;
    @FXML private Button btnTabPedidos;
    @FXML private Button btnTabProveedores;

    // ── Panels ────────────────────────────────────────────────────────────────
    @FXML private VBox   pnlStock;
    @FXML private VBox   pnlPedidos;
    @FXML private VBox   pnlProveedores;
    @FXML private Button btnNuevoPedido;
    @FXML private Button btnNuevoProveedor;
    @FXML private Button btnLimpiarFiltrosStock;
    @FXML private Button btnLimpiarFiltrosPedidos;

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
    @FXML private TextField                         txtBuscadorPedidos;
    @FXML private PieChart                          chartEstado;
    @FXML private javafx.scene.chart.BarChart<String, Number>       chartSku;
    @FXML private javafx.scene.chart.CategoryAxis                   chartSkuX;
    @FXML private javafx.scene.chart.NumberAxis                     chartSkuY;
    @FXML private Label                             lblChartSku;
    @FXML private Label                             lblPlaceholder;
    @FXML private javafx.scene.layout.HBox         pnlLeyenda;
    @FXML private Label                             lblTotalStock;

    private CheckBox cbOk;
    private CheckBox cbBajo;
    private CheckBox cbSinStock;
    private CheckBox cbDesactivado;
    private CustomMenuItem itemDesactivadoMenu;

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
    @FXML private DatePicker                              dpPedidosDesde;
    @FXML private DatePicker                              dpPedidosHasta;
    private final java.util.List<CheckBox> cbsEstado = new java.util.ArrayList<>();

    // ── Panel Proveedores ─────────────────────────────────────────────────────
    @FXML private TableView<Proveedor>             tablaProveedores;
    @FXML private TableColumn<Proveedor, String>   cpvNombre;
    @FXML private TableColumn<Proveedor, String>   cpvDivisa;
    @FXML private TableColumn<Proveedor, String>   cpvActivo;

    // ── Última actualización ──────────────────────────────────────────────────
    @FXML private Label lblUltimaActStock;
    @FXML private Label lblDesactivados;
    @FXML private Label lblUltimaActPedidos;
    @FXML private Label lblUltimaActProveedores;

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final ComponenteDAO       componenteDAO    = new ComponenteDAO();
    private final CompraComponenteDAO compraDAO        = new CompraComponenteDAO();
    private final ProveedorDAO        proveedorDAO     = new ProveedorDAO();
    private final com.reparaciones.dao.SolicitudStockDAO solicitudStockDAO =
            new com.reparaciones.dao.SolicitudStockDAO();

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
        if (!com.reparaciones.Sesion.esSuperTecnico()) {
            btnNuevoPedido   .setVisible(false); btnNuevoPedido   .setManaged(false);
            btnNuevoProveedor.setVisible(false); btnNuevoProveedor.setManaged(false);
        }
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

    private void navegarAPedidosDeComponente(Componente comp) {
        mostrarTabPedidos();
        // Activar solo pendiente + parcial (los que forman "en camino")
        cbsEstado.forEach(cb -> cb.setSelected(
                cb.getText().equals("pendiente") || cb.getText().equals("parcial")));
        // Rellenar buscador con el nombre del componente
        txtBuscadorPedidos.setText(comp.getTipo());
        // Seleccionar y hacer scroll al primer pedido activo del componente
        javafx.application.Platform.runLater(() ->
            tablaPedidos.getItems().stream().findFirst().ifPresent(p -> {
                tablaPedidos.getSelectionModel().select(p);
                tablaPedidos.scrollTo(p);
            })
        );
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

    public void irAPedidos() { mostrarTabPedidos(); }

    @FXML private void mostrarTabProveedores() {
        mostrarPanel(pnlProveedores, btnTabProveedores);
        cargarProveedores();
    }

    @FXML private void limpiarFiltrosStock() {
        cbOk.setSelected(false);
        cbBajo.setSelected(false);
        cbSinStock.setSelected(false);
        cbDesactivado.setSelected(false);
        txtBuscador.clear();
    }

    @FXML private void limpiarFiltrosPedidos() {
        cbsEstado.forEach(cb -> cb.setSelected(false));
        txtBuscadorPedidos.clear();
        dpPedidosDesde.setValue(null);
        dpPedidosHasta.setValue(null);
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
            private final Label lbl = new Label();
            {
                lbl.setStyle("-fx-cursor: hand; -fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ACCION + ";");
                lbl.setOnMouseEntered(e -> lbl.setStyle("-fx-cursor: hand; -fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ACCION + "; -fx-underline: true;"));
                lbl.setOnMouseExited(e  -> lbl.setStyle("-fx-cursor: hand; -fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ACCION + "; -fx-underline: false;"));
                lbl.setOnMouseClicked(e -> {
                    Componente comp = getTableView().getItems().get(getIndex());
                    navegarAPedidosDeComponente(comp);
                });
            }
            @Override protected void updateItem(Integer val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setGraphic(null); return; }
                if (val == 0) { setText("—"); setGraphic(null); }
                else          { lbl.setText(String.valueOf(val)); setText(null); setGraphic(lbl); }
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
                    case "OK"          -> "-fx-background-color: #E8EAF0; -fx-text-fill: #586376;";
                    case "Bajo"        -> "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BG + "; -fx-text-fill: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + ";";
                    case "Desactivado" -> "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_CANCELADO_BG + "; -fx-text-fill: " + com.reparaciones.utils.Colores.FILA_CANCELADO_TEXT + ";";
                    default            -> "-fx-background-color: #F9E0E3; -fx-text-fill: " + com.reparaciones.utils.Colores.ROJO_SIN_STOCK + ";";
                });
                setGraphic(badge);
            }
        });

        // Menú contextual (supertecnico: completo; tecnico: solo solicitar)
        if (!com.reparaciones.Sesion.esAdmin()) {
            ContextMenu ctxStock   = new ContextMenu();
            MenuItem itemSolicitar = new MenuItem("Solicitar pieza");

            if (com.reparaciones.Sesion.esSuperTecnico()) {
                MenuItem itemPedir        = new MenuItem("Pedir");
                MenuItem itemMin          = new MenuItem("Ajustar mínimo");
                MenuItem itemEditar       = new MenuItem("Editar stock");
                MenuItem itemToggleActivo = new MenuItem("Desactivar");
                ctxStock.getItems().addAll(itemPedir, itemEditar, new SeparatorMenuItem(), itemMin,
                        new SeparatorMenuItem(), itemToggleActivo, new SeparatorMenuItem(), itemSolicitar);
                tablaStock.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                    itemPedir        .setDisable(sel == null);
                    itemMin          .setDisable(sel == null);
                    itemEditar       .setDisable(sel == null);
                    itemToggleActivo .setDisable(sel == null);
                    itemSolicitar    .setDisable(sel == null);
                    if (sel != null) itemToggleActivo.setText(sel.isActivo() ? "Desactivar" : "Activar");
                });
                itemPedir       .setOnAction(e -> { Componente sel = tablaStock.getSelectionModel().getSelectedItem(); if (sel != null) pedirComponente(sel); });
                itemMin         .setOnAction(e -> { Componente sel = tablaStock.getSelectionModel().getSelectedItem(); if (sel != null) ajustarMinimo(sel); });
                itemEditar      .setOnAction(e -> { Componente sel = tablaStock.getSelectionModel().getSelectedItem(); if (sel != null) editarStock(sel); });
                itemToggleActivo.setOnAction(e -> { Componente sel = tablaStock.getSelectionModel().getSelectedItem(); if (sel != null) toggleActivarComponente(sel); });
            } else {
                ctxStock.getItems().add(itemSolicitar);
                tablaStock.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) ->
                        itemSolicitar.setDisable(sel == null));
            }

            itemSolicitar.setOnAction(e -> {
                Componente sel = tablaStock.getSelectionModel().getSelectedItem();
                if (sel != null) solicitarPieza(sel);
            });
            tablaStock.setContextMenu(ctxStock);
        }

        // Filtros: buscador + estado (MenuButton con CheckBoxes)

        cbOk          = new CheckBox("OK");
        cbBajo        = new CheckBox("Bajo");
        cbSinStock    = new CheckBox("Sin stock");
        cbDesactivado = new CheckBox("Desactivado");
        for (CheckBox cb : new CheckBox[]{cbOk, cbBajo, cbSinStock, cbDesactivado})
            cb.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");

        FilteredList<Componente> filtrada = new FilteredList<>(datosStock, c -> true);

        Runnable aplicarFiltros = () -> {
            boolean ok   = cbOk.isSelected();
            boolean baj  = cbBajo.isSelected();
            boolean sin  = cbSinStock.isSelected();
            boolean des  = cbDesactivado.isSelected();
            boolean ninguno = !ok && !baj && !sin && !des;
            actualizarTextoFiltroStock(ok, baj, sin, des, ninguno);
            filtrada.setPredicate(c -> {
                String texto = txtBuscador.getText();
                boolean coincideTexto = texto == null || texto.isBlank() ||
                        c.getTipo().toLowerCase().contains(texto.toLowerCase().trim());
                String est = estadoComponente(c);
                boolean coincideEstado = ninguno ||
                        (ok  && "OK".equals(est)) ||
                        (baj && "Bajo".equals(est)) ||
                        (sin && "Sin stock".equals(est)) ||
                        (des && "Desactivado".equals(est));
                return coincideTexto && coincideEstado;
            });
        };

        for (CheckBox cb : new CheckBox[]{cbOk, cbBajo, cbSinStock, cbDesactivado})
            cb.selectedProperty().addListener((obs, o, n) -> aplicarFiltros.run());
        txtBuscador.textProperty().addListener((obs, old, val) -> aplicarFiltros.run());

        CustomMenuItem itemOk  = new CustomMenuItem(cbOk,       false);
        CustomMenuItem itemBaj = new CustomMenuItem(cbBajo,     false);
        CustomMenuItem itemSin = new CustomMenuItem(cbSinStock, false);
        itemDesactivadoMenu    = new CustomMenuItem(cbDesactivado, false);
        itemDesactivadoMenu.setVisible(false);
        menuFiltroStock.getItems().addAll(itemOk, itemBaj, itemSin, itemDesactivadoMenu);

        tablaStock.setRowFactory(tv -> new TableRow<>() {
            {
                selectedProperty().addListener((obs, o, sel) -> actualizarEstilo());
            }
            private void actualizarEstilo() {
                if (isEmpty() || getItem() == null) { setStyle("-fx-border-width: 0 0 0 8; -fx-border-color: transparent;"); return; }
                if (!getItem().isActivo()) {
                    setStyle("-fx-opacity: 0.45; -fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0; -fx-border-color: transparent transparent " + com.reparaciones.utils.Colores.FILA_SEP + " transparent;");
                    return;
                }
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
            java.util.List<Componente> todos = componenteDAO.getAllGestionados();
            todos.sort(java.util.Comparator.comparingInt(c -> c.isActivo() ? 0 : 1));
            datosStock.setAll(todos);
            actualizarChart();
            long nDesactivados = todos.stream().filter(c -> !c.isActivo()).count();
            if (lblDesactivados != null) {
                lblDesactivados.setVisible(nDesactivados > 0);
                lblDesactivados.setManaged(nDesactivados > 0);
                lblDesactivados.setText(nDesactivados + " desactivado" + (nDesactivados == 1 ? "" : "s"));
            }
            if (itemDesactivadoMenu != null)
                itemDesactivadoMenu.setVisible(nDesactivados > 0);
            String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            if (lblUltimaActStock != null) lblUltimaActStock.setText("Actualizado " + hora);
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    private void actualizarChart() {
        long ok       = datosStock.stream().filter(c -> c.isActivo() && c.getStock() > c.getStockMinimo()).count();
        long bajo     = datosStock.stream().filter(c -> c.isActivo() && c.getStock() > 0 && c.getStock() <= c.getStockMinimo()).count();
        long sinStock = datosStock.stream().filter(c -> c.isActivo() && c.getStock() == 0).count();

        long total = ok + bajo + sinStock;
        lblTotalStock.setText(String.valueOf(total));
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
                leyendaItem("#3a7d44", "OK",        (int) ok),
                leyendaItem("#c77a00", "Bajo",      (int) bajo),
                leyendaItem(com.reparaciones.utils.Colores.ROJO_SIN_STOCK, "Sin stock", (int) sinStock)
        );
    }

    private javafx.scene.layout.HBox leyendaItem(String color, String nombre, int count) {
        javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(8, 8);
        rect.setFill(javafx.scene.paint.Color.web(color));
        Label lblNombre = new Label(nombre);
        lblNombre.setStyle("-fx-font-size: 9px; -fx-text-fill: #9AA0AA;");
        Label lblCount = new Label(String.valueOf(count));
        lblCount.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2C3B54;");
        javafx.scene.layout.VBox info = new javafx.scene.layout.VBox(0, lblNombre, lblCount);
        javafx.scene.layout.HBox hb = new javafx.scene.layout.HBox(6, rect, info);
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
                Tooltip tip = new Tooltip(String.valueOf(valor));
                tip.setShowDelay(javafx.util.Duration.millis(150));
                Tooltip.install(nodo, tip);
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

    private void editarStock(Componente c) {
        Label lblTitulo = new Label("Editar stock");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3B54;");

        Label lblActual = new Label("Componente: " + c.getTipo() + "   ·   Stock actual: " + c.getStock() + " ud(s).");
        lblActual.setStyle("-fx-font-size: 12px; -fx-text-fill: #586376;");

        Label lblNueva = new Label("Nueva cantidad");
        lblNueva.setStyle("-fx-font-size: 12px; -fx-text-fill: #586376; -fx-font-weight: bold;");

        TextField tfCantidad = new TextField(String.valueOf(c.getStock()));
        tfCantidad.setStyle("-fx-background-color: white; -fx-border-color: #C2C8D0;" +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;" +
                "-fx-text-fill: #2C3B54; -fx-font-size: 13px;");

        Label lblError = new Label();
        lblError.setStyle("-fx-font-size: 11px; -fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ERROR + ";");
        lblError.setVisible(false);

        Button btnConfirmar = new Button("Confirmar");
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);
        btnConfirmar.getStyleClass().add("btn-primary");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setMaxWidth(Double.MAX_VALUE);
        btnCancelar.getStyleClass().add("btn-secondary");

        javafx.scene.layout.HBox botones = new javafx.scene.layout.HBox(10, btnCancelar, btnConfirmar);
        botones.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        javafx.scene.layout.VBox contenido = new javafx.scene.layout.VBox(12,
                lblTitulo, lblActual, lblNueva, tfCantidad, lblError, botones);
        contenido.setPadding(new javafx.geometry.Insets(28));
        contenido.setPrefWidth(360);
        contenido.setStyle("-fx-background-color: #DDE1E7;");

        javafx.stage.Stage ventana = new javafx.stage.Stage();
        ventana.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        ventana.setResizable(false);
        ventana.setTitle("Editar stock — " + c.getTipo());

        btnCancelar.setOnAction(ev -> ventana.close());

        btnConfirmar.setOnAction(ev -> {
            try {
                int nueva = Integer.parseInt(tfCantidad.getText().trim());
                if (nueva < 0) throw new NumberFormatException();
                c.setStock(nueva);
                componenteDAO.actualizar(c);
                ventana.close();
                cargarStock();
            } catch (NumberFormatException ex) {
                lblError.setText("Cantidad no válida (debe ser ≥ 0).");
                lblError.setVisible(true);
            } catch (com.reparaciones.utils.StaleDataException ex) {
                ventana.close();
                new Alert(Alert.AlertType.WARNING, "El componente fue modificado mientras editabas. Recarga los datos.").showAndWait();
                cargarStock();
            } catch (SQLException ex) { mostrarError(ex); }
        });

        tfCantidad.setOnKeyPressed(ev -> { if (ev.getCode() == javafx.scene.input.KeyCode.ENTER) btnConfirmar.fire(); });

        javafx.scene.Scene scene = new javafx.scene.Scene(contenido);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        ventana.setScene(scene);
        javafx.application.Platform.runLater(tfCantidad::requestFocus);
        ventana.showAndWait();
    }

    private void toggleActivarComponente(Componente c) {
        try {
            componenteDAO.setActivo(c.getIdCom(), !c.isActivo());
            cargarStock();
        } catch (SQLException e) { mostrarError(e); }
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

    private void solicitarPieza(Componente c) {
        Label lblTitulo = new Label("Solicitar pieza");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3B54;");

        Label lblActual = new Label("Componente: " + c.getTipo() + "   ·   Stock actual: " + c.getStock() + " ud(s).");
        lblActual.setStyle("-fx-font-size: 12px; -fx-text-fill: #586376;");

        Label lblDesc = new Label("Descripción (opcional)");
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #586376; -fx-font-weight: bold;");

        javafx.scene.control.TextArea taDesc = new javafx.scene.control.TextArea();
        taDesc.setPromptText("Motivo o contexto de la solicitud...");
        taDesc.setPrefRowCount(3);
        taDesc.setWrapText(true);
        taDesc.setStyle("-fx-background-color: white; -fx-border-color: #C2C8D0;" +
                "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 6;" +
                "-fx-font-size: 13px; -fx-text-fill: #2C3B54;");

        Button btnConfirmar = new Button("Solicitar");
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);
        btnConfirmar.getStyleClass().add("btn-primary");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setMaxWidth(Double.MAX_VALUE);
        btnCancelar.getStyleClass().add("btn-secondary");

        javafx.scene.layout.HBox botones = new javafx.scene.layout.HBox(10, btnCancelar, btnConfirmar);
        botones.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        javafx.scene.layout.VBox contenido = new javafx.scene.layout.VBox(12,
                lblTitulo, lblActual, lblDesc, taDesc, botones);
        contenido.setPadding(new javafx.geometry.Insets(28));
        contenido.setPrefWidth(380);
        contenido.setStyle("-fx-background-color: #DDE1E7;");

        javafx.stage.Stage ventana = new javafx.stage.Stage();
        ventana.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        ventana.setResizable(false);
        ventana.setTitle("Solicitar pieza — " + c.getTipo());

        btnCancelar.setOnAction(ev -> ventana.close());
        btnConfirmar.setOnAction(ev -> {
            String desc = taDesc.getText().trim();
            try {
                solicitudStockDAO.insertar(c.getIdCom(), desc.isEmpty() ? null : desc);
                ventana.close();
            } catch (java.sql.SQLException ex) { mostrarError(ex); }
        });

        javafx.scene.Scene scene = new javafx.scene.Scene(contenido);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        ventana.setScene(scene);
        javafx.application.Platform.runLater(taDesc::requestFocus);
        ventana.showAndWait();
    }

    // ─── Configurar tabla Pedidos ─────────────────────────────────────────────

    private void configurarTablaPedidos() {
        cpId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleIntegerProperty(c.getValue().getIdCompra()).asObject());
        cpComponente.setCellValueFactory(c -> sp(c.getValue().getTipoComponente()));
        cpComponente.setCellFactory(col -> new TableCell<>() {
            private final Label lbl = new Label();
            {
                lbl.setStyle("-fx-cursor: hand; -fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ACCION + ";");
                lbl.setOnMouseEntered(e -> lbl.setStyle("-fx-cursor: hand; -fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ACCION + "; -fx-underline: true;"));
                lbl.setOnMouseExited(e  -> lbl.setStyle("-fx-cursor: hand; -fx-text-fill: " + com.reparaciones.utils.Colores.TEXTO_ACCION + "; -fx-underline: false;"));
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

        // Filtro por estado + buscador de componente
        FilteredList<CompraComponente> filtrada = new FilteredList<>(datosPedidos, p -> true);
        Runnable aplicarFiltroPedidos = () -> {
            actualizarTextoFiltroEstado();
            java.util.List<String> sel = cbsEstado.stream()
                    .filter(CheckBox::isSelected).map(CheckBox::getText)
                    .collect(java.util.stream.Collectors.toList());
            String texto = txtBuscadorPedidos.getText();
            java.time.LocalDate desde = dpPedidosDesde.getValue();
            java.time.LocalDate hasta = dpPedidosHasta.getValue();
            filtrada.setPredicate(p -> {
                boolean coincideEstado = sel.isEmpty() || sel.contains(p.getEstado().name());
                boolean coincideTexto  = texto == null || texto.isBlank() ||
                        p.getTipoComponente().toLowerCase().contains(texto.toLowerCase().trim());
                java.time.LocalDate fecha = p.getFechaPedido() != null ? p.getFechaPedido().toLocalDate() : null;
                boolean coincideDesde = desde == null || fecha == null || !fecha.isBefore(desde);
                boolean coincideHasta = hasta == null || fecha == null || !fecha.isAfter(hasta);
                return coincideEstado && coincideTexto && coincideDesde && coincideHasta;
            });
        };
        for (String estado : new String[]{"pendiente", "parcial", "recibido", "cancelado"}) {
            CheckBox cb = new CheckBox(estado);
            cb.setStyle("-fx-font-size: 12px; -fx-padding: 2 4 2 4;");
            cb.selectedProperty().addListener((obs, o, n) -> aplicarFiltroPedidos.run());
            cbsEstado.add(cb);
            menuFiltroEstado.getItems().add(new CustomMenuItem(cb, false));
        }
        txtBuscadorPedidos.textProperty().addListener((obs, o, n) -> aplicarFiltroPedidos.run());
        dpPedidosDesde.valueProperty().addListener((obs, o, n) -> aplicarFiltroPedidos.run());
        dpPedidosHasta.valueProperty().addListener((obs, o, n) -> aplicarFiltroPedidos.run());
        dpPedidosDesde.getEditor().setDisable(true);
        dpPedidosDesde.getEditor().setOpacity(1.0);
        dpPedidosHasta.getEditor().setDisable(true);
        dpPedidosHasta.getEditor().setOpacity(1.0);
        tablaPedidos.setItems(filtrada);

        // Menú contextual según estado de la fila (solo supertecnico)
        if (com.reparaciones.Sesion.esSuperTecnico()) {
            ContextMenu ctx = new ContextMenu();
            tablaPedidos.setContextMenu(ctx);
            tablaPedidos.getSelectionModel().selectedItemProperty().addListener(
                    (obs, old, sel) -> construirMenuContextual(ctx, sel));
        }
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
                MenuItem desrecibir = new MenuItem("Revertir a pendiente");
                MenuItem editar     = new MenuItem("Editar");
                desrecibir.setOnAction(e -> desrecibirPedido());
                editar    .setOnAction(e -> editarPedido());
                ctx.getItems().addAll(desrecibir, new SeparatorMenuItem(), editar);
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

    @FXML private void desrecibirPedido() {
        CompraComponente sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        int cantidadRevertir = sel.getCantidadRecibida() != null ? sel.getCantidadRecibida() : sel.getCantidad();
        ConfirmDialog.mostrar(
                "Desrecibir pedido",
                "¿Revertir el pedido #" + sel.getIdCompra() + " de " + sel.getTipoComponente() +
                " a pendiente?\nSe descontarán " + cantidadRevertir + " unidad(es) del stock.",
                "Desrecibir",
                () -> {
                    try {
                        compraDAO.desrecibir(sel);
                        cargarPedidos();
                        cargarStock();
                    } catch (com.reparaciones.utils.StaleDataException e) {
                        new Alert(Alert.AlertType.WARNING, e.getMessage()).showAndWait();
                        cargarPedidos(); cargarStock();
                    } catch (SQLException e) { mostrarError(e); }
                });
    }

    // ─── Configurar tabla Proveedores ─────────────────────────────────────────

    private void configurarTablaProveedores() {
        cpvNombre.setCellValueFactory(c -> sp(c.getValue().getNombre()));
        cpvDivisa.setCellValueFactory(c -> sp(c.getValue().getDivisa()));
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
                             "-fx-border-width: 0 0 1 8; -fx-border-insets: 1 0 0 0;");
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

        if (com.reparaciones.Sesion.esSuperTecnico()) {
            ContextMenu ctxProv = new ContextMenu();
            tablaProveedores.setContextMenu(ctxProv);
            tablaProveedores.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                ctxProv.getItems().clear();
                if (sel == null) return;
                MenuItem itemToggle = new MenuItem(sel.isActivo() ? "Desactivar" : "Activar");
                itemToggle.setOnAction(e -> activarProveedor());
                MenuItem itemDivisa = new MenuItem("Cambiar divisa");
                itemDivisa.setOnAction(e -> cambiarDivisa());
                ctxProv.getItems().addAll(itemToggle, itemDivisa);
                try {
                    if (!proveedorDAO.tienePedidos(sel.getIdProv())) {
                        MenuItem itemBorrar = new MenuItem("Borrar");
                        itemBorrar.setOnAction(e -> borrarProveedor());
                        ctxProv.getItems().add(itemBorrar);
                    }
                } catch (SQLException e) { mostrarError(e); }
            });
        }
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

    @FXML private void cambiarDivisa() {
        Proveedor sel = tablaProveedores.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Label lblTitulo = new Label("Divisa del proveedor");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3B54;");

        Label lblNombre = new Label(sel.getNombre());
        lblNombre.setStyle("-fx-font-size: 12px; -fx-text-fill: #586376;");

        Label lblLabel = new Label("Divisa por defecto");
        lblLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #586376; -fx-font-weight: bold;");

        ComboBox<String> cmbDiv = new ComboBox<>();
        cmbDiv.getItems().setAll("EUR", "USD");
        cmbDiv.setValue(sel.getDivisa());
        cmbDiv.setMaxWidth(Double.MAX_VALUE);

        Button btnConfirmar = new Button("Confirmar");
        btnConfirmar.setMaxWidth(Double.MAX_VALUE);
        btnConfirmar.getStyleClass().add("btn-primary");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setMaxWidth(Double.MAX_VALUE);
        btnCancelar.getStyleClass().add("btn-secondary");

        javafx.scene.layout.HBox botones = new javafx.scene.layout.HBox(10, btnCancelar, btnConfirmar);
        botones.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        javafx.scene.layout.VBox contenido = new javafx.scene.layout.VBox(12,
                lblTitulo, lblNombre, lblLabel, cmbDiv, botones);
        contenido.setPadding(new javafx.geometry.Insets(28));
        contenido.setPrefWidth(320);
        contenido.setStyle("-fx-background-color: #DDE1E7;");

        javafx.stage.Stage ventana = new javafx.stage.Stage();
        ventana.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        ventana.setResizable(false);
        ventana.setTitle("Divisa — " + sel.getNombre());

        btnCancelar.setOnAction(ev -> ventana.close());
        btnConfirmar.setOnAction(ev -> {
            try {
                proveedorDAO.setDivisa(sel.getIdProv(), cmbDiv.getValue());
                ventana.close();
                cargarProveedores();
            } catch (SQLException e) { mostrarError(e); }
        });

        javafx.scene.Scene scene = new javafx.scene.Scene(contenido);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
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
        if (!c.isActivo())                        return "Desactivado";
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

    private void actualizarTextoFiltroStock(boolean ok, boolean baj, boolean sin, boolean des, boolean ninguno) {
        int total = (ok ? 1 : 0) + (baj ? 1 : 0) + (sin ? 1 : 0) + (des ? 1 : 0);
        if (ninguno)         menuFiltroStock.setText("Estado");
        else if (total == 1) menuFiltroStock.setText(ok ? "OK" : baj ? "Bajo" : sin ? "Sin stock" : "Desactivado");
        else                 menuFiltroStock.setText(total + " estados");
    }

    private void mostrarError(SQLException e) {
        new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
    }

    private void mostrarConflicto() {
        new Alert(Alert.AlertType.WARNING,
                "Este pedido fue modificado por otro usuario. Los datos se han recargado.")
                .showAndWait();
    }

    // ─── Exportación CSV ──────────────────────────────────────────────────────

    @Override
    public void exportarCSV(Stage owner) {
        if (pnlStock.isVisible())       exportarStock(owner);
        else if (pnlPedidos.isVisible()) exportarPedidos(owner);
        else                             exportarProveedores(owner);
    }

    private void exportarStock(Stage owner) {
        List<String> cabeceras = List.of("Tipo", "Stock", "Stock mínimo", "Estado", "En camino", "Fecha registro");
        List<List<String>> filas = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Componente c : tablaStock.getItems()) {
            filas.add(List.of(
                    c.getTipo(),
                    String.valueOf(c.getStock()),
                    String.valueOf(c.getStockMinimo()),
                    estadoComponente(c),
                    String.valueOf(c.getEnCamino()),
                    c.getFechaRegistro() != null ? c.getFechaRegistro().format(fmt) : ""
            ));
        }
        com.reparaciones.utils.CsvExporter.exportar(owner, "stock_actual", cabeceras, filas);
    }

    private void exportarPedidos(Stage owner) {
        List<String> cabeceras = List.of(
                "ID", "Componente", "Cantidad", "Urgente", "Proveedor",
                "Estado", "Fecha pedido", "Fecha llegada", "Precio ud.", "Divisa", "Precio EUR", "Cant. recibida");
        List<List<String>> filas = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (CompraComponente p : tablaPedidos.getItems()) {
            filas.add(List.of(
                    String.valueOf(p.getIdCompra()),
                    p.getTipoComponente() != null ? p.getTipoComponente() : "",
                    String.valueOf(p.getCantidad()),
                    p.isEsUrgente() ? "Sí" : "No",
                    p.getNombreProveedor() != null ? p.getNombreProveedor() : "",
                    p.getEstado() != null ? p.getEstado().name() : "",
                    p.getFechaPedido() != null ? p.getFechaPedido().format(fmt) : "",
                    p.getFechaLlegada() != null ? p.getFechaLlegada().format(fmt) : "",
                    p.getPrecioUnidadPedido() > 0 ? String.valueOf(p.getPrecioUnidadPedido()) : "",
                    p.getDivisa() != null ? p.getDivisa() : "",
                    p.getPrecioEur() > 0 ? String.valueOf(p.getPrecioEur()) : "",
                    p.getCantidadRecibida() != null ? String.valueOf(p.getCantidadRecibida()) : ""
            ));
        }
        com.reparaciones.utils.CsvExporter.exportar(owner, "pedidos", cabeceras, filas);
    }

    private void exportarProveedores(Stage owner) {
        List<String> cabeceras = List.of("ID", "Nombre", "Activo");
        List<List<String>> filas = new ArrayList<>();
        for (Proveedor p : tablaProveedores.getItems()) {
            filas.add(List.of(
                    String.valueOf(p.getIdProv()),
                    p.getNombre(),
                    p.isActivo() ? "Sí" : "No"
            ));
        }
        com.reparaciones.utils.CsvExporter.exportar(owner, "proveedores", cabeceras, filas);
    }
}
