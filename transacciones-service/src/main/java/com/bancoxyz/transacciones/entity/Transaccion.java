package com.bancoxyz.transacciones.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transacciones")
public class Transaccion {

    @Id
    private Long id;

    private LocalDate fecha;

    private BigDecimal monto;

    private String tipo;

    private Long cuentaId;

    /** PENDIENTE_PUBLICACION | PENDIENTE | CONFIRMADA | RECHAZADA. Las migradas desde CSV nacen CONFIRMADA. */
    private String estado = ESTADO_CONFIRMADA;

    public static final String ESTADO_PENDIENTE_PUBLICACION = "PENDIENTE_PUBLICACION";
    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_CONFIRMADA = "CONFIRMADA";
    public static final String ESTADO_RECHAZADA = "RECHAZADA";

    public Transaccion() {
    }

    public Transaccion(Long id, LocalDate fecha, BigDecimal monto, String tipo) {
        this.id = id;
        this.fecha = fecha;
        this.monto = monto;
        this.tipo = tipo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
