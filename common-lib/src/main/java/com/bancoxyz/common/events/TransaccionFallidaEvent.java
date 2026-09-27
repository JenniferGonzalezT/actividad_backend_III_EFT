package com.bancoxyz.common.events;

import java.time.Instant;

/** Evento de compensacion: un consumidor agoto sus reintentos y la saga debe revertirse. */
public record TransaccionFallidaEvent(
        String eventId,
        String correlationId,
        Instant occurredAt,
        Long transaccionId,
        Long cuentaId,
        String servicioOrigen,
        String motivo
) implements EventoTransaccional {
}
