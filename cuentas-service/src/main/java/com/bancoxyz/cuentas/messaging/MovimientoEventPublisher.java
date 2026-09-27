package com.bancoxyz.cuentas.messaging;

import com.bancoxyz.common.events.MovimientoAplicadoEvent;
import com.bancoxyz.common.events.Topics;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Publicador protegido por Resilience4j. Sin fallback a proposito: si el envio falla, la excepcion
 * revierte la transaccion del listener y Kafka reentrega el evento (at-least-once).
 */
@Component
public class MovimientoEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MovimientoEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @CircuitBreaker(name = "kafkaPublisher")
    @Retry(name = "kafkaPublisher")
    public void publicar(MovimientoAplicadoEvent evento) throws Exception {
        kafkaTemplate.send(Topics.MOVIMIENTO_APLICADO, String.valueOf(evento.cuentaId()), evento)
                .get(5, TimeUnit.SECONDS);
    }
}
