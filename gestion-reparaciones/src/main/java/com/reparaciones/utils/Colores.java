package com.reparaciones.utils;

/**
 * Paleta de colores centralizada.
 * Los colores de marca se usan en estilos inline de controladores;
 * los colores del CSS (reparacion.css, main.css) se gestionan allí directamente.
 */
public final class Colores {

    private Colores() {} // impide instanciar — solo contiene constantes estáticas

    // ─── Paleta de marca ──────────────────────────────────────────────────────

    public static final String AZUL_NOCHE  = "#001232";
    public static final String AZUL_MEDIO  = "#2C3B54";
    public static final String AZUL_GRIS   = "#586376";
    public static final String CREMA       = "#F6F6F6";
    public static final String AMARILLO    = "#F1E356";

    // ─── Estados funcionales (filas tabla) ────────────────────────────────────

    /** Fila en modo edición activa — azul claro */
    public static final String FILA_EDICION_BG  = "#EBF4FF";
    public static final String FILA_EDICION_BRD = "#B3D4F5";

    /** Fila ya reparada (deshabilitada) — verde suave */
    public static final String FILA_REPARADO_BG  = "#EBF5EB";
    public static final String FILA_REPARADO_BRD = "#C5E1C5";
    public static final String FILA_REPARADO_ICO = "#8AC7AF";

    /** Fila pedido recibido — verde sólido */
    public static final String FILA_RECIBIDO_BG  = "#C8E6C9";
    public static final String FILA_RECIBIDO_BRD = "#3a7d44";

    /** Fila pedido alterado — naranja salmón, distinto del ámbar de solicitudes */
    public static final String FILA_ALTERADO_BG  = "#FAD9C1";
    public static final String FILA_ALTERADO_BRD = "#C0622A";

    /** Fila pedido parcial — lila claro */
    public static final String FILA_PARCIAL_BG  = "#E8E0F7";
    public static final String FILA_PARCIAL_BRD = "#7B5EA7";

    /** Fila pedido cancelado/devuelto — gris deshabilitado */
    public static final String FILA_CANCELADO_BG  = "#E0E0E0";
    public static final String FILA_CANCELADO_BRD = "#9E9E9E";
    public static final String FILA_CANCELADO_TEXT = "#9E9E9E";

    /** Fila con incidencia activa — fondo rosa saturado, borde inferior carmesí */
    public static final String FILA_INCIDENCIA_BG  = "#E8C8CE";
    public static final String FILA_INCIDENCIA_BRD = "#B83746";

    /** Fila con solicitud de pieza pendiente — fondo ámbar, borde naranja oscuro */
    public static final String FILA_SOLICITUD_BG   = "#FDEBC8";
    public static final String FILA_SOLICITUD_BRD  = "#C07800";

    /** Fila con cambio pendiente de confirmar — fondo cyan claro, borde teal */
    public static final String FILA_MODIFICADA_BG  = "#E0F7FA";
    public static final String FILA_MODIFICADA_BRD = "#00838F";

    // ─── Texto funcional ──────────────────────────────────────────────────────

    /** Texto de advertencia / incidencia / error de validación */
    public static final String TEXTO_ERROR    = "#B03040";

    /** Texto de acción secundaria — azul medio */
    public static final String TEXTO_ACCION   = "#5B8CFF";

    /** Verde confirmación */
    public static final String VERDE_OK       = "#4CAF50";

    // ─── UI genérica ──────────────────────────────────────────────────────────

    /** Fondo muy claro para inputs y filtros */
    public static final String FONDO_INPUT       = "#F3F3F3";

    /** Borde y texto de controles deshabilitados o neutros */
    public static final String GRIS_BORDE        = "#A9A9A9";

    /** Fondo de botón / elemento deshabilitado */
    public static final String GRIS_DISABLED     = "#E7E7E7";

    /** Borde inferior de fila seleccionada (azul oscuro) */
    public static final String FILA_SELECTED_BRD = "#3D5070";

    /** Separador inferior entre filas (gris claro) */
    public static final String FILA_SEP = "#C2C8D0";

    // ─── Acciones destructivas ────────────────────────────────────────────────

    /** Botón de acción destructiva (ConfirmDialog borrar) */
    public static final String ROJO_ACCION    = "#A84040";

    /** Estado sin stock (StockController, FormularioReparacion) */
    public static final String ROJO_SIN_STOCK = "#B03040";
}
