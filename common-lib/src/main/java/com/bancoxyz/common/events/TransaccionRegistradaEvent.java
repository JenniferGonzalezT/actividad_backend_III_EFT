package com.bancoxyz.common.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Publicado por transacciones-service cuando acepta una nueva transaccion. */
public record TransaccionRegistradaEvent(
        String eventId,
        String correlationId,
        Instant occurredAt,
        Long transaccionId,
        Long cuentaId,
        String tipo,
        BigDecimal monto,
        String fecha
) implements EventoTransaccional {
}
