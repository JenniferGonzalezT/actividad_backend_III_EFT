package com.bancoxyz.common.messaging;

import com.bancoxyz.common.events.EventoTransaccional;
import com.bancoxyz.common.events.TransaccionFallidaEvent;
import com.bancoxyz.common.events.Topics;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.time.Instant;
import java.util.UUID;

/**
 * Manejo de errores compartido por los consumidores: reintentos con backoff exponencial y, al
 * agotarlos, envio al Dead Letter Topic (mismo nombre + ".DLT") y publicacion del evento de
 * compensacion {@code transaccion.fallida}.
 */
public final class KafkaErrorHandling {

    private static final Log log = LogFactory.getLog(KafkaErrorHandling.class);

    private KafkaErrorHandling() {
    }

    public static DefaultErrorHandler errorHandler(KafkaOperations<String, Object> template, String servicio) {
        DeadLetterPublishingRecoverer dlt = new DeadLetterPublishingRecoverer(template);
        ConsumerRecordRecoverer recoverer = (record, ex) -> {
            dlt.accept(record, ex);
            publicarFallida(template, servicio, record, ex);
        };
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(4000);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    private static void publicarFallida(KafkaOperations<String, Object> template, String servicio,
                                        ConsumerRecord<?, ?> record, Exception ex) {
        // Solo se compensa cuando el mensaje pudo deserializarse a un evento de la saga.
        if (!(record.value() instanceof EventoTransaccional evento)) {
            log.error("Mensaje irrecuperable en " + record.topic() + " enviado a DLT sin compensacion: " + ex);
            return;
        }
        Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
        TransaccionFallidaEvent fallida = new TransaccionFallidaEvent(
                UUID.randomUUID().toString(), evento.correlationId(), Instant.now(),
                evento.transaccionId(), evento.cuentaId(), servicio, causa.toString());
        try {
            template.send(Topics.TRANSACCION_FALLIDA, String.valueOf(evento.cuentaId()), fallida);
            log.warn("Reintentos agotados en " + servicio + " para transaccion " + evento.transaccionId()
                    + "; publicado " + Topics.TRANSACCION_FALLIDA);
        } catch (RuntimeException e) {
            log.error("No se pudo publicar " + Topics.TRANSACCION_FALLIDA + ": " + e);
        }
    }
}
