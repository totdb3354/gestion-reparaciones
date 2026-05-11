package com.reparaciones.controllers;

import com.reparaciones.Sesion;
import com.reparaciones.dao.ComponenteDAO;
import com.reparaciones.dao.ReparacionComponenteDAO;
import com.reparaciones.models.Componente;
import com.reparaciones.models.SolicitudResumen;
import com.reparaciones.utils.Colores;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador raíz de la aplicación tras el login.
 * <p>Gestiona la barra de navegación superior y el {@code StackPane} central donde
 * se cargan dinámicamente las vistas secundarias (Reparaciones, Stock, Estadísticas).</p>
 *
 * <p><b>Responsabilidades principales:</b></p>
 * <ul>
 *   <li>Cargar la vista adecuada según el rol del usuario (admin / técnico).</li>
 *   <li>Delegar la recarga al controlador activo cuando la ventana recupera el foco.</li>
 *   <li>Pasar el callback {@link com.reparaciones.utils.Navegable} a {@link EstadisticasController}
 *       para habilitar la navegación desde el gráfico hasta el historial de reparaciones.</li>
 *   <li>Almacenar temporalmente el filtro de navegación ({@code filtroNav*}) y aplicarlo
 *       al cargar la vista de reparaciones.</li>
 *   <li>Mostrar el diálogo de alertas de stock al inicio si hay componentes bajo mínimo.</li>
 * </ul>
 *
 * @role ADMIN; TECNICO (con vistas distintas)
 */
public class MainController {

    @FXML private StackPane contenedor;
    @FXML private Button    btnReparaciones;
    @FXML private Button    btnStock;
    @FXML private Button    btnEstadisticas;
    @FXML private Button    btnUsuario;
    @FXML private Label     lblUsuario;
    @FXML private Label     lblAlertaStock;
    @FXML private StackPane  campanaPane;
    @FXML private ImageView  ivCampana;
    @FXML private StackPane  badgePane;
    @FXML private Label      lblBadge;

    private final Image imgCampanaOn  = new Image(getClass().getResourceAsStream("/images/NotfON.png"));
    private final Image imgCampanaOff = new Image(getClass().getResourceAsStream("/images/NotifOFF.png"));

    private final ReparacionComponenteDAO rcDAO = new ReparacionComponenteDAO();
    private ContextMenu menuUsuario;

    private List<Componente> alertasCriticas = List.of();
    private com.reparaciones.utils.Recargable controladorActivo;
    private Runnable accionVistaActual;
    private final java.util.Map<String, Object[]> vistaCache = new java.util.HashMap<>();

    // Filtro pendiente para la próxima carga de la vista de reparaciones
    private java.time.LocalDate filtroNavDesde;
    private java.time.LocalDate filtroNavHasta;
    private String              filtroNavTecnico;

    /**
     * Inicializa la barra de navegación, muestra la vista de reparaciones por defecto
     * y configura la recarga automática al recuperar el foco de la ventana.
     */
    @FXML
    public void initialize() {
        lblUsuario.setText("Hola, " + Sesion.getUsuario().getNombreUsuario());
        inicializarMenuUsuario();
        mostrarReparaciones();
        if (Sesion.esSuperTecnico()) {
            verificarStockAlertas();
            ivCampana.setImage(imgCampanaOff);
            campanaPane.setVisible(true);
            campanaPane.setManaged(true);
            actualizarBadge();
        }
        // Recargar al recuperar el foco de la ventana principal
        contenedor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            newScene.windowProperty().addListener((obs2, oldWin, win) -> {
                if (win == null) return;
                win.focusedProperty().addListener((obs3, wasFocused, isFocused) -> {
                    if (isFocused && controladorActivo != null) {
                        controladorActivo.recargar();
                        if (Sesion.esSuperTecnico()) actualizarBadge();
                    }
                });
            });
        });
    }

    /**
     * Actualiza el badge de notificaciones y la imagen de la campana.
     * <p>Cambia a {@code NotfON.png} y muestra el conteo si hay solicitudes
     * {@code PENDIENTE}; vuelve a {@code NotifOFF.png} y oculta el badge si no.</p>
     */
    void actualizarBadge() {
        try {
            int pendientes = rcDAO.contarSolicitudesPendientes();
            if (pendientes > 0) {
                ivCampana.setImage(imgCampanaOn);
                lblBadge.setText(String.valueOf(pendientes));
                badgePane.setVisible(true);
                badgePane.setManaged(true);
            } else {
                ivCampana.setImage(imgCampanaOff);
                badgePane.setVisible(false);
                badgePane.setManaged(false);
            }
        } catch (SQLException e) {
            // silencioso: polling de fondo
        }
    }

    /**
     * Abre el panel flotante de solicitudes de pieza (solo admin).
     * <p>Muestra las solicitudes {@code PENDIENTE} y {@code RECHAZADA} con opciones
     * para gestionar, rechazar, recuperar o limpiar cada una.</p>
     */
    @FXML
    private void abrirSolicitudes() {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.initStyle(StageStyle.UNDECORATED);
        ventana.setResizable(false);

        // ── Cabecera ──────────────────────────────────────────────────────────
        Label lblTitulo = new Label("Solicitudes de pieza");
        lblTitulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2C3B54;");
        Label lblX = new Label("✕");
        lblX.setStyle("-fx-font-size: 14px; -fx-cursor: hand; -fx-text-fill: #586376;");
        lblX.setOnMouseClicked(e -> ventana.close());
        Label lblIrPedidos = new Label("→ Ir a pedidos");
        lblIrPedidos.setStyle("-fx-font-size: 12px; -fx-cursor: hand; -fx-text-fill: #001232; -fx-font-weight: bold;");
        lblIrPedidos.setOnMouseClicked(e -> { ventana.close(); mostrarStockEnPedidos(); });
        HBox spacerH = new HBox(); HBox.setHgrow(spacerH, Priority.ALWAYS);
        HBox cabecera = new HBox(8, lblTitulo, spacerH, lblIrPedidos, lblX);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.setPadding(new Insets(0, 0, 12, 0));

        // ── Listas de solicitudes ─────────────────────────────────────────────
        VBox listaPendientes  = new VBox(6);
        VBox listaRechazadas  = new VBox(6);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Runnable recargar = () -> {
            listaPendientes.getChildren().clear();
            listaRechazadas.getChildren().clear();
            try {
                List<SolicitudResumen> pendientes  = rcDAO.getSolicitudes("PENDIENTE");
                List<SolicitudResumen> rechazadas  = rcDAO.getSolicitudes("RECHAZADA");

                for (SolicitudResumen s : pendientes)
                    listaPendientes.getChildren().add(tarjetaSolicitud(s, ventana, fmt));
                for (SolicitudResumen s : rechazadas)
                    listaRechazadas.getChildren().add(tarjetaRechazada(s, ventana, fmt));
            } catch (SQLException ex) { mostrarError(ex); }
        };

        // guardamos la referencia para poder llamarla desde los botones
        final Runnable[] recargarRef = { null };
        recargarRef[0] = () -> { recargar.run(); actualizarBadge(); };
        recargarRef[0].run();
        ventana.setUserData(recargarRef[0]);

        // ── Sección pendientes ────────────────────────────────────────────────
        Label lblPend = new Label("Pendientes");
        lblPend.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #586376;");

        // ── Sección rechazadas ────────────────────────────────────────────────
        Label lblRech = new Label("Rechazadas");
        lblRech.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #9AA0AA;");

        ScrollPane scroll = new ScrollPane();
        VBox contenido = new VBox(10, lblPend, listaPendientes, lblRech, listaRechazadas);
        contenido.setPadding(new Insets(4, 4, 4, 4));
        scroll.setContent(contenido);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);
        scroll.setStyle("-fx-background: #DDE1E7; -fx-background-color: #DDE1E7;");

        // ── Botón pedir piezas ────────────────────────────────────────────────
        Button btnPedir = new Button("Pedir piezas");
        btnPedir.setMaxWidth(Double.MAX_VALUE);
        btnPedir.getStyleClass().add("btn-primary");
        btnPedir.setOnAction(e -> {
            try {
                List<SolicitudResumen> pendientes = rcDAO.getSolicitudes("PENDIENTE");
                if (pendientes.isEmpty()) return;
                // paso 7: pre-rellenar FormularioCompra
                FormularioCompraController.abrirConSolicitudes(pendientes, () -> {
                    for (SolicitudResumen s : pendientes) {
                        try { rcDAO.actualizarEstadoSolicitud(s.getIdRc(), "GESTIONADA"); }
                        catch (SQLException ex) { mostrarError(ex); }
                    }
                    recargarRef[0].run();
                });
                ventana.close();
            } catch (SQLException ex) { mostrarError(ex); }
        });

        VBox raiz = new VBox(12, cabecera, scroll, btnPedir);
        raiz.setPadding(new Insets(20));
        raiz.setPrefWidth(480);
        raiz.setStyle("-fx-background-color: #DDE1E7; " +
                      "-fx-border-color: #C4C9D4; -fx-border-width: 1;");

        final double[] drag = new double[2];
        raiz.setOnMousePressed(ev  -> { drag[0] = ev.getSceneX(); drag[1] = ev.getSceneY(); });
        raiz.setOnMouseDragged(ev  -> {
            ventana.setX(ev.getScreenX() - drag[0]);
            ventana.setY(ev.getScreenY() - drag[1]);
        });

        Scene scene = new Scene(raiz);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
        actualizarBadge();
    }

    /**
     * Construye la tarjeta visual de una solicitud pendiente con botón de rechazo.
     *
     * @param s       datos de la solicitud
     * @param ventana ventana padre (para acceder al callback de recarga via {@code getUserData})
     * @param fmt     formateador de fecha
     * @return HBox listo para insertar en el panel de solicitudes
     */
    private HBox tarjetaSolicitud(SolicitudResumen s, Stage ventana, DateTimeFormatter fmt) {
        Label lblComp   = new Label(s.getTipoComponente());
        lblComp.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2C3B54;");
        Label lblInfo   = new Label(s.getNombreTecnico() + " · " + s.getIdRep());
        lblInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #586376;");
        Label lblFecha  = new Label(s.getFechaSolicitud().format(fmt));
        lblFecha.setStyle("-fx-font-size: 10px; -fx-text-fill: #9AA0AA;");
        String desc = s.getDescripcion() != null ? s.getDescripcion() : "";
        Label lblDesc   = new Label(desc);
        lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #586376;");
        lblDesc.setWrapText(true);

        VBox textos = new VBox(2, lblComp, lblInfo, lblDesc, lblFecha);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button btnRechazar = new Button("Rechazar");
        btnRechazar.setStyle("-fx-background-color: #F5A0A0; -fx-text-fill: #7A2020; " +
                             "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
        btnRechazar.setOnAction(e -> {
            try {
                rcDAO.actualizarEstadoSolicitud(s.getIdRc(), "RECHAZADA");
                actualizarBadge();
                // recargar panel
                ((Runnable) ventana.getUserData()).run();
            } catch (SQLException ex) { mostrarError(ex); }
        });

        HBox card = new HBox(10, textos, btnRechazar);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 6;");
        return card;
    }

    /**
     * Construye la tarjeta visual de una solicitud rechazada con opciones de recuperar y limpiar.
     * <p>Limpiar pone {@code ES_SOLICITUD = FALSE}, eliminando la solicitud del panel
     * sin borrar el registro histórico en BD.</p>
     *
     * @param s       datos de la solicitud rechazada
     * @param ventana ventana padre
     * @param fmt     formateador de fecha
     * @return HBox listo para insertar en el panel de solicitudes
     */
    private HBox tarjetaRechazada(SolicitudResumen s, Stage ventana, DateTimeFormatter fmt) {
        Label lblComp  = new Label(s.getTipoComponente());
        lblComp.setStyle("-fx-font-size: 12px; -fx-text-fill: #9AA0AA;");
        Label lblInfo  = new Label(s.getNombreTecnico() + " · " + s.getIdRep());
        lblInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #B0B5BF;");

        VBox textos = new VBox(2, lblComp, lblInfo);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button btnRecuperar = new Button("Recuperar");
        btnRecuperar.setStyle("-fx-background-color: #C8D8C8; -fx-text-fill: #2C4A2C; " +
                              "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
        btnRecuperar.setOnAction(e -> {
            try {
                rcDAO.actualizarEstadoSolicitud(s.getIdRc(), "PENDIENTE");
                actualizarBadge();
                ((Runnable) ventana.getUserData()).run();
            } catch (SQLException ex) { mostrarError(ex); }
        });

        ImageView ivBorrar = new ImageView(
                new Image(getClass().getResourceAsStream("/images/borrar.png")));
        ivBorrar.setFitWidth(18); ivBorrar.setFitHeight(18); ivBorrar.setPreserveRatio(true);
        ivBorrar.setStyle("-fx-cursor: hand; -fx-opacity: 0.5;");
        ivBorrar.setOnMouseClicked(e -> {
            try {
                rcDAO.limpiarSolicitud(s.getIdRc());
                ((Runnable) ventana.getUserData()).run();
            } catch (SQLException ex) { mostrarError(ex); }
        });

        HBox card = new HBox(10, textos, btnRecuperar, ivBorrar);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: #F0F1F3; -fx-background-radius: 6;");
        return card;
    }

    /**
     * Comprueba si hay componentes con stock bajo o sin stock y, de haberlos,
     * activa el indicador visual y muestra el diálogo de alertas al arrancar.
     * <p>Solo se llama para el rol ADMIN.</p>
     */
    private void verificarStockAlertas() {
        try {
            List<Componente> todos = new ComponenteDAO().getAllGestionados();
            alertasCriticas = todos.stream()
                    .filter(c -> c.getStock() <= c.getStockMinimo())
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            mostrarError(e);
            return;
        }
        if (alertasCriticas.isEmpty()) return;

        lblAlertaStock.setVisible(true);
        lblAlertaStock.setManaged(true);
        lblAlertaStock.setOnMouseClicked(e -> mostrarDialogoAlerta());
        Platform.runLater(this::mostrarDialogoAlerta);
    }

    /** Muestra el popup modal con la lista de componentes sin stock y bajo mínimo. */
    private void mostrarDialogoAlerta() {
        List<Componente> sinStock = alertasCriticas.stream().filter(c -> c.getStock() == 0).collect(Collectors.toList());
        List<Componente> bajoMin  = alertasCriticas.stream().filter(c -> c.getStock() > 0).collect(Collectors.toList());

        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.initStyle(StageStyle.UNDECORATED);
        ventana.setResizable(false);

        Label lblIcono = new Label("⚠");
        lblIcono.setStyle("-fx-font-size: 20px; -fx-text-fill: #D97B00;");

        Label lblTitulo = new Label("Alerta de stock");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + Colores.AZUL_MEDIO + ";");

        Label lblX = new Label("✕");
        lblX.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: " + Colores.AZUL_GRIS + ";");
        lblX.setOnMouseClicked(e -> ventana.close());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox cabecera = new HBox(8, lblIcono, lblTitulo, spacer, lblX);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.setPadding(new Insets(0, 0, 12, 0));

        Label lblResumen = new Label(alertasCriticas.size() + " componente(s) requieren atención");
        lblResumen.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Colores.AZUL_GRIS + ";");

        VBox cuerpo = new VBox(10, cabecera, lblResumen);

        if (!sinStock.isEmpty()) {
            Label lblSec = new Label("Sin stock (" + sinStock.size() + ")");
            lblSec.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Colores.ROJO_SIN_STOCK + ";");
            VBox filas = new VBox(4);
            for (Componente c : sinStock) {
                Label fila = new Label("• " + c.getTipo());
                fila.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Colores.AZUL_NOCHE + ";");
                filas.getChildren().add(fila);
            }
            ScrollPane scroll = new ScrollPane(filas);
            scroll.setFitToWidth(true);
            scroll.setMaxHeight(140);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            cuerpo.getChildren().addAll(lblSec, scroll);
        }

        if (!bajoMin.isEmpty()) {
            Label lblSec = new Label("Bajo mínimo (" + bajoMin.size() + ")");
            lblSec.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #D97B00;");
            VBox filas = new VBox(4);
            for (Componente c : bajoMin) {
                Label fila = new Label("• " + c.getTipo() + "   (" + c.getStock() + " uds.)");
                fila.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Colores.AZUL_NOCHE + ";");
                filas.getChildren().add(fila);
            }
            ScrollPane scroll = new ScrollPane(filas);
            scroll.setFitToWidth(true);
            scroll.setMaxHeight(140);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            cuerpo.getChildren().addAll(lblSec, scroll);
        }

        Button btnCerrar = new Button("Entendido");
        btnCerrar.setMaxWidth(Double.MAX_VALUE);
        btnCerrar.setStyle(
                "-fx-background-color: " + Colores.AZUL_NOCHE + "; -fx-text-fill: " + Colores.CREMA + ";" +
                "-fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 4;" +
                "-fx-padding: 10; -fx-cursor: hand;");
        btnCerrar.setOnAction(e -> ventana.close());
        cuerpo.getChildren().add(btnCerrar);

        cuerpo.setPadding(new Insets(24));
        cuerpo.setPrefWidth(360);
        cuerpo.setStyle("-fx-background-color: " + Colores.CREMA + ";" +
                "-fx-border-color: #C2C8D0; -fx-border-width: 1;");

        final double[] drag = new double[2];
        cuerpo.setOnMousePressed(ev  -> { drag[0] = ev.getSceneX(); drag[1] = ev.getSceneY(); });
        cuerpo.setOnMouseDragged(ev  -> {
            ventana.setX(ev.getScreenX() - drag[0]);
            ventana.setY(ev.getScreenY() - drag[1]);
        });

        Scene scene = new Scene(cuerpo);
        scene.setFill(Color.web(Colores.CREMA));
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    /** Navega a la vista de inicio según el rol (clickable desde el logo). */
    @FXML
    private void irAInicio() {
        mostrarReparaciones();
        String ruta = Sesion.esAdminOSuperTecnico()
                ? "/views/ReparacionViewAdmin.fxml"
                : "/views/ReparacionViewTecnico.fxml";
        Object[] cached = vistaCache.get(ruta);
        if (cached != null) {
            if (cached[1] instanceof ReparacionControllerAdmin rca)
                Platform.runLater(rca::irAInicio);
            else if (cached[1] instanceof ReparacionControllerTecnico rct)
                Platform.runLater(rct::irAInicio);
        }
    }

    /** Navega a la vista de reparaciones (admin o técnico según el rol). */
    @FXML
    private void mostrarReparaciones() {
        accionVistaActual = this::mostrarReparaciones;
        String vista = Sesion.esAdminOSuperTecnico()
                ? "/views/ReparacionViewAdmin.fxml"
                : "/views/ReparacionViewTecnico.fxml";
        mostrarVista(vista, btnReparaciones, btnStock, btnEstadisticas);
    }

    /** Navega a la vista de stock. */
    @FXML
    private void mostrarStock() {
        accionVistaActual = this::mostrarStock;
        mostrarVista("/views/StockView.fxml", btnStock, btnReparaciones, btnEstadisticas);
    }

    /** Navega a la vista de stock y abre directamente la sección de pedidos. */
    private void mostrarStockEnPedidos() {
        mostrarStock();
        Object[] cached = vistaCache.get("/views/StockView.fxml");
        if (cached != null && cached[1] instanceof StockController sc)
            Platform.runLater(sc::irAPedidos);
    }

    /** Navega a la vista de estadísticas. */
    @FXML
    private void mostrarEstadisticas() {
        accionVistaActual = this::mostrarEstadisticas;
        mostrarVista("/views/EstadisticasView.fxml", btnEstadisticas, btnReparaciones, btnStock);
    }

    /**
     * Construye el menú contextual del botón de usuario.
     * <p>Para ADMIN incluye "Gestionar técnicos". Para todos incluye
     * "Descargar CSV" (activo solo si la vista actual implementa {@link com.reparaciones.utils.Exportable})
     * y "Cerrar Sesión".</p>
     */
    private void inicializarMenuUsuario() {
        MenuItem itemDescargar = new MenuItem("Descargar CSV");
        SeparatorMenuItem sep  = new SeparatorMenuItem();
        MenuItem itemCerrar    = new MenuItem("Cerrar Sesión");

        itemDescargar.setOnAction(e -> descargarCSV());
        itemCerrar.setOnAction(e -> cerrarSesion());

        menuUsuario = new ContextMenu();
        if (Sesion.esAdmin()) {
            MenuItem itemGestionar = new MenuItem("Gestionar técnicos");
            itemGestionar.setOnAction(e -> abrirGestionTecnicos());
            MenuItem itemLogs = new MenuItem("Ver logs");
            itemLogs.setOnAction(e -> abrirLogs());
            menuUsuario.getItems().addAll(itemGestionar, itemLogs, new SeparatorMenuItem());
        }
        menuUsuario.getItems().addAll(itemDescargar, sep, itemCerrar);
    }

    /** Delega la exportación CSV al controlador activo si implementa {@link com.reparaciones.utils.Exportable}. */
    private void descargarCSV() {
        if (controladorActivo instanceof com.reparaciones.utils.Exportable exp) {
            exp.exportarCSV((Stage) btnUsuario.getScene().getWindow());
        }
    }

    /** Abre el modal de gestión de técnicos ({@code RegisterView.fxml}). */
    private void abrirGestionTecnicos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RegisterView.fxml"));
            Parent root = loader.load();
            Stage ventana = new Stage();
            ventana.initModality(Modality.APPLICATION_MODAL);
            ventana.initOwner(btnUsuario.getScene().getWindow());
            ventana.setTitle("Gestión de técnicos");
            ventana.setScene(new Scene(root));
            ventana.setResizable(false);
            ventana.showAndWait();
            if (accionVistaActual != null) accionVistaActual.run();
        } catch (IOException e) {
            mostrarError(e);
        }
    }

    /** Abre el modal de logs de actividad ({@code LogView.fxml}). */
    private void abrirLogs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LogView.fxml"));
            Parent root = loader.load();
            Stage ventana = new Stage();
            ventana.setTitle("Log de actividad");
            ventana.setScene(new Scene(root));
            ventana.setResizable(true);
            ventana.show();
        } catch (IOException e) {
            mostrarError(e);
        }
    }

    /** Despliega el menú contextual bajo el botón de usuario. */
    @FXML
    private void mostrarMenuUsuario() {
        menuUsuario.show(btnUsuario,
                javafx.geometry.Side.BOTTOM,
                0, 4);
    }

    /** Detiene el polling, limpia la sesión y vuelve a la pantalla de login. */
    @FXML
    private void cerrarSesion() {
        try {
            vistaCache.values().forEach(cached -> {
                if (cached[1] instanceof com.reparaciones.utils.Recargable r)
                    r.detenerPolling();
            });
            vistaCache.clear();
            Sesion.cerrar();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnUsuario.getScene().getWindow();

            stage.setMaximized(false);
            stage.setResizable(false);
            stage.setMinWidth(0);
            stage.setMinHeight(0);
            stage.setScene(new Scene(root));
            stage.setTitle("Gestión de Reparaciones — Login");
            stage.sizeToScene();
            stage.centerOnScreen();

        } catch (IOException e) {
            mostrarError(e);
        }
    }

    /**
     * Carga una vista FXML en el {@code StackPane} central, actualiza el estado visual de los
     * botones de navegación y gestiona el ciclo de vida del controlador anterior.
     * <p>Si la nueva vista es {@link EstadisticasController}, le inyecta el callback de
     * navegación. Si hay un filtro pendiente ({@code filtroNav*}) y la vista es de
     * reparaciones, lo aplica y lo limpia.</p>
     *
     * @param ruta      ruta al FXML de la vista a cargar (relativa a resources)
     * @param activo    botón de navegación que debe quedar marcado como activo
     * @param inactivos resto de botones que deben quedar inactivos
     */
    private void mostrarVista(String ruta, Button activo, Button... inactivos) {
        btnReparaciones.setDisable(true);
        btnStock.setDisable(true);
        btnEstadisticas.setDisable(true);

        try {
            Object[] cached = vistaCache.get(ruta);
            Node vista;
            Object ctrl;

            if (cached != null) {
                vista = (Node) cached[0];
                ctrl  = cached[1];
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
                vista = loader.load();
                ctrl  = loader.getController();

                // Pasar callback de navegación a EstadisticasController (solo primera carga)
                if (ctrl instanceof EstadisticasController ec) {
                    ec.setNavegacion((desde, hasta, tecnico) -> {
                        filtroNavDesde   = desde;
                        filtroNavHasta   = hasta;
                        filtroNavTecnico = tecnico;
                        mostrarReparaciones();
                    });
                }

                vistaCache.put(ruta, new Object[]{vista, ctrl});
            }

            if (ctrl instanceof com.reparaciones.utils.Recargable r)
                controladorActivo = r;

            // Filtro desde estadísticas: se aplica siempre que haya uno pendiente
            if (filtroNavDesde != null) {
                if (ctrl instanceof ReparacionControllerAdmin rca)
                    rca.setFiltroInicial(filtroNavDesde, filtroNavHasta, filtroNavTecnico);
                else if (ctrl instanceof ReparacionControllerTecnico rct)
                    rct.setFiltroInicial(filtroNavDesde, filtroNavHasta);
                filtroNavDesde = filtroNavHasta = null;
                filtroNavTecnico = null;
            } else if (cached != null && controladorActivo != null) {
                // Vista ya existente: refrescar datos sin tocar filtros
                controladorActivo.recargar();
            }

            contenedor.getChildren().setAll(vista);
            setActivo(activo, inactivos);
        } catch (IOException e) {
            mostrarError(e);
        } finally {
            btnReparaciones.setDisable(false);
            btnStock.setDisable(false);
            btnEstadisticas.setDisable(false);
        }
    }

    /**
     * Aplica las clases CSS {@code nav-btn-active} / {@code nav-btn} a los botones
     * de la barra de navegación para reflejar la sección actual.
     *
     * @param activo    botón que representa la vista activa
     * @param inactivos resto de botones de navegación
     */
    private void mostrarError(Exception e) {
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, e.getMessage()).showAndWait();
    }

    private void setActivo(Button activo, Button... inactivos) {
        activo.getStyleClass().remove("nav-btn");
        if (!activo.getStyleClass().contains("nav-btn-active"))
            activo.getStyleClass().add("nav-btn-active");
        for (Button inactivo : inactivos) {
            inactivo.getStyleClass().remove("nav-btn-active");
            if (!inactivo.getStyleClass().contains("nav-btn"))
                inactivo.getStyleClass().add("nav-btn");
        }
    }
}