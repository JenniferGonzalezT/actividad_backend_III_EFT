package com.bancoxyz.transacciones.messaging;

import com.bancoxyz.common.events.TransaccionRegistradaEvent;
import com.bancoxyz.common.events.Topics;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Publica eventos protegido por Resilience4j. Se bloquea en el resultado del envio para que un
 * fallo real del broker cuente como fallo del Circuit Breaker / Retry.
 */
@Component
public class TransaccionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransaccionEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransaccionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** @return true si el broker confirmo el envio; false si se activo el fallback. */
    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "publicarFallback")
    @Retry(name = "kafkaPublisher")
    public boolean publicar(TransaccionRegistradaEvent evento) throws Exception {
        kafkaTemplate.send(Topics.TRANSACCION_REGISTRADA, String.valueOf(evento.cuentaId()), evento)
                .get(5, TimeUnit.SECONDS);
        log.info("Evento {} publicado (transaccionId={}, correlationId={})",
                Topics.TRANSACCION_REGISTRADA, evento.transaccionId(), evento.correlationId());
        return true;
    }

    private boolean publicarFallback(TransaccionRegistradaEvent evento, Throwable t) {
        log.warn("Kafka no disponible, transaccion {} queda PENDIENTE_PUBLICACION: {}",
                evento.transaccionId(), t.toString());
        return false;
    }
}
