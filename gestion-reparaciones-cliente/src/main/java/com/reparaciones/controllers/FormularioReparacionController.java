package com.reparaciones.controllers;

import com.reparaciones.Sesion;
import com.reparaciones.dao.ComponenteDAO;
import com.reparaciones.dao.ReparacionDAO;
import com.reparaciones.dao.TecnicoDAO;
import com.reparaciones.models.Componente;
import com.reparaciones.models.FilaReparacion;
import com.reparaciones.models.Tecnico;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Pos;
import javafx.util.Duration;

import com.reparaciones.utils.StaleDataException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador del formulario de finalización de reparación.
 * <p>Abierto desde {@link PendientesTecnicoController} al pulsar el botón de finalizar
 * de una asignación ({@code A*}). Permite al técnico seleccionar el componente utilizado,
 * indicar si es reutilizado, añadir observaciones y solicitar componentes adicionales.</p>
 *
 * <p>Agrupa los componentes en filas por tipo (batería, pantalla, etc.) y marca en verde
 * los tipos ya reparados en el mismo IMEI para evitar duplicados.</p>
 *
 * <p>Al guardar, llama a {@link com.reparaciones.dao.ReparacionDAO#insertarCompleta}
 * en transacción: crea los registros {@code R*} para las filas normales, guarda las
 * solicitudes sobre la {@code A*} y descuenta el stock.</p>
 *
 * @role TECNICO; ADMIN (si tiene técnico asignado)
 */
public class FormularioReparacionController {

    @FXML private Label lblImei;
    @FXML private Label lblIncidencia;
    @FXML private javafx.scene.layout.HBox filaIncidencia;
    @FXML private Label lblSeleccionaModelo;
    @FXML private VBox contenedorFilas;
    @FXML private Button btnGuardar;
    @FXML private javafx.scene.layout.HBox zonaGuardar;
    @FXML private ComboBox<String> cbFiltroModelo;
    private boolean tieneSolicitudesIniciales = false;
    private List<FilaReparacion> solicitudesCargadas = null;
    private boolean       modoEdicion = false;
    private String        idRepEditar;
    private String        imeiEditar;
    private int           idTecEditar;
    private LocalDateTime updatedAtEdicion;
    private boolean esperandoConfirmacion = false;
    private Timeline timelineReset;
    private int segundosRestantes;

    private final ReparacionDAO reparacionDAO = new ReparacionDAO();
    private final ComponenteDAO componenteDAO = new ComponenteDAO();

    private String imei;
    private String idRepAnterior;
    private String idAsignacion;
    private Runnable onGuardado;

    private final List<FilaUI> filasUI = new ArrayList<>();

    /**
     * Lista de modelos en orden de tienda Apple.
     * Para añadir iPhone 17 en el futuro:
     * agregar "17", "17plus", "17pro", "17promax" al final
     * No hace falta tocar nada más — traducirModelo lo traduce automáticamente.
     */
    static final List<String> MODELOS_ORDENADOS = List.of(
            "6s", "6splus", "7", "7plus", "8", "8plus", "se2020",
            "x", "xr", "xs", "xsmax",
            "11", "11pro", "11promax",
            "12", "12mini", "12pro", "12promax",
            "13", "13mini", "13pro", "13promax",
            "14", "14plus", "14pro", "14promax",
            "15", "15plus", "15pro", "15promax",
            "16", "16e", "16plus", "16pro", "16promax"
    // Ejemplo futuro: "17", "17plus", "17pro", "17promax"
    );

    public void init(String imei, String idRepAnterior, String idAsignacion, Runnable onGuardado) {
        this.imei = imei;
        this.idAsignacion = idAsignacion;
        this.onGuardado = onGuardado;

        this.idRepAnterior = idRepAnterior;
        if (this.idRepAnterior == null) {
            try {
                this.idRepAnterior = reparacionDAO.getIncidenciaActivaPorImei(imei);
            } catch (SQLException e) {
                mostrarError(e);
            }
        }

        lblImei.setText("IMEI: " + imei);
        if (this.idRepAnterior != null) {
            lblIncidencia.setText("⚠ Resuelve incidencia: " + this.idRepAnterior);
            filaIncidencia.setVisible(true);
            filaIncidencia.setManaged(true);
        }

        cargarFilas();
        if (idAsignacion != null) {
            try {
                List<FilaReparacion> solicitudes = reparacionDAO.getSolicitudesPorAsignacion(idAsignacion);
                if (!solicitudes.isEmpty()) {
                    solicitudesCargadas = solicitudes; // todas (incluidas RECHAZADAS) para detección de modelo
                    boolean hayActivas = solicitudes.stream()
                            .anyMatch(s -> !"RECHAZADA".equals(s.getEstadoSolicitud()));
                    tieneSolicitudesIniciales = hayActivas;
                    // Primera pasada: activar solo las no-rechazadas
                    for (FilaReparacion sol : solicitudes) {
                        if ("RECHAZADA".equals(sol.getEstadoSolicitud())) continue;
                        for (FilaUI fila : filasUI)
                            fila.activarSolicitud(sol.getIdCom(), sol.getDescripcionSolicitud(),
                                    "PENDIENTE".equals(sol.getEstadoSolicitud()));
                    }
                }
            } catch (SQLException e) {
                mostrarError(e);
            }
        }
        configurarFiltroModelo(); // el auto-filtro de modelo dispara aplicarFiltroModelo → resetea solicitudActiva
        // Segunda pasada: re-activar solo las no-rechazadas después del reset del filtro
        if (solicitudesCargadas != null) {
            for (FilaReparacion sol : solicitudesCargadas) {
                if ("RECHAZADA".equals(sol.getEstadoSolicitud())) continue;
                for (FilaUI fila : filasUI)
                    fila.activarSolicitud(sol.getIdCom(), sol.getDescripcionSolicitud(),
                            "PENDIENTE".equals(sol.getEstadoSolicitud()));
            }
        }
    }

    public void initEditar(String idRep, Runnable onGuardado) {
        this.modoEdicion  = true;
        this.idRepEditar  = idRep;
        this.onGuardado   = onGuardado;

        try {
            ReparacionDAO.DetalleEdicion d = reparacionDAO.getDetalleEdicion(idRep);
            if (d == null) return;
            this.imeiEditar      = d.imei;
            this.idTecEditar     = d.idTec;
            this.updatedAtEdicion = d.updatedAt;

            lblImei.setText("IMEI: " + d.imei + "  ·  Editando " + idRep);
            btnGuardar.setText("Guardar cambios");

            List<Tecnico> tecnicos = new TecnicoDAO().getAllActivos();

            cargarFilas();
            Set<Integer> yaReparados = reparacionDAO.getIdComsYaReparados(d.imei, idRep);
            // Primera pasada: activar solo la fila que tiene el componente editado,
            // para que configurarFiltroModelo pueda detectar el modelo
            for (FilaUI fila : filasUI)
                if (fila.getSkus().stream().anyMatch(c -> c.getIdCom() == d.idCom))
                    fila.activarModoEdicion(d.idCom, d.esReutilizado, d.observacion, d.cantidad);
            configurarFiltroModelo();
            // Segunda pasada: re-aplicar tras el reset del filtro de modelo
            for (FilaUI fila : filasUI) {
                if (fila.getSkus().stream().anyMatch(c -> c.getIdCom() == d.idCom))
                    fila.activarModoEdicion(d.idCom, d.esReutilizado, d.observacion, d.cantidad);
                else if (!fila.isModoEdicion() && fila.getSkus().stream()
                        .anyMatch(c -> yaReparados.contains(c.getIdCom())))
                    fila.deshabilitarYaReparado();
                else if (!fila.isModoEdicion())
                    fila.configurarTecnicoEdicion(tecnicos, d.idTec);
            }
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    public static void abrirEditar(String idRep, Runnable onGuardado) {
        Platform.runLater(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        FormularioReparacionController.class.getResource(
                                "/views/FormularioReparacionView.fxml"));
                javafx.scene.Parent root = loader.load();
                FormularioReparacionController ctrl = loader.getController();

                Stage stage = new Stage();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setResizable(true);
                stage.setMinWidth(900);
                stage.setMinHeight(520);

                ctrl.initEditar(idRep, onGuardado);
                stage.setTitle("Editar reparación — " + idRep);
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.show();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            }
        });
    }

    private void cargarFilas() {
        try {
            Map<String, List<Componente>> grupos = componenteDAO.getAgrupadosPorTipo();
            Image imgBorrar  = new Image(getClass().getResourceAsStream("/images/borrar.png"));
            Image imgEditar  = new Image(getClass().getResourceAsStream("/images/editar.png"));
            for (Map.Entry<String, List<Componente>> entry : grupos.entrySet()) {
                if (entry.getValue().isEmpty())
                    continue;
                FilaUI fila = new FilaUI(entry.getKey(), entry.getValue(), imgBorrar, imgEditar);
                fila.setOnCambio(this::actualizarBoton);
                contenedorFilas.getChildren().add(fila.getRoot());
                filasUI.add(fila);
            }
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    private void configurarFiltroModelo() {
        Set<String> modelosEnBD = filasUI.stream()
                .flatMap(f -> f.getSkus().stream()
                        .map(c -> extraerModelo(c.getTipo(), f.getPrefijo())))
                .filter(m -> !m.isEmpty())
                .collect(Collectors.toSet());

        List<String> modelosFiltrados = MODELOS_ORDENADOS.stream()
                .filter(modelosEnBD::contains)
                .collect(Collectors.toList());

        cbFiltroModelo.getItems().addAll(modelosFiltrados);
        cbFiltroModelo.setPromptText("— Selecciona modelo —");

        cbFiltroModelo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String m, boolean empty) {
                super.updateItem(m, empty);
                setText((empty || m == null) ? null : traducirModelo(m));
            }
        });
        cbFiltroModelo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String m, boolean empty) {
                super.updateItem(m, empty);
                setText((empty || m == null) ? null : traducirModelo(m));
            }
        });

        cbFiltroModelo.valueProperty().addListener((obs, o, n) -> {
            boolean hayModelo = n != null;
            if (!tieneSolicitudesIniciales && !modoEdicion) {
                contenedorFilas.setVisible(hayModelo);
                contenedorFilas.setManaged(hayModelo);
                lblSeleccionaModelo.setVisible(!hayModelo);
                lblSeleccionaModelo.setManaged(!hayModelo);
            }
            for (FilaUI fila : filasUI) {
                fila.aplicarFiltroModelo(n);
            }
        });

        if (solicitudesCargadas != null && !solicitudesCargadas.isEmpty()) {
            boolean hayPendientes = filasUI.stream().anyMatch(FilaUI::isSolicitud);
            solicitudesCargadas.stream()
                    .flatMap(sol -> filasUI.stream()
                        .filter(f -> f.getSkus().stream().anyMatch(c -> c.getIdCom() == sol.getIdCom()))
                        .map(f -> f.getSkus().stream()
                            .filter(c -> c.getIdCom() == sol.getIdCom())
                            .findFirst()
                            .map(c -> extraerModelo(c.getTipo(), f.getPrefijo()))
                            .orElse("")))
                    .filter(m -> !m.isEmpty() && cbFiltroModelo.getItems().contains(m))
                    .findFirst()
                    .ifPresent(modelo -> {
                        cbFiltroModelo.setValue(modelo);
                        cbFiltroModelo.setDisable(hayPendientes);
                    });
        } else if (modoEdicion) {
            // Auto-detectar modelo desde la fila en edición y bloquearlo
            filasUI.stream()
                    .filter(FilaUI::isModoEdicion)
                    .map(f -> f.getComponenteSeleccionado() != null
                            ? extraerModelo(f.getComponenteSeleccionado().getTipo(), f.getPrefijo()) : "")
                    .filter(m -> !m.isEmpty() && cbFiltroModelo.getItems().contains(m))
                    .findFirst()
                    .ifPresent(modelo -> {
                        cbFiltroModelo.setValue(modelo);
                        cbFiltroModelo.setDisable(true);
                    });
        } else {
            // Flujo normal: ocultar filas hasta que se seleccione modelo
            contenedorFilas.setVisible(false);
            contenedorFilas.setManaged(false);
            lblSeleccionaModelo.setVisible(true);
            lblSeleccionaModelo.setManaged(true);
        }
    }

    private void actualizarBoton() {
        boolean habilitado;
        if (modoEdicion) {
            boolean filaEditadaInvalida = filasUI.stream()
                    .filter(FilaUI::isModoEdicion)
                    .anyMatch(f -> f.hayCambio() && !f.tieneUso());
            boolean cambioEnEdicion = filasUI.stream()
                    .filter(FilaUI::isModoEdicion)
                    .anyMatch(f -> f.hayCambio() && f.tieneUso());
            boolean filasNuevas = filasUI.stream()
                    .filter(f -> !f.isModoEdicion()).anyMatch(FilaUI::isActiva);
            habilitado = !filaEditadaInvalida && (cambioEnEdicion || filasNuevas);
        } else {
            boolean activa = filasUI.stream().anyMatch(FilaUI::isActiva);
            boolean solicitudCancelada = tieneSolicitudesIniciales
                    && filasUI.stream().anyMatch(FilaUI::isSolicitudCancelada);
            habilitado = activa || solicitudCancelada;
        }
        zonaGuardar.setVisible(habilitado);
        zonaGuardar.setManaged(habilitado);
    }

    @FXML
    private void guardar() {
        if (!esperandoConfirmacion) {
            activarConfirmacion();
        } else {
            ejecutarGuardar();
        }
    }

    private void activarConfirmacion() {
        esperandoConfirmacion = true;
        btnGuardar.setDisable(true);
        segundosRestantes = 5;
        btnGuardar.setText("Guardar (" + segundosRestantes + ")");

        timelineReset = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segundosRestantes--;
            if (segundosRestantes > 0) {
                btnGuardar.setText("Guardar (" + segundosRestantes + ")");
            } else {
                timelineReset.stop();
                btnGuardar.setDisable(false);
                btnGuardar.setText("✓  Confirmar guardar");
            }
        }));
        timelineReset.setCycleCount(5);
        timelineReset.play();
    }

    private void ejecutarGuardar() {
        if (timelineReset != null) { timelineReset.stop(); timelineReset = null; }
        esperandoConfirmacion = false;

        if (modoEdicion) {
            ejecutarGuardarEdicion();
        } else {
            ejecutarGuardarNueva();
        }
    }

    private void ejecutarGuardarNueva() {
        // 1. Filas agotadas: descontar stock + crear solicitud, sin crear R*
        for (FilaUI fila : filasUI) {
            if (!fila.isAgotado()) continue;
            try {
                reparacionDAO.agotarComponente(
                        idAsignacion,
                        fila.getIdComSeleccionado(),
                        fila.getCantidad(),
                        fila.getDescripcionAgotado());
            } catch (StaleDataException ex) {
                new Alert(Alert.AlertType.WARNING,
                        "No se pudo registrar componente agotado: " + ex.getMessage()).showAndWait();
                return;
            } catch (SQLException ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Error al registrar componente agotado: " + ex.getMessage()).showAndWait();
                return;
            }
        }

        // 2. Filas normales → insertarCompleta
        List<FilaReparacion> filasActivas = new ArrayList<>();
        for (FilaUI fila : filasUI) {
            if (fila.isAgotado()) continue;
            if (fila.isActiva()) {
                boolean esSolicitudNueva = fila.isSolicitud() && fila.isSolicitudNueva();
                boolean tieneUso = fila.getCantidad() > 0 || fila.isReutilizado();
                if (esSolicitudNueva && tieneUso) {
                    // Fila con componente usado Y aviso de solicitud: emitir dos entradas separadas
                    filasActivas.add(new FilaReparacion(
                            fila.getIdComSeleccionado(),
                            fila.getCantidad(),
                            fila.isReutilizado(),
                            fila.getObservacion(),
                            fila.getPrefijo(),
                            false,
                            null,
                            null));
                    filasActivas.add(new FilaReparacion(
                            fila.getIdComSeleccionado(),
                            1,
                            false,
                            null,
                            fila.getPrefijo(),
                            true,
                            fila.getDescripcionSolicitud(),
                            null));
                } else {
                    filasActivas.add(new FilaReparacion(
                            fila.getIdComSeleccionado(),
                            fila.getCantidad(),
                            fila.isReutilizado(),
                            fila.getObservacion(),
                            fila.getPrefijo(),
                            esSolicitudNueva,
                            fila.getDescripcionSolicitud(),
                            null));
                }
            }
        }

        // Si solo había filas agotadas, cerrar sin crear reparación
        boolean soloAgotadas = filasActivas.isEmpty()
                && filasUI.stream().anyMatch(FilaUI::isAgotado);
        if (soloAgotadas) {
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();
            if (onGuardado != null)
                onGuardado.run();
            return;
        }

        try {
            reparacionDAO.insertarCompleta(filasActivas, imei,
                    Sesion.getIdTec(), idRepAnterior, idAsignacion);
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();
            if (onGuardado != null)
                onGuardado.run();
        } catch (StaleDataException ex) {
            new Alert(Alert.AlertType.WARNING,
                    "No se pudo guardar: " + ex.getMessage() + "\n" +
                    "Cierra el formulario y comprueba el estado de la asignación.")
                    .showAndWait();
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo guardar: " + ex.getMessage()).showAndWait();
        }
    }

    private void ejecutarGuardarEdicion() {
        try {
            // 1. Editar la fila que está en modo edición (si cambió)
            for (FilaUI fila : filasUI) {
                if (fila.isModoEdicion() && fila.hayCambio()) {
                    reparacionDAO.editarReparacion(
                            idRepEditar,
                            fila.getIdComSeleccionado(),
                            fila.isReutilizado(),
                            fila.getObservacion(),
                            fila.getCantidad(),
                            updatedAtEdicion);
                    break;
                }
            }
            // 2. Insertar filas nuevas agrupadas por técnico
            Map<Integer, List<FilaReparacion>> porTecnico = new java.util.LinkedHashMap<>();
            for (FilaUI fila : filasUI) {
                if (!fila.isModoEdicion() && fila.isActiva()) {
                    int idTec = fila.getTecnicoId() != -1 ? fila.getTecnicoId() : idTecEditar;
                    porTecnico.computeIfAbsent(idTec, k -> new ArrayList<>()).add(
                            new FilaReparacion(
                                    fila.getIdComSeleccionado(), fila.getCantidad(),
                                    fila.isReutilizado(), fila.getObservacion(),
                                    fila.getPrefijo(), fila.isSolicitud(),
                                    fila.getDescripcionSolicitud(), null));
                }
            }
            for (Map.Entry<Integer, List<FilaReparacion>> entry : porTecnico.entrySet()) {
                reparacionDAO.insertarCompleta(entry.getValue(), imeiEditar,
                        entry.getKey(), null, null);
            }
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();
            if (onGuardado != null)
                onGuardado.run();
        } catch (StaleDataException ex) {
            new Alert(Alert.AlertType.WARNING,
                    "No se pudo guardar: otro usuario modificó esta reparación.\n" +
                    "Cierra y vuelve a abrir el formulario para ver los cambios actuales.")
                    .showAndWait();
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo guardar: " + ex.getMessage()).showAndWait();
        }
    }

    public static void abrir(String imei, String idRepAnterior,
            String idAsignacion, Runnable onGuardado) {
        Platform.runLater(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        FormularioReparacionController.class.getResource(
                                "/views/FormularioReparacionView.fxml"));
                javafx.scene.Parent root = loader.load();
                FormularioReparacionController ctrl = loader.getController();

                Stage stage = new Stage();
                stage.setTitle("Nueva reparación — IMEI " + imei);
                stage.setScene(new javafx.scene.Scene(root));
                stage.setResizable(true);
                stage.setMinWidth(900);
                stage.setMinHeight(520);

                ctrl.init(imei, idRepAnterior, idAsignacion, onGuardado);

                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.show();

            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
            }
        });
    }

    private void mostrarError(Exception e) {
        new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
    }

    // ─── Utilidades estáticas ─────────────────────────────────────────────────

    /**
     * Extrae el modelo del SKU buscando qué modelo conocido aparece
     * al inicio del SKU tras quitar el prefijo y la 'i' de iPhone.
     * Compatible con cualquier sufijo nuevo sin tocar el código.
     */
    static String extraerModelo(String sku, String prefijo) {
        String s = sku.toLowerCase().substring(prefijo.length());
        if (s.startsWith("i"))
            s = s.substring(1);
        final String resto = s;
        return MODELOS_ORDENADOS.stream()
                .sorted((a, b) -> b.length() - a.length())
                .filter(resto::startsWith)
                .findFirst()
                .orElse("");
    }

    /**
     * Traduce el modelo interno del SKU al nombre comercial legible.
     *
     * Modelos numéricos (11, 12, 13, 14, 15, 16...):
     * Se traducen automáticamente — no hace falta añadir casos nuevos.
     * Ejemplo futuro: "17" → "iPhone 17" (automático)
     * "17plus" → "iPhone 17 Plus" (automático)
     * "17pro" → "iPhone 17 Pro" (automático)
     * "17promax" → "iPhone 17 Pro Max" (automático)
     *
     * Modelos especiales sin número (x, xs, xsmax, xr, se2020):
     * Hardcodeados porque son históricos y no van a cambiar.
     */
    static String traducirModelo(String modelo) {
        if (modelo == null || modelo.isEmpty())
            return modelo;
        return switch (modelo) {
            case "se2020" -> "iPhone SE 2020";
            case "x" -> "iPhone X";
            case "xr" -> "iPhone XR";
            case "xs" -> "iPhone XS";
            case "xsmax" -> "iPhone XS Max";
            // Casos especiales con letra sin número claro
            case "6s" -> "iPhone 6S";
            case "6splus" -> "iPhone 6S Plus";
            default -> {
                String num = modelo.replaceAll("[^0-9]", "");
                String variante = modelo.replaceAll("[0-9]", "");
                String sufijo = switch (variante) {
                    case "plus" -> " Plus";
                    case "mini" -> " Mini";
                    case "pro" -> " Pro";
                    case "promax" -> " Pro Max";
                    case "e" -> "e";
                    default -> "";
                };
                yield "iPhone " + num + sufijo;
            }
        };
    }

    // ─── FilaUI ───────────────────────────────────────────────────────────────

    static class FilaUI {

        private final String prefijo;
        private final List<Componente> skus;
        private final VBox root;

        private final Label lblContador;
        private final Button btnMas;
        private final Button btnMenos;
        private final ComboBox<Componente> cbSku;
        private final Label lblStock;
        private final CheckBox chkReutilizado;
        private final Button btnObservacion;
        private final Label lblObservacion;
        private final Button btnBorrarObs;
        private final Button btnSolicitud;

        private static final String STYLE_SOL_INACTIVA =
                "-fx-background-color: #E7E7E7; -fx-text-fill: #A9A9A9;" +
                "-fx-font-size: 11px; -fx-background-radius: 0; -fx-padding: 4 10 4 10;";
        private static final String STYLE_SOL_ACTIVA =
                "-fx-background-color: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + "; -fx-text-fill: white;" +
                "-fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 0; -fx-padding: 4 10 4 10;";

        private final HBox mainRow;

        private int cantidad = 0;
        private String observacion = null;
        private boolean solicitudActiva = false;
        private boolean solicitudFueCancelada = false;
        private boolean solicitudNuevaEnEstaSesion = false;
        private String descripcionSolicitud = null;
        private Runnable onCambio;

        // ── Agotado ───────────────────────────────────────────────────────────
        private HBox subFilaAgotado;
        private Label lblSubAgotado;
        private Button btnSubAgotado;
        private Button btnEditarDesc;
        private boolean agotadoConfirmado = false;
        private String descripcionAgotado = null;

        // ── Edición ───────────────────────────────────────────────────────────
        private boolean modoEdicion = false;
        private int idComOriginal = -1;
        private int cantidadOriginal = 0;
        private boolean esReutilizadoOriginal = false;
        private String observacionOriginal = null;

        FilaUI(String prefijo, List<Componente> skus, Image imgBorrar, Image imgEditar) {
            this.prefijo = prefijo;
            this.skus = skus;

            lblContador = new Label("0");
            lblContador.setMinWidth(34);
            lblContador.setPrefWidth(34);
            lblContador.setAlignment(Pos.CENTER);
            lblContador.setFont(Font.font("Inter", FontWeight.BOLD, 20));
            lblContador.setStyle("-fx-text-fill: #A9A9A9;");

            btnMas = botonContador("+");
            btnMenos = botonContador("-");
            btnMenos.setDisable(true);

            VBox botonera = new VBox(btnMas, btnMenos);
            botonera.setMinWidth(35);
            botonera.setMaxWidth(35);

            HBox wrapContador = new HBox(lblContador, botonera);
            wrapContador.setMinWidth(70);
            wrapContador.setMaxWidth(70);
            wrapContador.setAlignment(Pos.CENTER_LEFT);

            Label lblNombre = new Label(traducirTipo(prefijo));
            lblNombre.setMinWidth(100);
            lblNombre.setPrefWidth(100);
            lblNombre.setMaxWidth(100);
            lblNombre.setStyle("-fx-font-size: 12px; -fx-padding: 0 10 0 10;");

            chkReutilizado = new CheckBox("Reutilizado");
            chkReutilizado.setMinWidth(110);
            chkReutilizado.setPrefWidth(110);
            chkReutilizado.setStyle("-fx-font-size: 12px; -fx-padding: 0 10 0 10;");

            cbSku = new ComboBox<>();
            cbSku.getItems().addAll(skus);
            cbSku.setMinWidth(170);
            cbSku.setPrefWidth(170);
            cbSku.setMaxWidth(170);
            cbSku.setStyle("-fx-font-size: 11px;");

            cbSku.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Componente c, boolean empty) {
                    super.updateItem(c, empty);
                    if (empty || c == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    setText(c.getTipo());
                    aplicarColorStock(this, c);
                }
            });
            cbSku.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Componente c, boolean empty) {
                    super.updateItem(c, empty);
                    if (empty || c == null) {
                        setText("—");
                        setStyle("");
                        return;
                    }
                    setText(c.getTipo());
                    aplicarColorStock(this, c);
                }
            });

            Componente porDefecto = skus.stream()
                    .filter(c -> c.getStock() > 0)
                    .findFirst().orElse(skus.get(0));
            cbSku.setValue(porDefecto);

            lblStock = new Label(prefijo.equals("otro") ? "—" : String.valueOf(porDefecto.getStock()));
            lblStock.setMinWidth(70);
            lblStock.setPrefWidth(70);
            lblStock.setMaxWidth(70);
            lblStock.setAlignment(Pos.CENTER);
            lblStock.setStyle("-fx-font-size: 12px; -fx-padding: 0 10 0 10;");

            btnMas.setDisable(!prefijo.equals("otro") && porDefecto.getStock() <= 0);

            cbSku.valueProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    lblStock.setText(prefijo.equals("otro") ? "—" : String.valueOf(n.getStock()));
                    if (!prefijo.equals("otro")) {
                        // En edición con mismo componente, el stock visible ya no incluye
                        // las piezas de esta reparación — no resetear cantidad por eso
                        boolean mismoComponenteEnEdicion = modoEdicion && n.getIdCom() == idComOriginal;
                        if (!mismoComponenteEnEdicion && cantidad > n.getStock()) {
                            cantidad = 0;
                            actualizarContador();
                            if (!chkReutilizado.isSelected())
                                chkReutilizado.setDisable(false);
                        }
                        if (!chkReutilizado.isSelected()) {
                            btnMas.setDisable(!mismoComponenteEnEdicion && cantidad >= n.getStock());
                        }
                    }
                    if (modoEdicion) actualizarStockPreview();
                    actualizarSubFilaAgotado();
                    notificar();
                }
            });

            btnObservacion = new Button("✎  Añadir observación");
            btnObservacion.setStyle(
                    "-fx-background-color: #A9A9A9; -fx-text-fill: #E7E7E7;" +
                            "-fx-font-size: 11px; -fx-cursor: hand;" +
                            "-fx-background-radius: 0; -fx-padding: 4 10 4 10;");
            btnObservacion.setMinHeight(27);
            btnObservacion.setMaxHeight(27);

            lblObservacion = new Label();
            lblObservacion.setStyle("-fx-font-size: 12px; -fx-text-fill: #000000;");
            lblObservacion.setMinWidth(0);
            lblObservacion.setMaxWidth(Double.MAX_VALUE);
            lblObservacion.setTextOverrun(OverrunStyle.ELLIPSIS);
            lblObservacion.setVisible(false);
            lblObservacion.setManaged(false);

            ImageView ivBorrarObs = new ImageView(imgBorrar);
            ivBorrarObs.setFitWidth(20);
            ivBorrarObs.setFitHeight(20);
            ivBorrarObs.setPreserveRatio(true);
            btnBorrarObs = new Button();
            btnBorrarObs.setGraphic(ivBorrarObs);
            btnBorrarObs.setStyle(
                    "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 4 2 4;");
            btnBorrarObs.setVisible(false);
            btnBorrarObs.setManaged(false);

            HBox wrapObs = new HBox(4, btnObservacion, lblObservacion, btnBorrarObs);
            wrapObs.setAlignment(Pos.CENTER_LEFT);
            wrapObs.setMaxWidth(280);
            HBox.setHgrow(wrapObs, Priority.ALWAYS);
            HBox.setHgrow(lblObservacion, Priority.ALWAYS);

            btnSolicitud = new Button("⚠ Solicitud pieza");
            btnSolicitud.setStyle(STYLE_SOL_INACTIVA);
            btnSolicitud.setMinHeight(27);
            btnSolicitud.setMaxHeight(27);
            btnSolicitud.setOnAction(e -> abrirSolicitud());
            // Oculto siempre — solo se muestra cuando activarSolicitud() carga una solicitud existente de BD
            btnSolicitud.setVisible(false);
            btnSolicitud.setManaged(false);

            mainRow = new HBox(wrapContador, lblNombre, cbSku, lblStock, chkReutilizado, wrapObs, btnSolicitud);
            mainRow.setAlignment(Pos.CENTER_LEFT);
            mainRow.setMinHeight(37);

            // ── Sub-fila agotado ──────────────────────────────────────────────
            lblSubAgotado = new Label("⚠  Stock agotado. Puedes descontar los componentes fallidos y solicitar reposición.");
            lblSubAgotado.setStyle("-fx-font-size: 11px; -fx-text-fill: #7A5C00;");
            btnSubAgotado = new Button("Solicitar y descontar stock");
            btnSubAgotado.setStyle(
                    "-fx-background-color: #E8A825; -fx-text-fill: white;" +
                    "-fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 4 10 4 10;");
            ImageView ivEditar = new ImageView(imgEditar);
            ivEditar.setFitWidth(16); ivEditar.setFitHeight(16); ivEditar.setPreserveRatio(true);
            btnEditarDesc = new Button();
            btnEditarDesc.setGraphic(ivEditar);
            btnEditarDesc.setStyle(
                    "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 4 2 4;");
            btnEditarDesc.setVisible(false);
            btnEditarDesc.setManaged(false);
            btnEditarDesc.setOnAction(e -> editarDescripcionAgotado());
            subFilaAgotado = new HBox(10, lblSubAgotado, btnSubAgotado, btnEditarDesc);
            subFilaAgotado.setAlignment(Pos.CENTER_LEFT);
            subFilaAgotado.setStyle("-fx-background-color: #FFF8E0; -fx-padding: 4 8 4 70;");
            subFilaAgotado.setVisible(false);
            subFilaAgotado.setManaged(false);

            root = new VBox(mainRow, subFilaAgotado);
            root.setStyle("-fx-background-color: #F3F3F3; " +
                    "-fx-border-color: transparent transparent #E0E0E0 transparent;" +
                    "-fx-border-width: 0 0 1 0;");

            btnSubAgotado.setOnAction(e -> abrirDialogoAgotado());

            btnMas.setOnAction(e -> {
                if (prefijo.equals("otro")) {
                    cantidad++;
                    actualizarContador();
                    chkReutilizado.setDisable(true);
                    notificar();
                    return;
                }
                int stockActual = cbSku.getValue() != null ? cbSku.getValue().getStock() : 0;
                if (cantidad < stockActual) {
                    cantidad++;
                    actualizarContador();
                    btnMas.setDisable(cantidad >= stockActual);
                    chkReutilizado.setDisable(true);
                    notificar();
                }
            });

            btnMenos.setOnAction(e -> {
                if (cantidad > 0) {
                    cantidad--;
                    actualizarContador();
                    if (!prefijo.equals("otro")) {
                        int stockActual = cbSku.getValue() != null ? cbSku.getValue().getStock() : 0;
                        btnMas.setDisable(cantidad >= stockActual);
                    }
                    if (cantidad == 0)
                        chkReutilizado.setDisable(false);
                    notificar();
                }
            });

            chkReutilizado.setOnAction(e -> {
                boolean marcado = chkReutilizado.isSelected();
                btnMas.setDisable(marcado);
                btnMenos.setDisable(marcado);
                actualizarContador();
                actualizarSubFilaAgotado();
                notificar();
            });

            btnObservacion.setOnAction(e -> abrirObservacion());
            Platform.runLater(this::actualizarSubFilaAgotado);

            btnBorrarObs.setOnAction(e -> {
                observacion = null;
                lblObservacion.setText("");
                btnObservacion.setVisible(true);
                btnObservacion.setManaged(true);
                lblObservacion.setVisible(false);
                lblObservacion.setManaged(false);
                btnBorrarObs.setVisible(false);
                btnBorrarObs.setManaged(false);
            });
        }

        // ── Filtro global de modelo ───────────────────────────────────────────

        void aplicarFiltroModelo(String modelo) {
            agotadoConfirmado = false;
            descripcionAgotado = null;
            subFilaAgotado.setVisible(false);
            subFilaAgotado.setManaged(false);
            lblSubAgotado.setText("⚠  Stock agotado. Puedes descontar los componentes fallidos y solicitar reposición.");
            lblSubAgotado.setStyle("-fx-font-size: 11px; -fx-text-fill: #7A5C00;");
            btnSubAgotado.setVisible(true);
            btnSubAgotado.setManaged(true);
            btnEditarDesc.setVisible(false); btnEditarDesc.setManaged(false);
            subFilaAgotado.setStyle("-fx-background-color: #FFF8E0; -fx-padding: 4 8 4 70;");

            cantidad = 0;
            actualizarContador();
            chkReutilizado.setSelected(false);
            chkReutilizado.setDisable(false);
            cbSku.setDisable(false);
            solicitudActiva = false;
            solicitudFueCancelada = false;
            solicitudNuevaEnEstaSesion = false;
            descripcionSolicitud = null;
            btnSolicitud.setText("⚠ Solicitud pieza");
            btnSolicitud.setStyle(STYLE_SOL_INACTIVA);
            btnSolicitud.setDisable(false);
            observacion = null;
            lblObservacion.setText("");
            btnObservacion.setVisible(true);
            btnObservacion.setManaged(true);
            lblObservacion.setVisible(false);
            lblObservacion.setManaged(false);
            btnBorrarObs.setVisible(false);
            btnBorrarObs.setManaged(false);

            if (modelo == null) {
                cbSku.getItems().setAll(skus);
                Componente def = skus.stream()
                        .filter(c -> c.getStock() > 0)
                        .findFirst().orElse(skus.get(0));
                cbSku.setValue(def);
                cbSku.setDisable(false);
                btnMas.setDisable(!prefijo.equals("otro") && def.getStock() <= 0);
                btnMenos.setDisable(true);
                chkReutilizado.setDisable(false);
                btnObservacion.setDisable(false);
                root.setOpacity(1.0);
                return;
            }

            List<Componente> filtrados = skus.stream()
                    .filter(c -> extraerModelo(c.getTipo(), prefijo).equals(modelo))
                    .collect(Collectors.toList());

            if (filtrados.isEmpty()) {
                cbSku.getItems().clear();
                cbSku.setValue(null);
                cbSku.setDisable(true);
                btnMas.setDisable(true);
                btnMenos.setDisable(true);
                chkReutilizado.setDisable(true);
                btnObservacion.setDisable(true);
                root.setOpacity(0.4);
            } else {
                cbSku.getItems().setAll(filtrados);
                Componente def = filtrados.stream()
                        .filter(c -> c.getStock() > 0)
                        .findFirst().orElse(filtrados.get(0));
                cbSku.setValue(def);
                cbSku.setDisable(false);
                btnMas.setDisable(!prefijo.equals("otro") && def.getStock() <= 0);
                btnMenos.setDisable(true);
                chkReutilizado.setDisable(false);
                btnObservacion.setDisable(false);
                root.setOpacity(1.0);
            }
        }

        // ── Color stock ───────────────────────────────────────────────────────

        private static void aplicarColorStock(ListCell<Componente> cell, Componente c) {
            if (c.getStock() == 0) {
                cell.setStyle("-fx-text-fill: " + com.reparaciones.utils.Colores.ROJO_SIN_STOCK + ";");
            } else if (c.getStock() <= c.getStockMinimo()) {
                cell.setStyle("-fx-text-fill: " + com.reparaciones.utils.Colores.FILA_SOLICITUD_BRD + ";");
            } else {
                cell.setStyle("");
            }
        }

        // ── Observación ───────────────────────────────────────────────────────

        private void abrirObservacion() {
            TextArea ta = new TextArea(observacion != null ? observacion : "");
            ta.setWrapText(true);
            ta.setPrefRowCount(5);
            ta.setPrefWidth(400);
            ta.setStyle("-fx-font-size: 13px;");

            Button btnGuardar = new Button("Guardar");
            btnGuardar.setMaxWidth(Double.MAX_VALUE);
            btnGuardar.setStyle("-fx-background-color: #8AC7AF; -fx-text-fill: white;" +
                    "-fx-font-size: 12px; -fx-padding: 8; -fx-cursor: hand;");

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Observación");
            dialog.setHeaderText("Observación para: " + traducirTipo(prefijo));
            dialog.getDialogPane().setContent(new VBox(8, ta, btnGuardar));
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.getDialogPane().setPrefWidth(440);

            btnGuardar.setOnAction(e -> {
                String trimmed = ta.getText().trim();
                if (!trimmed.isEmpty()) {
                    observacion = trimmed;
                    lblObservacion.setText(observacion);
                    btnObservacion.setVisible(false);
                    btnObservacion.setManaged(false);
                    lblObservacion.setVisible(true);
                    lblObservacion.setManaged(true);
                    btnBorrarObs.setVisible(true);
                    btnBorrarObs.setManaged(true);
                }
                dialog.close();
            });

            dialog.showAndWait();
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        private static Button botonContador(String texto) {
            Button btn = new Button(texto);
            btn.setMinWidth(35);
            btn.setMaxWidth(35);
            btn.setMinHeight(18);
            btn.setMaxHeight(18);
            btn.setStyle("-fx-background-color: #A9A9A9; -fx-text-fill: #E7E7E7;" +
                    "-fx-font-size: 14px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;");
            return btn;
        }

        private void actualizarContador() {
            lblContador.setText(String.valueOf(cantidad));
            btnMenos.setDisable(cantidad == 0);
            String color;
            if (modoEdicion && cantidad == 0 && !chkReutilizado.isSelected())
                color = "#C94040";
            else
                color = cantidad > 0 ? "#000000" : "#A9A9A9";
            lblContador.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: " + (modoEdicion && cantidad == 0 && !chkReutilizado.isSelected() ? "bold" : "normal") + ";");
            if (modoEdicion) actualizarStockPreview();
            actualizarSubFilaAgotado();
        }

        private void actualizarStockPreview() {
            if (prefijo.equals("otro")) return;
            Componente sel = cbSku.getValue();
            if (sel == null) return;

            int stockActual = sel.getStock();
            boolean mismoComponente = sel.getIdCom() == idComOriginal;
            boolean reutilizadoNuevo = chkReutilizado.isSelected();

            int devuelto = (mismoComponente && !esReutilizadoOriginal) ? cantidadOriginal : 0;
            int descontado = reutilizadoNuevo ? 0 : cantidad;
            int preview = stockActual + devuelto - descontado;

            String color;
            if      (preview < stockActual) color = "#C94040";
            else if (preview > stockActual) color = "#4CAF50";
            else                            color = "#586376";

            lblStock.setText(stockActual + " → " + preview);
            lblStock.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
        }

        private void notificar() {
            if (onCambio != null)
                onCambio.run();
        }

        static String traducirTipo(String prefijo) {
            return switch (prefijo) {
                case "bat" -> "Batería";
                case "cha" -> "Chasis";
                case "g" -> "Glass";
                case "cam" -> "Cámara";
                case "lcd" -> "Pantalla";
                case "mc" -> "Marco";
                case "otro" -> "Otros";
                default -> prefijo;
            };
        }

        // ── Agotado: métodos ─────────────────────────────────────────────────

        private void actualizarSubFilaAgotado() {
            if (prefijo.equals("otro") || modoEdicion) return;
            if (agotadoConfirmado) return;
            if (solicitudActiva) return;
            Componente sel = cbSku.getValue();
            if (sel == null) { subFilaAgotado.setVisible(false); subFilaAgotado.setManaged(false); return; }
            boolean sinStock = sel.getStock() == 0;
            boolean enLimite = !chkReutilizado.isSelected() && sel.getStock() > 0 && cantidad >= sel.getStock();
            boolean mostrar = sinStock || enLimite;
            if (mostrar) {
                if (sinStock) {
                    lblSubAgotado.setText("⚠  Sin stock disponible. Solicita la pieza para que el admin gestione el pedido.");
                    btnSubAgotado.setText("Solicitar pieza");
                } else {
                    lblSubAgotado.setText("⚠  Stock agotado. Puedes descontar los componentes fallidos y solicitar reposición.");
                    btnSubAgotado.setText("Solicitar y descontar stock");
                }
            }
            subFilaAgotado.setVisible(mostrar);
            subFilaAgotado.setManaged(mostrar);
        }

        private void abrirDialogoAgotado() {
            Componente sel = cbSku.getValue();
            int stock = sel != null ? sel.getStock() : 0;
            boolean sinStock = stock == 0;

            TextArea ta = new TextArea(descripcionAgotado != null ? descripcionAgotado : "");
            ta.setPromptText("Describe la pieza que necesitas (opcional)...");
            ta.setWrapText(true);
            ta.setPrefRowCount(4);
            ta.setPrefWidth(380);
            ta.setStyle("-fx-font-size: 13px;");

            String btnOkText = sinStock
                    ? "Confirmar: solicitar pieza"
                    : "Confirmar: descontar " + stock + " ud. de stock y solicitar";
            Button btnOk = new Button(btnOkText);
            btnOk.setMaxWidth(Double.MAX_VALUE);
            btnOk.setStyle("-fx-background-color: #E8A825; -fx-text-fill: white;" +
                    "-fx-font-size: 12px; -fx-padding: 8; -fx-cursor: hand;");

            String headerText = sinStock
                    ? "Sin stock disponible.\nSe creará una solicitud PENDIENTE para que el admin gestione el pedido."
                    : "Se descontarán " + stock + " unidades de stock y quedará una solicitud PENDIENTE.\nLa asignación permanecerá abierta hasta recibir la pieza.";
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Solicitar pieza — " + traducirTipo(prefijo));
            dialog.setHeaderText(headerText);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.getDialogPane().setPrefWidth(460);
            dialog.getDialogPane().setContent(new VBox(8, ta, btnOk));

            btnOk.setOnAction(e -> {
                descripcionAgotado = ta.getText().trim().isEmpty() ? null : ta.getText().trim();
                agotadoConfirmado = true;
                btnMas.setDisable(true);
                btnMenos.setDisable(true);
                chkReutilizado.setDisable(true);
                cbSku.setDisable(true);
                observacion = null;
                lblObservacion.setText("");
                btnObservacion.setVisible(true);  btnObservacion.setManaged(true);
                lblObservacion.setVisible(false);  lblObservacion.setManaged(false);
                btnBorrarObs.setVisible(false);    btnBorrarObs.setManaged(false);
                btnObservacion.setDisable(true);
                solicitudActiva = false;
                solicitudNuevaEnEstaSesion = false;
                descripcionSolicitud = null;
                btnSolicitud.setDisable(true);
                actualizarLabelConfirmado(stock);
                lblSubAgotado.setStyle("-fx-font-size: 11px; -fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                btnSubAgotado.setVisible(false); btnSubAgotado.setManaged(false);
                btnEditarDesc.setVisible(true);  btnEditarDesc.setManaged(true);
                subFilaAgotado.setStyle("-fx-background-color: #E8F5E9; -fx-padding: 4 8 4 70;");
                dialog.close();
                notificar();
            });

            dialog.showAndWait();
        }

        private void editarDescripcionAgotado() {
            TextArea ta = new TextArea(descripcionAgotado != null ? descripcionAgotado : "");
            ta.setPromptText("Describe la pieza que necesitas (opcional)...");
            ta.setWrapText(true);
            ta.setPrefRowCount(4);
            ta.setPrefWidth(380);
            ta.setStyle("-fx-font-size: 13px;");

            Button btnOk = new Button("Guardar descripción");
            btnOk.setMaxWidth(Double.MAX_VALUE);
            btnOk.setStyle("-fx-background-color: #E8A825; -fx-text-fill: white;" +
                    "-fx-font-size: 12px; -fx-padding: 8; -fx-cursor: hand;");

            Button btnBorrar = new Button("Cancelar solicitud");
            btnBorrar.setMaxWidth(Double.MAX_VALUE);
            btnBorrar.setStyle("-fx-background-color: #C94040; -fx-text-fill: white;" +
                    "-fx-font-size: 12px; -fx-padding: 8; -fx-cursor: hand;");

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Editar descripción de solicitud");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.getDialogPane().setPrefWidth(440);
            dialog.getDialogPane().setContent(new VBox(8, ta, btnOk, btnBorrar));

            btnOk.setOnAction(e -> {
                descripcionAgotado = ta.getText().trim().isEmpty() ? null : ta.getText().trim();
                Componente s = cbSku.getValue();
                actualizarLabelConfirmado(s != null ? s.getStock() : 0);
                dialog.close();
            });

            btnBorrar.setOnAction(e -> {
                dialog.close();
                resetearAgotado();
            });

            dialog.showAndWait();
        }

        private void actualizarLabelConfirmado(int stock) {
            String desc = descripcionAgotado != null ? " — " + descripcionAgotado : "";
            if (stock == 0) {
                lblSubAgotado.setText("✓  Solicitud de reposición pendiente" + desc);
            } else {
                lblSubAgotado.setText("✓  " + stock + " uds. se descontarán al guardar — solicitud pendiente" + desc);
            }
        }

        private void resetearAgotado() {
            agotadoConfirmado = false;
            descripcionAgotado = null;
            cantidad = 0;
            actualizarContador();
            chkReutilizado.setDisable(false);
            cbSku.setDisable(false);
            Componente sel = cbSku.getValue();
            if (sel != null && !prefijo.equals("otro"))
                btnMas.setDisable(sel.getStock() <= 0);
            lblSubAgotado.setStyle("-fx-font-size: 11px; -fx-text-fill: #7A5C00;");
            btnSubAgotado.setVisible(true);  btnSubAgotado.setManaged(true);
            btnEditarDesc.setVisible(false); btnEditarDesc.setManaged(false);
            subFilaAgotado.setStyle("-fx-background-color: #FFF8E0; -fx-padding: 4 8 4 70;");
            btnObservacion.setDisable(false);
            btnSolicitud.setDisable(false);
            actualizarSubFilaAgotado();
            notificar();
        }

        // ── Estado público ────────────────────────────────────────────────────

        boolean isActiva() {
            return cantidad > 0 || chkReutilizado.isSelected() || solicitudNuevaEnEstaSesion || agotadoConfirmado;
        }

        boolean tieneUso() {
            return cantidad > 0 || chkReutilizado.isSelected();
        }

        int getIdComSeleccionado() {
            return cbSku.getValue() != null ? cbSku.getValue().getIdCom() : -1;
        }

        Componente getComponenteSeleccionado() {
            return cbSku.getValue();
        }

        int getCantidad() {
            return cantidad;
        }

        boolean isReutilizado() {
            return chkReutilizado.isSelected();
        }

        String getObservacion() {
            return observacion;
        }

        String getPrefijo() {
            return prefijo;
        }

        List<Componente> getSkus() {
            return skus;
        }

        boolean isSolicitud() {
            return solicitudActiva;
        }

        /** @return {@code true} si el técnico canceló una solicitud existente en esta sesión */
        boolean isSolicitudCancelada() {
            return solicitudFueCancelada;
        }

        /** @return {@code true} si el técnico marcó una nueva solicitud en esta sesión (no cargada de BD) */
        boolean isSolicitudNueva() {
            return solicitudNuevaEnEstaSesion;
        }

        String getDescripcionSolicitud() {
            return descripcionSolicitud;
        }

        boolean isAgotado() {
            return agotadoConfirmado;
        }

        String getDescripcionAgotado() {
            return descripcionAgotado;
        }

        private void abrirSolicitud() {
            TextArea ta = new TextArea(descripcionSolicitud != null ? descripcionSolicitud : "");
            ta.setPromptText("Describe la pieza que falta (opcional)...");
            ta.setWrapText(true);
            ta.setPrefRowCount(4);
            ta.setPrefWidth(380);
            ta.setStyle("-fx-font-size: 13px;");

            Button btnGuardar = new Button(solicitudActiva ? "Guardar cambios" : "Marcar como solicitud");
            btnGuardar.setMaxWidth(Double.MAX_VALUE);
            btnGuardar.setStyle("-fx-background-color: #8AC7AF; -fx-text-fill: white;" +
                    "-fx-font-size: 12px; -fx-padding: 8; -fx-cursor: hand;");

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Solicitud de pieza");
            dialog.setHeaderText("Pieza pendiente: " + traducirTipo(prefijo));
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.getDialogPane().setPrefWidth(440);

            VBox content = new VBox(8, ta, btnGuardar);

            if (solicitudActiva) {
                Button btnCancelarSol = new Button("Cancelar solicitud");
                btnCancelarSol.setMaxWidth(Double.MAX_VALUE);
                btnCancelarSol.setStyle("-fx-background-color: #E05252; -fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-padding: 8; -fx-cursor: hand;");
                btnCancelarSol.setOnAction(e -> {
                    solicitudActiva = false;
                    solicitudFueCancelada = true;
                    descripcionSolicitud = null;
                    btnSolicitud.setText("⚠ Solicitud pieza");
                    btnSolicitud.setStyle(STYLE_SOL_INACTIVA);
                    chkReutilizado.setDisable(false);
                    Componente sel = cbSku.getValue();
                    if (sel != null && !prefijo.equals("otro")) {
                        btnMas.setDisable(sel.getStock() <= 0);
                        btnMenos.setDisable(cantidad == 0);
                    }
                    dialog.close();
                    notificar();
                });
                content.getChildren().add(btnCancelarSol);
            }

            dialog.getDialogPane().setContent(content);

            btnGuardar.setOnAction(e -> {
                String trimmed = ta.getText().trim();
                descripcionSolicitud = trimmed.isEmpty() ? null : trimmed;
                solicitudActiva = true;
                solicitudNuevaEnEstaSesion = true;
                btnSolicitud.setText("⚠ Pieza pendiente");
                btnSolicitud.setStyle(STYLE_SOL_ACTIVA);
                dialog.close();
                notificar();
            });

            dialog.showAndWait();
        }

        void deshabilitarYaReparado() {
            cbSku.setDisable(true);
            btnMas.setDisable(true);
            btnMenos.setDisable(true);
            chkReutilizado.setDisable(true);
            btnObservacion.setDisable(true);
            btnSolicitud.setDisable(true);
            Label lblYaReparado = new Label("✓  Ya reparado");
            lblYaReparado.setStyle("-fx-font-size: 11px; -fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-padding: 0 10 0 10;");
            mainRow.getChildren().add(lblYaReparado);
            root.setStyle("-fx-background-color: #EBF5EB; " +
                    "-fx-border-color: transparent transparent #C5E1C5 transparent;" +
                    "-fx-border-width: 0 0 1 0;");
        }

        void deshabilitarPorNoImplicada() {
            cbSku.setDisable(true);
            btnMas.setDisable(true);
            btnMenos.setDisable(true);
            chkReutilizado.setDisable(true);
            btnObservacion.setDisable(true);
            btnSolicitud.setDisable(true);
            root.setOpacity(0.4);
        }

        // Estado      | Stock>0 | Resultado
        // PENDIENTE   |  sí/no  | Bloqueado (admin aún no gestionó)
        // GESTIONADA  |   sí    | Desbloqueado (pedido llegó a stock)
        // GESTIONADA  |   no    | Bloqueado (pedido hecho, pieza en camino)
        // RECHAZADA   |   —     | No se carga (filtrado en SQL)
        void activarSolicitud(int idCom, String descripcion, boolean esPendiente) {
            for (Componente c : skus) {
                if (c.getIdCom() == idCom) {
                    cbSku.setValue(c);
                    if (!esPendiente && c.getStock() > 0) {
                        // GESTIONADA y ya hay stock — pre-seleccionar sin bloquear
                        solicitudActiva = false;
                        descripcionSolicitud = null;
                        notificar();
                    } else {
                        // PENDIENTE o GESTIONADA sin stock: mostrar aviso, bloquear solo si no hay stock
                        descripcionSolicitud = descripcion;
                        solicitudActiva = true;
                        cantidad = 0;
                        actualizarContador();
                        chkReutilizado.setSelected(false);
                        chkReutilizado.setDisable(true);
                        btnSolicitud.setText("⚠ Pieza pendiente");
                        btnSolicitud.setStyle(STYLE_SOL_ACTIVA);
                        if (c.getStock() > 0) {
                            btnMas.setDisable(false);
                            btnMenos.setDisable(true);
                        } else {
                            btnMas.setDisable(true);
                            btnMenos.setDisable(true);
                        }
                        notificar();
                    }
                    return;
                }
            }
        }


        // ── Modo edición ──────────────────────────────────────────────────────

        void activarModoEdicion(int idCom, boolean esReutilizado, String observacion, int cantOrig) {
            this.modoEdicion = true;
            this.idComOriginal = idCom;
            this.cantidadOriginal = cantOrig;
            this.esReutilizadoOriginal = esReutilizado;
            this.observacionOriginal = observacion;

            // Pre-seleccionar componente
            skus.stream().filter(c -> c.getIdCom() == idCom).findFirst()
                    .ifPresent(cbSku::setValue);

            cantidad = cantOrig;
            Componente sel = cbSku.getValue();
            int stock = sel != null ? sel.getStock() : 0;
            btnMas.setDisable(!prefijo.equals("otro") && stock <= 0);
            chkReutilizado.setSelected(esReutilizado);
            if (esReutilizado) {
                btnMas.setDisable(true);
                btnMenos.setDisable(true);
            } else if (cantOrig > 0) {
                chkReutilizado.setDisable(true);
            }
            actualizarContador();

            // Pre-rellenar observación
            this.observacion = observacion;
            if (observacion != null && !observacion.isBlank()) {
                lblObservacion.setText(observacion);
                btnObservacion.setVisible(false);
                btnObservacion.setManaged(false);
                lblObservacion.setVisible(true);
                lblObservacion.setManaged(true);
                btnBorrarObs.setVisible(true);
                btnBorrarObs.setManaged(true);
            }

            // Ocultar solicitud en modo edición
            btnSolicitud.setVisible(false);
            btnSolicitud.setManaged(false);

            // Fondo azul
            root.setStyle("-fx-background-color: #EBF4FF; " +
                    "-fx-border-color: transparent transparent #B3D4F5 transparent;" +
                    "-fx-border-width: 0 0 1 0;");
        }

        // ── Técnico por fila (solo modo edición, filas grises nuevas) ────────

        private ComboBox<Tecnico> cbTecnicoFila = null;
        private int idTecFila = -1;

        void configurarTecnicoEdicion(List<Tecnico> tecnicos, int idTecDefault) {
            btnSolicitud.setVisible(false);
            btnSolicitud.setManaged(false);
            idTecFila = idTecDefault;
            cbTecnicoFila = new ComboBox<>();
            cbTecnicoFila.getItems().setAll(tecnicos);
            cbTecnicoFila.setPrefWidth(150);
            cbTecnicoFila.setStyle("-fx-font-size: 11px;");
            cbTecnicoFila.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Tecnico t, boolean empty) {
                    super.updateItem(t, empty);
                    setText(empty || t == null ? null : t.getNombre());
                }
            });
            cbTecnicoFila.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Tecnico t, boolean empty) {
                    super.updateItem(t, empty);
                    setText(empty || t == null ? null : t.getNombre());
                }
            });
            tecnicos.stream().filter(t -> t.getIdTec() == idTecDefault)
                    .findFirst().ifPresent(cbTecnicoFila::setValue);
            cbTecnicoFila.valueProperty().addListener((obs, o, n) -> {
                if (n != null) idTecFila = n.getIdTec();
            });
            mainRow.getChildren().add(cbTecnicoFila);
        }

        int getTecnicoId() { return idTecFila; }

        boolean isModoEdicion() { return modoEdicion; }

        boolean hayCambio() {
            if (!modoEdicion) return false;
            Componente sel = cbSku.getValue();
            int idComActual = sel != null ? sel.getIdCom() : -1;
            return cantidad != cantidadOriginal
                    || idComActual != idComOriginal
                    || chkReutilizado.isSelected() != esReutilizadoOriginal
                    || !Objects.equals(observacion, observacionOriginal);
        }

        VBox getRoot() {
            return root;
        }

        void setOnCambio(Runnable r) {
            this.onCambio = r;
        }
    }
}