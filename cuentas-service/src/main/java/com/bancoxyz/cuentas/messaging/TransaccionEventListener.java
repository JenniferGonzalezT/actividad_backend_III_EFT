package com.bancoxyz.cuentas.messaging;

import com.bancoxyz.common.events.MovimientoAplicadoEvent;
import com.bancoxyz.common.events.TransaccionFallidaEvent;
import com.bancoxyz.common.events.TransaccionRegistradaEvent;
import com.bancoxyz.common.events.Topics;
import com.bancoxyz.cuentas.entity.CuentaAnual;
import com.bancoxyz.cuentas.entity.EventoProcesado;
import com.bancoxyz.cuentas.repository.CuentaAnualRepository;
import com.bancoxyz.cuentas.repository.EventoProcesadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class TransaccionEventListener {

    static final String ORIGEN = "cuentas-service";

    private static final Logger log = LoggerFactory.getLogger(TransaccionEventListener.class);

    private final CuentaAnualRepository cuentas;
    private final EventoProcesadoRepository procesados;
    private final MovimientoEventPublisher publisher;

    public TransaccionEventListener(CuentaAnualRepository cuentas, EventoProcesadoRepository procesados,
                                    MovimientoEventPublisher publisher) {
        this.cuentas = cuentas;
        this.procesados = procesados;
        this.publisher = publisher;
    }

    /** Paso 2 de la saga: registra el movimiento y avisa a intereses-service. */
    @KafkaListener(topics = Topics.TRANSACCION_REGISTRADA)
    @Transactional
    public void onTransaccionRegistrada(TransaccionRegistradaEvent evento) throws Exception {
        if (procesados.existsById(evento.eventId())) {
            log.info("Evento {} ya procesado, se ignora", evento.eventId());
            return;
        }
        String movimiento = "credito".equalsIgnoreCase(evento.tipo()) ? "deposito" : "retiro";
        cuentas.save(new CuentaAnual(evento.cuentaId(), LocalDate.now(), movimiento, evento.monto(),
                "Transaccion " + evento.transaccionId()));
        procesados.save(new EventoProcesado(evento.eventId()));
        publisher.publicar(new MovimientoAplicadoEvent(UUID.randomUUID().toString(), evento.correlationId(),
                Instant.now(), evento.transaccionId(), evento.cuentaId(), movimiento, evento.monto()));
        log.info("Movimiento {} aplicado a cuenta {} (transaccionId={})", movimiento, evento.cuentaId(),
                evento.transaccionId());
    }

    /** Compensacion: si un paso posterior fallo, se revierte el movimiento con un asiento inverso. */
    @KafkaListener(topics = Topics.TRANSACCION_FALLIDA)
    @Transactional
    public void onTransaccionFallida(TransaccionFallidaEvent evento) {
        if (ORIGEN.equals(evento.servicioOrigen()) || procesados.existsById(evento.eventId())) {
            return; // fallo propio: no se aplico nada que revertir
        }
        cuentas.findByCuentaId(evento.cuentaId()).stream()
                .filter(c -> ("Transaccion " + evento.transaccionId()).equals(c.getDescripcion()))
                .findFirst()
                .ifPresent(c -> {
                    String inverso = "deposito".equals(c.getTransaccion()) ? "retiro" : "deposito";
                    cuentas.save(new CuentaAnual(c.getCuentaId(), LocalDate.now(), inverso, c.getMonto(),
                            "Reverso transaccion " + evento.transaccionId()));
                    log.warn("Reverso aplicado a cuenta {} por transaccion fallida {}", c.getCuentaId(),
                            evento.transaccionId());
                });
        procesados.save(new EventoProcesado(evento.eventId()));
    }
}
