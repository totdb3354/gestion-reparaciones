package com.reparaciones.utils;

import javafx.stage.Stage;

/**
 * Contrato para controladores de vista que pueden exportar sus datos visibles a CSV.
 * <p>El menú de usuario llama a {@link #exportarCSV(Stage)} cuando el usuario
 * selecciona "Exportar CSV" desde el botón de acciones del contexto actual.</p>
 */
public interface Exportable {

    /**
     * Abre un diálogo de guardado y exporta los datos actualmente visibles a un fichero CSV.
     *
     * @param owner ventana padre para el {@code FileChooser} (garantiza que se muestra centrado)
     */
    void exportarCSV(Stage owner);
}
