package com.bancoxyz.common.events;

/** Nombres de los topicos Kafka del sistema. Cada topico tiene su Dead Letter Topic con sufijo ".DLT". */
public final class Topics {

    public static final String TRANSACCION_REGISTRADA = "transaccion.registrada";
    public static final String MOVIMIENTO_APLICADO = "cuenta.movimiento-aplicado";
    public static final String INTERES_RECALCULADO = "interes.recalculado";
    public static final String TRANSACCION_FALLIDA = "transaccion.fallida";

    public static final String DLT_SUFFIX = ".DLT";
    public static final int PARTICIONES = 3;

    private Topics() {
    }
}
