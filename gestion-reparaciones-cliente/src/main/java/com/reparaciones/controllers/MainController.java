package com.reparaciones.controllers;

import com.reparaciones.Sesion;
import com.reparaciones.dao.ComponenteDAO;
import com.reparaciones.dao.ReparacionComponenteDAO;
import com.reparaciones.dao.SolicitudStockDAO;
import com.reparaciones.models.Componente;
import com.reparaciones.models.SolicitudResumen;
import com.reparaciones.models.SolicitudStock;
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
    @FXML private StackPane  campanaPane;
    @FXML private ImageView  ivCampana;
    @FXML private StackPane  badgePane;
    @FXML private Label      lblBadge;

    private final Image imgCampanaOn  = new Image(getClass().getResourceAsStream("/images/NotfON.png"));
    private final Image imgCampanaOff = new Image(getClass().getResourceAsStream("/images/NotifOFF.png"));

    private final ReparacionComponenteDAO rcDAO             = new ReparacionComponenteDAO();
    private final SolicitudStockDAO       solicitudStockDAO = new SolicitudStockDAO();
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

    /**
     * Abre el panel flotante de solicitudes de pieza (solo admin).
     * <p>Muestra las solicitudes {@code PENDIENTE} y {@code RECHAZADA} con opciones
     * para gestionar, rechazar, recuperar o limpiar cada una.</p>
     */
    @FXML
    private void abrirSolicitudes() { abrirSolicitudes(false); }

    private void abrirSolicitudes(boolean abrirEnAlertas) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.initStyle(StageStyle.UNDECORATED);
        ventana.setResizable(false);

        // ── Tab bar ───────────────────────────────────────────────────────────
        Button btnTabSol   = new Button("Solicitudes");
        Button btnTabAlert = new Button("Alertas");
        btnTabSol  .setStyle(estiloTabActivo());
        btnTabAlert.setStyle(estiloTabInactivo());

        Label lblIrPedidos = new Label("→ Ir a pedidos");
        lblIrPedidos.setStyle("-fx-font-size: 12px; -fx-cursor: hand; -fx-text-fill: #001232; -fx-font-weight: bold;");
        lblIrPedidos.setOnMouseClicked(e -> { ventana.close(); mostrarStockEnPedidos(); });
        Label lblX = new Label("✕");
        lblX.setStyle("-fx-font-size: 14px; -fx-cursor: hand; -fx-text-fill: #586376;");
        lblX.setOnMouseClicked(e -> ventana.close());
        HBox spacerH = new HBox(); HBox.setHgrow(spacerH, Priority.ALWAYS);
        HBox tabBar = new HBox(4, btnTabSol, btnTabAlert, spacerH, lblIrPedidos, lblX);
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.setPadding(new Insets(0, 0, 8, 0));

        // ── Panel Solicitudes ─────────────────────────────────────────────────
        VBox listaPendientes = new VBox(6);
        VBox listaRechazadas = new VBox(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Label lblSeccion = new Label("Solicitudes de pieza");
        lblSeccion.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2C3B54;");
        Label lblRech = new Label("Rechazadas");
        lblRech.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #9AA0AA;");

        VBox contenidoSol = new VBox(8, lblSeccion, listaPendientes, lblRech, listaRechazadas);
        contenidoSol.setPadding(new Insets(4));
        ScrollPane scroll = new ScrollPane(contenidoSol);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(370);
        scroll.setStyle("-fx-background: #DDE1E7; -fx-background-color: #DDE1E7;");

        Button btnPedir = new Button("Pedir piezas");
        btnPedir.setMaxWidth(Double.MAX_VALUE);
        btnPedir.getStyleClass().add("btn-primary");
        HBox.setHgrow(btnPedir, Priority.ALWAYS);

        Button btnRechazarTodo = new Button("Rechazar todo");
        btnRechazarTodo.setMaxWidth(Double.MAX_VALUE);
        btnRechazarTodo.setStyle("-fx-background-color: #F5A0A0; -fx-text-fill: #7A2020;" +
                "-fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand;" +
                "-fx-font-weight: bold; -fx-padding: 10;");
        HBox.setHgrow(btnRechazarTodo, Priority.ALWAYS);

        HBox botones = new HBox(8, btnPedir, btnRechazarTodo);
        VBox panelSolicitudes = new VBox(8, scroll, botones);

        // ── Panel Alertas ─────────────────────────────────────────────────────
        List<Componente> sinStock = alertasCriticas.stream().filter(c -> c.getStock() == 0).collect(Collectors.toList());
        List<Componente> bajoMin  = alertasCriticas.stream().filter(c -> c.getStock() > 0).collect(Collectors.toList());
        VBox contenidoAlertas = new VBox(8);
        contenidoAlertas.setPadding(new Insets(4));
        if (alertasCriticas.isEmpty()) {
            Label lblSinAlertas = new Label("Sin alertas de stock");
            lblSinAlertas.setStyle("-fx-font-size: 13px; -fx-text-fill: #9AA0AA;");
            contenidoAlertas.getChildren().add(lblSinAlertas);
        } else {
            Label lblResumen = new Label(alertasCriticas.size() + " componente(s) requieren atención");
            lblResumen.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Colores.AZUL_GRIS + ";");
            contenidoAlertas.getChildren().add(lblResumen);
            if (!sinStock.isEmpty()) {
                Label lblSec = new Label("Sin stock (" + sinStock.size() + ")");
                lblSec.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + Colores.ROJO_SIN_STOCK + ";");
                VBox filas = new VBox(4);
                for (Componente c : sinStock) {
                    Label fila = new Label("• " + c.getTipo());
                    fila.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Colores.AZUL_NOCHE + ";");
                    filas.getChildren().add(fila);
                }
                ScrollPane scrollA = new ScrollPane(filas);
                scrollA.setFitToWidth(true); scrollA.setMaxHeight(140);
                scrollA.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                contenidoAlertas.getChildren().addAll(lblSec, scrollA);
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
                ScrollPane scrollA = new ScrollPane(filas);
                scrollA.setFitToWidth(true); scrollA.setMaxHeight(140);
                scrollA.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                contenidoAlertas.getChildren().addAll(lblSec, scrollA);
            }
        }
        ScrollPane scrollAlertas = new ScrollPane(contenidoAlertas);
        scrollAlertas.setFitToWidth(true);
        scrollAlertas.setPrefHeight(370);
        scrollAlertas.setStyle("-fx-background: #DDE1E7; -fx-background-color: #DDE1E7;");
        VBox panelAlertas = new VBox(8, scrollAlertas);
        panelAlertas.setVisible(false);
        panelAlertas.setManaged(false);

        // ── Recargar ──────────────────────────────────────────────────────────
        final Runnable[] recargarRef = { null };
        recargarRef[0] = () -> {
            listaPendientes.getChildren().clear();
            listaRechazadas.getChildren().clear();
            try {
                for (SolicitudResumen s : rcDAO.getSolicitudes("PENDIENTE"))
                    listaPendientes.getChildren().add(tarjetaSolicitud(s, ventana, fmt));
                for (SolicitudStock s : solicitudStockDAO.getSolicitudes("PENDIENTE"))
                    listaPendientes.getChildren().add(tarjetaSolicitudPreventiva(s, ventana, fmt));
                for (SolicitudResumen s : rcDAO.getSolicitudes("RECHAZADA"))
                    listaRechazadas.getChildren().add(tarjetaRechazada(s, ventana, fmt));
                for (SolicitudStock s : solicitudStockDAO.getSolicitudes("RECHAZADA"))
                    listaRechazadas.getChildren().add(tarjetaRechazadaPreventiva(s, ventana, fmt));
            } catch (SQLException ex) { mostrarError(ex); }
            actualizarBadge();
        };
        recargarRef[0].run();
        ventana.setUserData(recargarRef[0]);

        // ── Acciones ──────────────────────────────────────────────────────────
        btnPedir.setOnAction(e -> {
            try {
                List<SolicitudResumen> pendientes = rcDAO.getSolicitudes("PENDIENTE");
                if (pendientes.isEmpty()) return;
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

        btnRechazarTodo.setOnAction(e -> {
            try {
                for (SolicitudResumen s : rcDAO.getSolicitudes("PENDIENTE"))
                    rcDAO.actualizarEstadoSolicitud(s.getIdRc(), "RECHAZADA");
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

        // ── Raíz ──────────────────────────────────────────────────────────────
        VBox raiz = new VBox(12, tabBar, panelSolicitudes, panelAlertas);
        raiz.setPadding(new Insets(20));
        raiz.setPrefWidth(480);
        raiz.setStyle("-fx-background-color: #DDE1E7; -fx-border-color: #C4C9D4; -fx-border-width: 1;");

        final double[] drag = new double[2];
        raiz.setOnMousePressed(ev -> { drag[0] = ev.getSceneX(); drag[1] = ev.getSceneY(); });
        raiz.setOnMouseDragged(ev -> {
            ventana.setX(ev.getScreenX() - drag[0]);
            ventana.setY(ev.getScreenY() - drag[1]);
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
        ventana.showAndWait();
        actualizarBadge();
    }

    private static String estiloTabActivo() {
        return "-fx-background-color: #2C3B54; -fx-text-fill: white;" +
               "-fx-font-size: 12px; -fx-background-radius: 20; -fx-padding: 6 16 6 16;" +
               "-fx-cursor: hand; -fx-font-weight: bold;";
    }

    private static String estiloTabInactivo() {
        return "-fx-background-color: transparent; -fx-text-fill: #586376;" +
               "-fx-font-size: 12px; -fx-background-radius: 20; -fx-padding: 6 16 6 16;" +
               "-fx-cursor: hand; -fx-border-color: #C4C9D4; -fx-border-radius: 20; -fx-border-width: 1;";
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
                "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
        btnRechazar.setOnAction(e -> rechazarSolicitud(s.getIdRc(), ventana));

        HBox card = new HBox(10, icoPane, textos, btnRechazar);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 6;");

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
    private HBox tarjetaRechazada(SolicitudResumen s, Stage ventana, DateTimeFormatter fmt) {
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
        btnRecuperar.setStyle("-fx-background-color: #C8D8C8; -fx-text-fill: #2C4A2C;" +
                "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
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
        card.setStyle("-fx-background-color: #F0F1F3; -fx-background-radius: 6;");

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

    private HBox tarjetaSolicitudPreventiva(SolicitudStock s, Stage ventana, DateTimeFormatter fmt) {
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
                "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
        btnRechazar.setOnAction(e -> rechazarPreventiva(s.getIdSol(), ventana));

        HBox card = new HBox(10, icoPane, textos, btnRechazar);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 6;");

        ContextMenu ctx = new ContextMenu();
        MenuItem itemRechazarSol = new MenuItem("Rechazar solicitud");
        ctx.getItems().add(itemRechazarSol);
        itemRechazarSol.setOnAction(e -> rechazarPreventiva(s.getIdSol(), ventana));
        card.setOnContextMenuRequested(e -> ctx.show(card, e.getScreenX(), e.getScreenY()));

        return card;
    }

    private HBox tarjetaRechazadaPreventiva(SolicitudStock s, Stage ventana, DateTimeFormatter fmt) {
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
        btnRecuperar.setStyle("-fx-background-color: #C8D8C8; -fx-text-fill: #2C4A2C;" +
                "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
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
        card.setStyle("-fx-background-color: #F0F1F3; -fx-background-radius: 6;");

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

        Platform.runLater(() -> abrirSolicitudes(true));
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