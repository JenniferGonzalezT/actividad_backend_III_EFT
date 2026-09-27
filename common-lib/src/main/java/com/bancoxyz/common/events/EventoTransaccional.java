package com.bancoxyz.common.events;

import java.time.Instant;

/** Contrato comun de los eventos que viajan por la saga de una transaccion. */
public interface EventoTransaccional {

    /** Identificador unico del evento; se usa para deduplicar (idempotencia del consumidor). */
    String eventId();

    /** Identificador que une todos los eventos de una misma transaccion de negocio. */
    String correlationId();

    Instant occurredAt();

    Long transaccionId();

    Long cuentaId();
}
