package com.reparaciones.utils;

import javafx.stage.Stage;

/** Controlador de vista que sabe exportar sus datos visibles a CSV. */
public interface Exportable {
    void exportarCSV(Stage owner);
}
