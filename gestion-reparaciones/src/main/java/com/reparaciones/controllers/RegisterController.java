package com.reparaciones.controllers;

import com.reparaciones.dao.UsuarioDAO;
import com.reparaciones.models.Usuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class RegisterController {

    @FXML private TextField     campoNombreTecnico;
    @FXML private TextField     campoNombreUsuario;
    @FXML private PasswordField campoPassword;
    @FXML private PasswordField campoConfirmar;
    @FXML private Label         lblError;
    @FXML private Button        btnRegistrar;

    @FXML private TableView<Usuario>           tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colNombreUsuario;
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
     * Configura las columnas de la tabla de usuarios existentes.
     * La columna acciones tiene un botón borrar por fila.
     */
    private void configurarTabla() {
        colNombreUsuario.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNombreUsuario()));

        Image imgBorrar = new Image(getClass().getResourceAsStream("/images/borrar.png"));

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView(imgBorrar);
            private final HBox contenedor = new HBox(iv);

            {
                iv.setFitWidth(20);
                iv.setFitHeight(20);
                iv.setPreserveRatio(true);
                iv.setStyle("-fx-cursor: hand;");
                contenedor.setAlignment(javafx.geometry.Pos.CENTER);

                iv.setOnMouseClicked(e -> {
                    Usuario u = getTableView().getItems().get(getIndex());
                    confirmarBorrado(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : contenedor);
            }
        });

        tablaUsuarios.setItems(datos);
    }

    /**
     * Carga todos los usuarios con rol TECNICO en la tabla.
     */
    private void cargarUsuarios() {
        try {
            List<Usuario> lista = usuarioDAO.getUsuariosTecnicos();
            datos.setAll(lista);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Muestra confirmación antes de borrar el usuario.
     * Solo borra el registro de Usuario (credenciales) — la tabla Tecnico
     * y todas las reparaciones históricas quedan intactas.
     */
    private void confirmarBorrado(Usuario u) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Borrar usuario");
        confirm.setHeaderText("¿Borrar el usuario \"" + u.getNombreUsuario() + "\"?");
        confirm.setContentText(
            "El técnico seguirá apareciendo en las reparaciones históricas.\n" +
            "Solo se elimina su acceso al sistema.");

        confirm.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.OK) {
                try {
                    usuarioDAO.borrarUsuario(u.getIdUsu());
                    cargarUsuarios();
                } catch (SQLException e) {
                    e.printStackTrace();
                    mostrarError("Error al borrar el usuario.");
                }
            }
        });
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
     * Vuelve a LoginView sin registrar nada.
     */
    @FXML
    private void volverLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnRegistrar.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestión de Reparaciones — Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }
}