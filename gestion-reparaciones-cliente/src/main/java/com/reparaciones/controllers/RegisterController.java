package com.reparaciones.controllers;

import com.reparaciones.dao.UsuarioDAO;
import com.reparaciones.models.Usuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controlador del modal de gestión de técnicos.
 * <p>Permite al administrador registrar nuevos técnicos, activar/desactivar
 * cuentas existentes y eliminarlas cuando no tienen reparaciones asociadas.</p>
 * <p>La tabla se recarga tras cada operación sin cerrar el modal.</p>
 *
 * @role ADMIN
 */
public class RegisterController {

    @FXML private TextField     campoNombreTecnico;
    @FXML private TextField     campoNombreUsuario;
    @FXML private PasswordField campoPassword;
    @FXML private PasswordField campoConfirmar;
    @FXML private Label         lblError;
    @FXML private Button        btnRegistrar;

    @FXML private TableView<Usuario>           tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colNombreTecnico;
    @FXML private TableColumn<Usuario, String> colNombreUsuario;
    @FXML private TableColumn<Usuario, Void>   colEstado;
    @FXML private TableColumn<Usuario, Void>   colAcciones;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private ObservableList<Usuario> datos = FXCollections.observableArrayList();

    /**
     * Inicializa la tabla de usuarios técnicos al cargar la vista.
     */
    @FXML
    public void initialize() {
        configurarTabla();
        cargarUsuarios();
    }

    /**
     * Configura las columnas de la tabla.
     * - colNombreTecnico: nombre visible del técnico
     * - colNombreUsuario: credencial de login
     * - colEstado: badge Activo / Inactivo
     * - colAcciones: icono toggle (activar/desactivar) + icono borrar (solo si sin reparaciones)
     */
    private void configurarTabla() {
        colNombreTecnico.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNombreTecnico()));
        colNombreUsuario.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNombreUsuario()));

        // Badge de estado
        colEstado.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; " +
                               "-fx-background-radius: 10; -fx-padding: 3 10 3 10;");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                boolean activo = getTableView().getItems().get(getIndex()).isActivo();
                if (activo) {
                    badge.setText("Activo");
                    badge.setStyle(badge.getStyle() +
                        "-fx-background-color: #D4EDDA; -fx-text-fill: #2E7D32;");
                } else {
                    badge.setText("Inactivo");
                    badge.setStyle(badge.getStyle() +
                        "-fx-background-color: #F5E6E6; -fx-text-fill: #B03040;");
                }
                setGraphic(badge);
            }
        });

        // Iconos de acción: toggle activo/inactivo + borrar (solo si no tiene reparaciones)
        Image imgBorrar     = new Image(getClass().getResourceAsStream("/images/borrar.png"));
        Image imgDesactivar = new Image(getClass().getResourceAsStream("/images/Lock.png"));
        Image imgActivar    = new Image(getClass().getResourceAsStream("/images/Unlock.png"));

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnToggle = new Button();
            private final ImageView ivToggle = new ImageView();
            private final ImageView ivBorrar = new ImageView(imgBorrar);
            private final Region    spacer   = new Region();
            private final HBox      contenedor;

            {
                ivToggle.setFitWidth(18); ivToggle.setFitHeight(18); ivToggle.setPreserveRatio(true);
                ivBorrar.setFitWidth(22); ivBorrar.setFitHeight(22); ivBorrar.setPreserveRatio(true);

                // Botón transparente con padding amplio para área de click cómoda
                btnToggle.setGraphic(ivToggle);
                btnToggle.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                                   "-fx-cursor: hand; -fx-padding: 4 6 4 6;");

                ivBorrar.setStyle("-fx-cursor: hand;");
                spacer.setPrefWidth(4);
                contenedor = new HBox(btnToggle, spacer, ivBorrar);
                contenedor.setAlignment(javafx.geometry.Pos.CENTER);

                btnToggle.setOnAction(e -> {
                    Usuario u = getTableView().getItems().get(getIndex());
                    toggleActivacion(u);
                });
                ivBorrar.setOnMouseClicked(e -> {
                    Usuario u = getTableView().getItems().get(getIndex());
                    intentarEliminar(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                boolean activo = getTableView().getItems().get(getIndex()).isActivo();
                // candado abierto = activo (tiene acceso), cerrado = inactivo (sin acceso)
                ivToggle.setImage(activo ? imgActivar : imgDesactivar);
                Tooltip.install(btnToggle, new Tooltip(activo ? "Desactivar acceso" : "Activar acceso"));
                setGraphic(contenedor);
            }
        });

        tablaUsuarios.setItems(datos);
    }

    /** Recarga la lista de técnicos desde BD y actualiza la tabla. */
    private void cargarUsuarios() {
        try {
            List<Usuario> lista = usuarioDAO.getUsuariosTecnicos();
            datos.setAll(lista);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Activa o desactiva el técnico según su estado actual.
     * El login queda bloqueado automáticamente si está inactivo.
     */
    private void toggleActivacion(Usuario u) {
        try {
            if (u.isActivo()) {
                usuarioDAO.desactivarTecnico(u.getIdTec());
            } else {
                usuarioDAO.activarTecnico(u.getIdTec());
            }
            cargarUsuarios();
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error al cambiar el estado del técnico.");
        }
    }

    /**
     * Intenta eliminar el técnico. Solo lo permite si no tiene reparaciones asociadas.
     * Si las tiene, informa al usuario y le ofrece desactivarlo en su lugar.
     * Si no las tiene, pide confirmación y elimina Usuario + Tecnico en transacción.
     */
    private void intentarEliminar(Usuario u) {
        try {
            if (usuarioDAO.tieneReparaciones(u.getIdTec())) {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("No se puede eliminar");
                info.setHeaderText("\"" + u.getNombreTecnico() + "\" tiene reparaciones asociadas.");
                info.setContentText("No es posible eliminarlo para conservar el historial.\n" +
                                    "Puedes desactivarlo para bloquear su acceso.");
                info.showAndWait();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Eliminar técnico");
            confirm.setHeaderText("¿Eliminar a \"" + u.getNombreTecnico() + "\" definitivamente?");
            confirm.setContentText("Se borrarán sus credenciales de acceso y su registro de técnico.");
            confirm.showAndWait().ifPresent(respuesta -> {
                if (respuesta == ButtonType.OK) {
                    try {
                        usuarioDAO.eliminarTecnico(u.getIdUsu(), u.getIdTec());
                        cargarUsuarios();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        mostrarError("Error al eliminar el técnico.");
                    }
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarError("Error al comprobar las reparaciones del técnico.");
        }
    }

    /**
     * Valida los campos y registra el nuevo técnico.
     * Inserta en Tecnico y Usuario en transacción — si falla uno, rollback de ambos.
     * Tras el registro limpia el formulario y recarga la tabla sin salir de la vista.
     */
    @FXML
    private void registrar() {
        String nombreTecnico = campoNombreTecnico.getText().trim();
        String nombreUsuario = campoNombreUsuario.getText().trim();
        String password      = campoPassword.getText();
        String confirmar     = campoConfirmar.getText();

        if (nombreTecnico.isEmpty() || nombreUsuario.isEmpty() || password.isEmpty()) {
            mostrarError("Todos los campos son obligatorios.");
            return;
        }

        if (!password.equals(confirmar)) {
            mostrarError("Las contraseñas no coinciden.");
            return;
        }

        if (password.length() < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        try {
            usuarioDAO.registrarTecnico(nombreTecnico, nombreUsuario, password);
            limpiarFormulario();
            cargarUsuarios();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                mostrarError("Ese nombre de usuario ya existe.");
            } else {
                e.printStackTrace();
                mostrarError("Error al registrar. Inténtalo de nuevo.");
            }
        }
    }

    /**
     * Limpia el formulario tras un registro exitoso sin salir de la vista.
     */
    private void limpiarFormulario() {
        campoNombreTecnico.clear();
        campoNombreUsuario.clear();
        campoPassword.clear();
        campoConfirmar.clear();
        lblError.setVisible(false);
    }

    /**
     * Cierra el modal de gestión de técnicos.
     */
    @FXML
    private void cerrar() {
        ((Stage) btnRegistrar.getScene().getWindow()).close();
    }

    /** Muestra el mensaje de error bajo el formulario. */
    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }
}