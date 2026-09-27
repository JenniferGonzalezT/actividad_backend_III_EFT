package com.bancoxyz.common.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Publicado por intereses-service cuando actualiza el saldo; cierra la saga con exito. */
public record InteresRecalculadoEvent(
        String eventId,
        String correlationId,
        Instant occurredAt,
        Long transaccionId,
        Long cuentaId,
        BigDecimal nuevoSaldo
) implements EventoTransaccional {
}
