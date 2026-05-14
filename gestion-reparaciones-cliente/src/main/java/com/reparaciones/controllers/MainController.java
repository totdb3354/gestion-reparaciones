package com.reparaciones.controllers;

import com.reparaciones.Sesion;
import com.reparaciones.dao.ComponenteDAO;
import com.reparaciones.dao.ReparacionComponenteDAO;
import com.reparaciones.dao.SolicitudStockDAO;
import com.reparaciones.models.Componente;
import com.reparaciones.models.SolicitudResumen;
import com.reparaciones.models.SolicitudStock;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;
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
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
    @FXML private StackPane  campanaPane;
    @FXML private ImageView  ivCampana;
    @FXML private StackPane  badgePane;
    @FXML private Label      lblBadge;

    private final Image imgCampanaOn  = new Image(getClass().getResourceAsStream("/images/NotfON.png"));
    private final Image imgCampanaOff = new Image(getClass().getResourceAsStream("/images/NotifOFF.png"));

    private final ReparacionComponenteDAO rcDAO             = new ReparacionComponenteDAO();
    private final SolicitudStockDAO       solicitudStockDAO = new SolicitudStockDAO();
    private ContextMenu menuUsuario;
    private Stage ventanaNotificaciones;

    private List<Componente> alertasCriticas = List.of();
    private com.reparaciones.utils.Recargable controladorActivo;
    private Timeline pulsoAlertas;
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
        // Recargar al recuperar el foco; abrir alertas la primera vez que la ventana se muestra
        contenedor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            newScene.windowProperty().addListener((obs2, oldWin, win) -> {
                if (win == null) return;
                if (!win.isShowing()) {
                    win.showingProperty().addListener((obs3, wasShowing, isShowing) -> {
                        if (isShowing && !alertasCriticas.isEmpty()) iniciarPulso();
                    });
                }
                win.focusedProperty().addListener((obs3, wasFocused, isFocused) -> {
                    if (isFocused && controladorActivo != null) {
                        controladorActivo.recargar();
                        if (Sesion.esSuperTecnico()) actualizarBadge();
                    }
                    if (isFocused && ventanaNotificaciones != null && ventanaNotificaciones.isShowing())
                        ((Runnable) ventanaNotificaciones.getUserData()).run();
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
            int total = rcDAO.contarSolicitudesPendientes() + solicitudStockDAO.contarPendientes();
            if (total > 0) {
                ivCampana.setImage(imgCampanaOn);
                lblBadge.setText(String.valueOf(total));
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

    private void iniciarPulso() {
        if (pulsoAlertas != null && pulsoAlertas.getStatus() == Timeline.Status.RUNNING) return;
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#F1E356"));
        glow.setRadius(0);
        campanaPane.setEffect(glow);
        pulsoAlertas = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(glow.radiusProperty(),       0),
                new KeyValue(campanaPane.scaleXProperty(), 1.0),
                new KeyValue(campanaPane.scaleYProperty(), 1.0)),
            new KeyFrame(Duration.millis(600),
                new KeyValue(glow.radiusProperty(),       40),
                new KeyValue(campanaPane.scaleXProperty(), 1.25),
                new KeyValue(campanaPane.scaleYProperty(), 1.25)),
            new KeyFrame(Duration.millis(1200),
                new KeyValue(glow.radiusProperty(),       0),
                new KeyValue(campanaPane.scaleXProperty(), 1.0),
                new KeyValue(campanaPane.scaleYProperty(), 1.0))
        );
        pulsoAlertas.setCycleCount(Timeline.INDEFINITE);
        pulsoAlertas.play();
    }

    private void detenerPulso() {
        if (pulsoAlertas != null) {
            pulsoAlertas.stop();
            pulsoAlertas = null;
            campanaPane.setEffect(null);
            campanaPane.setScaleX(1.0);
            campanaPane.setScaleY(1.0);
        }
    }

    /**
     * Abre el panel flotante de solicitudes de pieza (solo admin).
     * <p>Muestra las solicitudes {@code PENDIENTE} y {@code RECHAZADA} con opciones
     * para gestionar, rechazar, recuperar o limpiar cada una.</p>
     */
    @FXML
    private void abrirSolicitudes() { abrirSolicitudes(pulsoAlertas != null); }

    private void abrirSolicitudes(boolean abrirEnAlertas) {
        detenerPulso();
        if (ventanaNotificaciones != null && ventanaNotificaciones.isShowing()) {
            ventanaNotificaciones.close();
            return;
        }

        // ── Cargar estructura desde FXML ──────────────────────────────────────
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NotificacionesModal.fxml"));
        VBox raiz;
        try { raiz = loader.load(); } catch (IOException e) { mostrarError(e); return; }
        Map<String, Object> ns = loader.getNamespace();

        Button btnTabSol      = (Button) ns.get("btnTabSolicitudes");
        Button btnTabAlert    = (Button) ns.get("btnTabAlertas");
        Label  lblIrPedidos   = (Label)  ns.get("lblIrPedidos");
        VBox   panelSolicitudes = (VBox) ns.get("panelSolicitudes");
        VBox   panelAlertas     = (VBox) ns.get("panelAlertas");
        VBox   listaPendientes  = (VBox) ns.get("listaPendientes");
        VBox   listaRechazadas  = (VBox) ns.get("listaRechazadas");
        VBox   contenedorAlertas = (VBox) ns.get("contenedorAlertas");
        Button btnPedir         = (Button) ns.get("btnPedir");
        Button btnRechazarTodo  = (Button) ns.get("btnRechazarTodo");
        Button btnPedirTodas    = (Button) ns.get("btnPedirTodas");
        Button btnVerStock      = (Button) ns.get("btnVerStock");

        Stage ventana = new Stage();
        ventana.initOwner(campanaPane.getScene().getWindow());
        ventana.initStyle(StageStyle.UNDECORATED);
        ventana.setResizable(false);

        btnTabSol  .setStyle(estiloTabActivo());
        btnTabAlert.setStyle(estiloTabInactivo());
        lblIrPedidos.setOnMouseClicked(e -> { ventana.close(); mostrarStockEnPedidos(); });

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // ── Panel Alertas — poblar contenedor del FXML ────────────────────────
        final Runnable recargarAlertas = () -> {
            try {
                List<Componente> todos = new ComponenteDAO().getAllGestionados();
                alertasCriticas = todos.stream()
                        .filter(c -> c.getStock() <= c.getStockMinimo())
                        .collect(Collectors.toList());
            } catch (SQLException ex) { /* silencioso: polling de fondo */ }
            contenedorAlertas.getChildren().clear();
            List<Componente> sinStk = alertasCriticas.stream().filter(c -> c.getStock() == 0).collect(Collectors.toList());
            List<Componente> bajMin = alertasCriticas.stream().filter(c -> c.getStock() > 0).collect(Collectors.toList());
            int ia = 0;
            for (Componente c : sinStk)
                contenedorAlertas.getChildren().add(tarjetaAlerta(c, true,  ia++ % 2 != 0, ventana));
            for (Componente c : bajMin)
                contenedorAlertas.getChildren().add(tarjetaAlerta(c, false, ia++ % 2 != 0, ventana));
            if (contenedorAlertas.getChildren().isEmpty()) {
                Label lbl = new Label("Sin alertas de stock");
                lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #9AA0AA;");
                contenedorAlertas.getChildren().add(lbl);
            }
        };
        recargarAlertas.run();
        btnPedirTodas.setOnAction(e -> {
            if (alertasCriticas.isEmpty()) return;
            FormularioCompraController.abrirConComponentes(alertasCriticas, () -> {});
        });
        btnVerStock.setOnAction(e -> { ventana.close(); mostrarStock(); });

        // ── Recargar ──────────────────────────────────────────────────────────
        final Runnable[] recargarRef = { null };
        final java.util.concurrent.atomic.AtomicReference<java.util.Set<String>> snapshotRef =
                new java.util.concurrent.atomic.AtomicReference<>(java.util.Collections.emptySet());
        recargarRef[0] = () -> {
            try {
                List<SolicitudResumen> pendRC  = rcDAO.getSolicitudes("PENDIENTE");
                List<SolicitudStock>   pendSol = solicitudStockDAO.getSolicitudes("PENDIENTE");
                List<SolicitudResumen> rechRC  = rcDAO.getSolicitudes("RECHAZADA");
                List<SolicitudStock>   rechSol = solicitudStockDAO.getSolicitudes("RECHAZADA");

                java.util.Set<String> snapshot = new java.util.LinkedHashSet<>();
                pendRC .forEach(s -> snapshot.add("pr" + s.getIdRc()));
                pendSol.forEach(s -> snapshot.add("ps" + s.getIdSol()));
                rechRC .forEach(s -> snapshot.add("rr" + s.getIdRc()));
                rechSol.forEach(s -> snapshot.add("rs" + s.getIdSol()));

                if (snapshot.equals(snapshotRef.get())) { actualizarBadge(); return; }
                snapshotRef.set(snapshot);

                listaPendientes.getChildren().clear();
                listaRechazadas.getChildren().clear();
                int ip = 0;
                for (SolicitudResumen s : pendRC)
                    listaPendientes.getChildren().add(tarjetaSolicitud(s, ventana, fmt, ip++ % 2 != 0));
                for (SolicitudStock s : pendSol)
                    listaPendientes.getChildren().add(tarjetaSolicitudPreventiva(s, ventana, fmt, ip++ % 2 != 0));
                int ir = 0;
                for (SolicitudResumen s : rechRC)
                    listaRechazadas.getChildren().add(tarjetaRechazada(s, ventana, fmt, ir++ % 2 != 0));
                for (SolicitudStock s : rechSol)
                    listaRechazadas.getChildren().add(tarjetaRechazadaPreventiva(s, ventana, fmt, ir++ % 2 != 0));
            } catch (SQLException ex) { mostrarError(ex); }
            actualizarBadge();
        };
        recargarRef[0].run();
        ventana.setUserData((Runnable) () -> { recargarRef[0].run(); recargarAlertas.run(); });

        java.util.concurrent.ScheduledExecutorService poller =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "poller-notificaciones");
                t.setDaemon(true);
                return t;
            });
        poller.scheduleAtFixedRate(
            () -> Platform.runLater(() -> {
                recargarRef[0].run();
                recargarAlertas.run();
            }),
            60, 60, java.util.concurrent.TimeUnit.SECONDS);

        // ── Acciones ──────────────────────────────────────────────────────────
        btnPedir.setOnAction(e -> {
            try {
                List<SolicitudResumen> urgentes   = rcDAO.getSolicitudes("PENDIENTE");
                List<SolicitudStock>   preventivas = solicitudStockDAO.getSolicitudes("PENDIENTE");
                if (urgentes.isEmpty() && preventivas.isEmpty()) return;
                FormularioCompraController.abrirConSolicitudes(urgentes, preventivas, () -> {
                    for (SolicitudResumen s : urgentes) {
                        try { rcDAO.actualizarEstadoSolicitud(s.getIdRc(), "GESTIONADA"); }
                        catch (SQLException ex) { mostrarError(ex); }
                    }
                    for (SolicitudStock s : preventivas) {
                        try { solicitudStockDAO.actualizarEstado(s.getIdSol(), "GESTIONADA"); }
                        catch (SQLException ex) { mostrarError(ex); }
                    }
                    recargarRef[0].run();
                });
            } catch (SQLException ex) { mostrarError(ex); }
        });

        btnRechazarTodo.setOnAction(e -> {
            try {
                for (SolicitudResumen s : rcDAO.getSolicitudes("PENDIENTE"))
                    rcDAO.actualizarEstadoSolicitud(s.getIdRc(), "RECHAZADA");
                for (SolicitudStock s : solicitudStockDAO.getSolicitudes("PENDIENTE"))
                    solicitudStockDAO.actualizarEstado(s.getIdSol(), "RECHAZADA");
                recargarRef[0].run();
            } catch (SQLException ex) { mostrarError(ex); }
        });

        // ── Tab switching ─────────────────────────────────────────────────────
        btnTabSol.setOnAction(e -> {
            btnTabSol  .setStyle(estiloTabActivo());
            btnTabAlert.setStyle(estiloTabInactivo());
            panelSolicitudes.setVisible(true);  panelSolicitudes.setManaged(true);
            panelAlertas    .setVisible(false); panelAlertas    .setManaged(false);
        });
        btnTabAlert.setOnAction(e -> {
            btnTabSol  .setStyle(estiloTabInactivo());
            btnTabAlert.setStyle(estiloTabActivo());
            panelSolicitudes.setVisible(false); panelSolicitudes.setManaged(false);
            panelAlertas    .setVisible(true);  panelAlertas    .setManaged(true);
        });

        if (abrirEnAlertas) {
            btnTabSol  .setStyle(estiloTabInactivo());
            btnTabAlert.setStyle(estiloTabActivo());
            panelSolicitudes.setVisible(false); panelSolicitudes.setManaged(false);
            panelAlertas    .setVisible(true);  panelAlertas    .setManaged(true);
        }

        Scene scene = new Scene(raiz);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        ventana.setScene(scene);
        Stage mainStage = (Stage) campanaPane.getScene().getWindow();
        Runnable reposicionar = () -> {
            javafx.geometry.Bounds b = campanaPane.localToScreen(campanaPane.getBoundsInLocal());
            if (b == null) return;
            double x = b.getMaxX() - ventana.getWidth();
            double y = b.getMaxY() + 6;
            ventana.setX(Math.max(0, x));
            ventana.setY(y);
        };
        javafx.beans.value.ChangeListener<Number> moveListener   = (obs, o, n) -> reposicionar.run();
        javafx.beans.value.ChangeListener<Number> resizeListener = (obs, o, n) -> ventana.close();
        javafx.beans.value.ChangeListener<Boolean> iconListener  = (obs, o, n) -> { if (n) ventana.close(); };

        mainStage.xProperty().addListener(moveListener);
        mainStage.yProperty().addListener(moveListener);
        mainStage.widthProperty().addListener(resizeListener);
        mainStage.heightProperty().addListener(resizeListener);
        mainStage.iconifiedProperty().addListener(iconListener);

        ventana.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) Platform.runLater(() -> { recargarRef[0].run(); recargarAlertas.run(); });
        });

        EventHandler<MouseEvent> clickFueraFilter = e -> {
            javafx.geometry.Bounds b = campanaPane.localToScreen(campanaPane.getBoundsInLocal());
            if (b == null || !b.contains(e.getScreenX(), e.getScreenY())) ventana.close();
        };

        ventana.setOnShown(ev -> {
            reposicionar.run();
            mainStage.addEventFilter(MouseEvent.MOUSE_PRESSED, clickFueraFilter);
        });
        ventana.setOnHidden(ev -> {
            poller.shutdownNow();
            mainStage.removeEventFilter(MouseEvent.MOUSE_PRESSED, clickFueraFilter);
            mainStage.xProperty().removeListener(moveListener);
            mainStage.yProperty().removeListener(moveListener);
            mainStage.widthProperty().removeListener(resizeListener);
            mainStage.heightProperty().removeListener(resizeListener);
            mainStage.iconifiedProperty().removeListener(iconListener);
            actualizarBadge();
        });
        ventana.show();
        ventanaNotificaciones = ventana;
    }

    private static String estiloTabActivo() {
        return "-fx-background-color: #2C3B54; -fx-text-fill: white;" +
               "-fx-font-size: 12px; -fx-background-radius: 17; -fx-padding: 7 18 7 18;" +
               "-fx-cursor: hand; -fx-font-weight: bold;";
    }

    private static String estiloTabInactivo() {
        return "-fx-background-color: transparent; -fx-text-fill: #586376;" +
               "-fx-font-size: 12px; -fx-background-radius: 17; -fx-padding: 7 18 7 18;" +
               "-fx-cursor: hand;";
    }

    /**
     * Construye la tarjeta visual de una solicitud pendiente con botón de rechazo.
     *
     * @param s       datos de la solicitud
     * @param ventana ventana padre (para acceder al callback de recarga via {@code getUserData})
     * @param fmt     formateador de fecha
     * @return HBox listo para insertar en el panel de solicitudes
     */
    private HBox tarjetaSolicitud(SolicitudResumen s, Stage ventana, DateTimeFormatter fmt, boolean alterno) {
        // Icono circular con inicial del componente
        String inicial = s.getTipoComponente() != null && !s.getTipoComponente().isEmpty()
                ? s.getTipoComponente().substring(0, 1).toUpperCase() : "?";
        Label lblIco = new Label(inicial);
        lblIco.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #586376;");
        StackPane icoPane = new StackPane(lblIco);
        icoPane.setMinSize(36, 36); icoPane.setMaxSize(36, 36);
        icoPane.setStyle("-fx-background-color: #E8EAF0; -fx-background-radius: 50;");

        Label lblComp = new Label(s.getTipoComponente());
        lblComp.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2C3B54;");
        Label lblTag = new Label("⚠");
        lblTag.setStyle("-fx-font-size: 10px; -fx-text-fill: #D97B00; -fx-font-weight: bold;" +
                "-fx-background-color: #FFF3E0; -fx-background-radius: 4; -fx-padding: 1 5 1 5;");
        HBox compRow = new HBox(6, lblComp, lblTag);
        compRow.setAlignment(Pos.CENTER_LEFT);
        Label lblInfo = new Label(s.getNombreTecnico() + "  ·  " +
                s.getFechaSolicitud().format(fmt) + "  ·  " + s.getIdRep());
        lblInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #9AA0AA;");
        VBox textos = new VBox(3, compRow, lblInfo);
        if (s.getDescripcion() != null && !s.getDescripcion().isEmpty()) {
            Label lblDesc = new Label(s.getDescripcion());
            lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #586376;");
            lblDesc.setWrapText(true);
            textos.getChildren().add(lblDesc);
        }
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button btnRechazar = new Button("Rechazar");
        btnRechazar.setStyle("-fx-background-color: #F5A0A0; -fx-text-fill: #7A2020;" +
                "-fx-font-size: 11px; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 14 6 14;");
        btnRechazar.setOnAction(e -> rechazarSolicitud(s.getIdRc(), ventana));

        HBox card = new HBox(10, icoPane, textos, btnRechazar);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: " + (alterno ? "#F5F6F8" : "white") + "; -fx-background-radius: 6;");

        ContextMenu ctx = new ContextMenu();
        MenuItem itemRechazarSol = new MenuItem("Rechazar solicitud");
        ctx.getItems().add(itemRechazarSol);
        itemRechazarSol.setOnAction(e -> rechazarSolicitud(s.getIdRc(), ventana));
        card.setOnContextMenuRequested(e -> ctx.show(card, e.getScreenX(), e.getScreenY()));

        return card;
    }

    private void rechazarSolicitud(int idRc, Stage ventana) {
        try {
            rcDAO.actualizarEstadoSolicitud(idRc, "RECHAZADA");
            ((Runnable) ventana.getUserData()).run();
        } catch (SQLException ex) { mostrarError(ex); }
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
    private HBox tarjetaRechazada(SolicitudResumen s, Stage ventana, DateTimeFormatter fmt, boolean alterno) {
        String inicial = s.getTipoComponente() != null && !s.getTipoComponente().isEmpty()
                ? s.getTipoComponente().substring(0, 1).toUpperCase() : "?";
        Label lblIco = new Label(inicial);
        lblIco.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #B0B5BF;");
        StackPane icoPane = new StackPane(lblIco);
        icoPane.setMinSize(36, 36); icoPane.setMaxSize(36, 36);
        icoPane.setStyle("-fx-background-color: #EDEEF0; -fx-background-radius: 50;");

        Label lblComp = new Label(s.getTipoComponente());
        lblComp.setStyle("-fx-font-size: 13px; -fx-text-fill: #9AA0AA;");
        Label lblTagR = new Label("⚠");
        lblTagR.setStyle("-fx-font-size: 10px; -fx-text-fill: #C8A060; -fx-font-weight: bold;" +
                "-fx-background-color: #FFF8ED; -fx-background-radius: 4; -fx-padding: 1 5 1 5;");
        HBox compRowR = new HBox(6, lblComp, lblTagR);
        compRowR.setAlignment(Pos.CENTER_LEFT);
        Label lblInfo = new Label(s.getNombreTecnico() + "  ·  " + s.getIdRep());
        lblInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #B0B5BF;");
        VBox textos = new VBox(3, compRowR, lblInfo);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button btnRecuperar = new Button("Recuperar");
        btnRecuperar.setStyle("-fx-background-color: #2C3B54; -fx-text-fill: white;" +
                "-fx-font-size: 11px; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 14 6 14;");
        btnRecuperar.setOnAction(e -> recuperarSolicitud(s.getIdRc(), ventana));

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

        HBox card = new HBox(10, icoPane, textos, btnRecuperar, ivBorrar);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: " + (alterno ? "#E9EAEC" : "#F0F1F3") + "; -fx-background-radius: 6;");

        ContextMenu ctx = new ContextMenu();
        MenuItem itemRecuperarSol = new MenuItem("Recuperar solicitud");
        ctx.getItems().add(itemRecuperarSol);
        itemRecuperarSol.setOnAction(e -> recuperarSolicitud(s.getIdRc(), ventana));
        card.setOnContextMenuRequested(e -> ctx.show(card, e.getScreenX(), e.getScreenY()));

        return card;
    }

    private void recuperarSolicitud(int idRc, Stage ventana) {
        try {
            rcDAO.actualizarEstadoSolicitud(idRc, "PENDIENTE");
            ((Runnable) ventana.getUserData()).run();
        } catch (SQLException ex) { mostrarError(ex); }
    }

    private HBox tarjetaSolicitudPreventiva(SolicitudStock s, Stage ventana, DateTimeFormatter fmt, boolean alterno) {
        String inicial = s.getTipoComponente() != null && !s.getTipoComponente().isEmpty()
                ? s.getTipoComponente().substring(0, 1).toUpperCase() : "?";
        Label lblIco = new Label(inicial);
        lblIco.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #586376;");
        StackPane icoPane = new StackPane(lblIco);
        icoPane.setMinSize(36, 36); icoPane.setMaxSize(36, 36);
        icoPane.setStyle("-fx-background-color: #E8EAF0; -fx-background-radius: 50;");

        Label lblComp = new Label(s.getTipoComponente());
        lblComp.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2C3B54;");
        Label lblInfo = new Label(s.getNombreUsuario() + "  ·  " + s.getFecha().format(fmt));
        lblInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #9AA0AA;");
        VBox textos = new VBox(3, lblComp, lblInfo);
        if (s.getDescripcion() != null && !s.getDescripcion().isEmpty()) {
            Label lblDesc = new Label(s.getDescripcion());
            lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #586376;");
            lblDesc.setWrapText(true);
            textos.getChildren().add(lblDesc);
        }
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button btnRechazar = new Button("Rechazar");
        btnRechazar.setStyle("-fx-background-color: #F5A0A0; -fx-text-fill: #7A2020;" +
                "-fx-font-size: 11px; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 14 6 14;");
        btnRechazar.setOnAction(e -> rechazarPreventiva(s.getIdSol(), ventana));

        HBox card = new HBox(10, icoPane, textos, btnRechazar);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: " + (alterno ? "#F5F6F8" : "white") + "; -fx-background-radius: 6;");

        ContextMenu ctx = new ContextMenu();
        MenuItem itemRechazarSol = new MenuItem("Rechazar solicitud");
        ctx.getItems().add(itemRechazarSol);
        itemRechazarSol.setOnAction(e -> rechazarPreventiva(s.getIdSol(), ventana));
        card.setOnContextMenuRequested(e -> ctx.show(card, e.getScreenX(), e.getScreenY()));

        return card;
    }

    private HBox tarjetaRechazadaPreventiva(SolicitudStock s, Stage ventana, DateTimeFormatter fmt, boolean alterno) {
        String inicial = s.getTipoComponente() != null && !s.getTipoComponente().isEmpty()
                ? s.getTipoComponente().substring(0, 1).toUpperCase() : "?";
        Label lblIco = new Label(inicial);
        lblIco.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #B0B5BF;");
        StackPane icoPane = new StackPane(lblIco);
        icoPane.setMinSize(36, 36); icoPane.setMaxSize(36, 36);
        icoPane.setStyle("-fx-background-color: #EDEEF0; -fx-background-radius: 50;");

        Label lblComp = new Label(s.getTipoComponente());
        lblComp.setStyle("-fx-font-size: 13px; -fx-text-fill: #9AA0AA;");
        Label lblInfo = new Label(s.getNombreUsuario() + "  ·  " + s.getFecha().format(fmt));
        lblInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #B0B5BF;");
        VBox textos = new VBox(3, lblComp, lblInfo);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button btnRecuperar = new Button("Recuperar");
        btnRecuperar.setStyle("-fx-background-color: #2C3B54; -fx-text-fill: white;" +
                "-fx-font-size: 11px; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 14 6 14;");
        btnRecuperar.setOnAction(e -> recuperarPreventiva(s.getIdSol(), ventana));

        ImageView ivBorrar = new ImageView(
                new Image(getClass().getResourceAsStream("/images/borrar.png")));
        ivBorrar.setFitWidth(18); ivBorrar.setFitHeight(18); ivBorrar.setPreserveRatio(true);
        ivBorrar.setStyle("-fx-cursor: hand; -fx-opacity: 0.5;");
        ivBorrar.setOnMouseClicked(e -> {
            try {
                solicitudStockDAO.borrar(s.getIdSol());
                ((Runnable) ventana.getUserData()).run();
            } catch (SQLException ex) { mostrarError(ex); }
        });

        HBox card = new HBox(10, icoPane, textos, btnRecuperar, ivBorrar);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: " + (alterno ? "#E9EAEC" : "#F0F1F3") + "; -fx-background-radius: 6;");

        ContextMenu ctx = new ContextMenu();
        MenuItem itemRecuperarSol = new MenuItem("Recuperar solicitud");
        ctx.getItems().add(itemRecuperarSol);
        itemRecuperarSol.setOnAction(e -> recuperarPreventiva(s.getIdSol(), ventana));
        card.setOnContextMenuRequested(e -> ctx.show(card, e.getScreenX(), e.getScreenY()));

        return card;
    }

    private void rechazarPreventiva(int idSol, Stage ventana) {
        try {
            solicitudStockDAO.actualizarEstado(idSol, "RECHAZADA");
            ((Runnable) ventana.getUserData()).run();
        } catch (SQLException ex) { mostrarError(ex); }
    }

    private void recuperarPreventiva(int idSol, Stage ventana) {
        try {
            solicitudStockDAO.actualizarEstado(idSol, "PENDIENTE");
            ((Runnable) ventana.getUserData()).run();
        } catch (SQLException ex) { mostrarError(ex); }
    }

    private HBox tarjetaAlerta(Componente c, boolean sinStock, boolean alterno, Stage ventana) {
        Label lblIco = new Label(sinStock ? "✕" : "!");
        lblIco.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane icoPane = new StackPane(lblIco);
        icoPane.setMinSize(40, 40); icoPane.setMaxSize(40, 40);
        icoPane.setStyle("-fx-background-color: " + (sinStock ? "#E8504A" : "#E8903A") +
                "; -fx-background-radius: 50;");

        Label lblComp = new Label(c.getTipo());
        lblComp.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2C3B54;");
        String etiqueta  = sinStock ? "Sin Stock" : "Stock Bajo";
        String unidades  = sinStock ? "Sin unidades" : c.getStock() + " unid. restantes";
        Label lblEtiq = new Label(etiqueta);
        lblEtiq.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " +
                (sinStock ? "#E8504A" : "#E8903A") + ";");
        Label lblInfo = new Label(unidades);
        lblInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #9AA0AA;");
        HBox infoRow = new HBox(6, lblEtiq, lblInfo);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        VBox textos = new VBox(3, lblComp, infoRow);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button btnPedir = new Button("Pedir");
        btnPedir.setStyle("-fx-background-color: #2C3B54; -fx-text-fill: white;" +
                "-fx-font-size: 11px; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 16 6 16;");
        btnPedir.setOnAction(e -> {
            FormularioCompraController.abrir(c, () -> {});
        });

        HBox card = new HBox(12, icoPane, textos, btnPedir);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + (alterno ? "#F5F6F8" : "white") + "; -fx-background-radius: 6;");
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
            if (!alertasCriticas.isEmpty()) iniciarPulso();
        } catch (SQLException e) {
            mostrarError(e);
            return;
        }
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