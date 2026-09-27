package com.bancoxyz.transacciones.messaging;

import com.bancoxyz.common.events.InteresRecalculadoEvent;
import com.bancoxyz.common.events.TransaccionFallidaEvent;
import com.bancoxyz.common.events.Topics;
import com.bancoxyz.transacciones.entity.Transaccion;
import com.bancoxyz.transacciones.repository.TransaccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Cierra la saga: confirma la transaccion al recibir el resultado final o la rechaza si fallo. */
@Component
public class TransaccionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransaccionEventListener.class);

    private final TransaccionRepository repository;

    public TransaccionEventListener(TransaccionRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = Topics.INTERES_RECALCULADO)
    public void onInteresRecalculado(InteresRecalculadoEvent evento) {
        cambiarEstado(evento.transaccionId(), Transaccion.ESTADO_CONFIRMADA);
    }

    @KafkaListener(topics = Topics.TRANSACCION_FALLIDA)
    public void onTransaccionFallida(TransaccionFallidaEvent evento) {
        log.warn("Transaccion {} fallida en {}: {}", evento.transaccionId(), evento.servicioOrigen(), evento.motivo());
        cambiarEstado(evento.transaccionId(), Transaccion.ESTADO_RECHAZADA);
    }

    private void cambiarEstado(Long transaccionId, String estado) {
        repository.findById(transaccionId).ifPresentOrElse(t -> {
            // Una transaccion rechazada no vuelve a confirmarse por un evento tardio o duplicado.
            if (!Transaccion.ESTADO_RECHAZADA.equals(t.getEstado())) {
                t.setEstado(estado);
                repository.save(t);
                log.info("Transaccion {} -> {}", transaccionId, estado);
            }
        }, () -> log.warn("Evento para transaccion inexistente {}", transaccionId));
    }
}
