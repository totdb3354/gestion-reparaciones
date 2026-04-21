package com.reparaciones.models;

import javafx.beans.property.*;

/**
 * Línea de un pedido multi-componente antes de persistirse en BD.
 * <p>Cada instancia representa un componente con su cantidad y precio
 * unitario dentro de un pedido en construcción. Usa JavaFX properties
 * para que la {@code TableView} del formulario de pedidos refleje
 * los cambios en tiempo real.</p>
 *
 * @see com.reparaciones.dao.CompraComponenteDAO
 */
public class LineaPedido {

    /** Componente seleccionado para esta línea. */
    private final ObjectProperty<Componente> componente  = new SimpleObjectProperty<>();

    /** Número de unidades a pedir (mínimo 1). */
    private final IntegerProperty            cantidad    = new SimpleIntegerProperty(1);

    /** Precio por unidad en la divisa seleccionada. */
    private final DoubleProperty             precioUnidad= new SimpleDoubleProperty(0.0);

    /** {@code true} si el pedido de esta línea es urgente. */
    private final BooleanProperty            esUrgente   = new SimpleBooleanProperty(false);

    /** Proveedor seleccionado para esta línea. */
    private final ObjectProperty<Proveedor>  proveedor   = new SimpleObjectProperty<>();

    /** Tasa de cambio a EUR para la divisa del proveedor de esta línea. */
    private double tasa = 1.0;

    /** Constructor por defecto — todos los campos en sus valores iniciales. */
    public LineaPedido() {}

    /**
     * @param componente   componente seleccionado
     * @param cantidad     número de unidades a pedir
     * @param precioUnidad precio por unidad en la divisa elegida
     * @param esUrgente    {@code true} si el pedido es urgente
     */
    public LineaPedido(Componente componente, int cantidad, double precioUnidad, boolean esUrgente) {
        this.componente.set(componente);
        this.cantidad.set(cantidad);
        this.precioUnidad.set(precioUnidad);
        this.esUrgente.set(esUrgente);
    }

    // ── Properties ────────────────────────────────────────────────────────────

    /** @return property del componente (observable por la TableView) */
    public ObjectProperty<Componente> componenteProperty()  { return componente; }

    /** @return property de la cantidad (observable por la TableView) */
    public IntegerProperty cantidadProperty() { return cantidad; }

    /** @return property del precio unitario (observable por la TableView) */
    public DoubleProperty precioUnidadProperty() { return precioUnidad; }

    /** @return property de urgencia (observable por la TableView) */
    public BooleanProperty esUrgenteProperty() { return esUrgente; }

    // ── Getters / setters ─────────────────────────────────────────────────────

    /** @return componente seleccionado */
    public Componente getComponente() { return componente.get(); }

    /** @return número de unidades a pedir */
    public int getCantidad() { return cantidad.get(); }

    /** @return precio por unidad en la divisa elegida */
    public double getPrecioUnidad() { return precioUnidad.get(); }

    /** @return {@code true} si el pedido es urgente */
    public boolean isEsUrgente() { return esUrgente.get(); }

    public void setComponente(Componente c) { componente.set(c); }
    public void setCantidad(int v)          { cantidad.set(v); }
    public void setPrecioUnidad(double v)   { precioUnidad.set(v); }
    public void setEsUrgente(boolean v)     { esUrgente.set(v); }
    public void setProveedor(Proveedor p)   { proveedor.set(p); }
    public void setTasa(double t)           { tasa = t; }

    public ObjectProperty<Proveedor> proveedorProperty() { return proveedor; }
    public Proveedor getProveedor()  { return proveedor.get(); }
    public double getTasa()          { return tasa; }
}
