package com.reparaciones.utils;

/**
 * Contrato para controladores de vista principal que gestionan polling periódico.
 * <p>El {@code MainController} invoca {@link #recargar()} cada vez que el usuario
 * navega de vuelta a una vista, y {@link #detenerPolling()} al salir.</p>
 */
public interface Recargable {

    /**
     * Recarga los datos de la vista desde BD y actualiza los componentes visuales.
     * <p>Debe ser seguro llamarlo desde el hilo de JavaFX.</p>
     */
    void recargar();

    /**
     * Detiene el mecanismo de polling o refresco automático de la vista.
     * <p>Llamar antes de navegar a otra sección para liberar recursos
     * (p. ej. cancelar un {@code ScheduledExecutorService}).</p>
     */
    void detenerPolling();
}
