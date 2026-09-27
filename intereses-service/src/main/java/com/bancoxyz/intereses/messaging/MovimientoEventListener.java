package com.bancoxyz.intereses.messaging;

import com.bancoxyz.common.events.InteresRecalculadoEvent;
import com.bancoxyz.common.events.MovimientoAplicadoEvent;
import com.bancoxyz.common.events.Topics;
import com.bancoxyz.intereses.entity.EventoProcesado;
import com.bancoxyz.intereses.entity.Interes;
import com.bancoxyz.intereses.repository.EventoProcesadoRepository;
import com.bancoxyz.intereses.repository.InteresRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class MovimientoEventListener {

    private static final Logger log = LoggerFactory.getLogger(MovimientoEventListener.class);

    private final InteresRepository intereses;
    private final EventoProcesadoRepository procesados;
    private final InteresEventPublisher publisher;

    public MovimientoEventListener(InteresRepository intereses, EventoProcesadoRepository procesados,
                                   InteresEventPublisher publisher) {
        this.intereses = intereses;
        this.procesados = procesados;
        this.publisher = publisher;
    }

    /** Paso 3 de la saga: ajusta el saldo. Si la cuenta no existe se lanza excepcion -> reintentos -> DLT. */
    @KafkaListener(topics = Topics.MOVIMIENTO_APLICADO)
    @Transactional
    public void onMovimientoAplicado(MovimientoAplicadoEvent evento) throws Exception {
        if (procesados.existsById(evento.eventId())) {
            log.info("Evento {} ya procesado, se ignora", evento.eventId());
            return;
        }
        Interes interes = intereses.findFirstByCuentaId(evento.cuentaId())
                .orElseThrow(() -> new IllegalStateException("No existe cuenta " + evento.cuentaId() + " en intereses"));
        BigDecimal saldo = interes.getSaldo() != null ? interes.getSaldo() : BigDecimal.ZERO;
        BigDecimal nuevo = "deposito".equals(evento.movimiento()) ? saldo.add(evento.monto()) : saldo.subtract(evento.monto());
        interes.setSaldo(nuevo);
        intereses.save(interes);
        procesados.save(new EventoProcesado(evento.eventId()));
        publisher.publicar(new InteresRecalculadoEvent(UUID.randomUUID().toString(), evento.correlationId(),
                Instant.now(), evento.transaccionId(), evento.cuentaId(), nuevo));
        log.info("Saldo de cuenta {} recalculado a {} (transaccionId={})", evento.cuentaId(), nuevo,
                evento.transaccionId());
    }
}
