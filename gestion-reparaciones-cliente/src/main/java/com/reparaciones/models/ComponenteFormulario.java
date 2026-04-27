package com.reparaciones.models;

import javafx.beans.property.*;

/**
 * Modelo de fila para la tabla de componentes del formulario de finalización
 * de reparación.
 * <p>Utiliza JavaFX properties para que la {@code TableView} detecte cambios
 * en tiempo real y pueda habilitar o deshabilitar el botón de guardar de forma
 * reactiva, sin necesidad de refrescar la tabla manualmente.</p>
 * <p>El campo {@code unidades} está limitado a 0 o 1 (máximo un componente
 * de cada tipo por reparación en el flujo actual).</p>
 */
public class ComponenteFormulario {

    /** ID del componente (clave foránea a {@code Componente}). */
    private final IntegerProperty idCom;

    /** Descripción del tipo de componente. */
    private final StringProperty tipo;

    /** SKU del componente (código de referencia para pedidos). */
    private final StringProperty sku;

    /** Unidades disponibles en almacén en el momento de cargar el formulario. */
    private final IntegerProperty stock;

    /** Unidades seleccionadas por el técnico: {@code 0} (no usa) o {@code 1} (usa). */
    private final IntegerProperty unidades;

    /** {@code true} si el componente proviene de una reparación anterior (no resta stock). */
    private final BooleanProperty reutilizado;

    /** Notas libres del técnico sobre este componente en la reparación actual. */
    private final StringProperty observacion;

    /**
     * Crea una fila inicializada con {@code unidades=0}, {@code reutilizado=false}
     * y {@code observacion=""}.
     *
     * @param idCom  ID del componente
     * @param tipo   descripción del tipo de componente
     * @param sku    código SKU del componente
     * @param stock  unidades disponibles en almacén
     */
    public ComponenteFormulario(int idCom, String tipo, String sku, int stock) {
        this.idCom      = new SimpleIntegerProperty(idCom);
        this.tipo       = new SimpleStringProperty(tipo);
        this.sku        = new SimpleStringProperty(sku);
        this.stock      = new SimpleIntegerProperty(stock);
        this.unidades   = new SimpleIntegerProperty(0);
        this.reutilizado = new SimpleBooleanProperty(false);
        this.observacion = new SimpleStringProperty("");
    }

    /** @return ID del componente */
    public int getIdCom() { return idCom.get(); }

    /** @return descripción del tipo de componente */
    public String getTipo() { return tipo.get(); }

    /** @return property del SKU (observable por la TableView) */
    public StringProperty skuProperty() { return sku; }

    /** @return código SKU del componente */
    public String getSku() { return sku.get(); }
    public void setSku(String s) { sku.set(s); }

    /** @return unidades disponibles en almacén */
    public int getStock() { return stock.get(); }

    /** @return unidades seleccionadas: {@code 0} o {@code 1} */
    public int getUnidades() { return unidades.get(); }

    /** @return property de unidades (observable por la TableView) */
    public IntegerProperty unidadesProperty() { return unidades; }
    public void setUnidades(int u) { unidades.set(u); }

    /** @return {@code true} si el componente se reutiliza y no resta stock */
    public boolean isReutilizado() { return reutilizado.get(); }

    /** @return property de reutilizado (observable por la TableView) */
    public BooleanProperty reutilizadoProperty() { return reutilizado; }
    public void setReutilizado(boolean r) { reutilizado.set(r); }

    /** @return notas libres del técnico */
    public String getObservacion() { return observacion.get(); }
    public void setObservacion(String o) { observacion.set(o); }
}
