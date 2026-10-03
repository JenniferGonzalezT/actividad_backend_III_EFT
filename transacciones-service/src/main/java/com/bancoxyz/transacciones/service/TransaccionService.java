package com.bancoxyz.transacciones.service;

import com.bancoxyz.common.events.TransaccionRegistradaEvent;
import com.bancoxyz.transacciones.entity.Transaccion;
import com.bancoxyz.transacciones.messaging.TransaccionEventPublisher;
import com.bancoxyz.transacciones.repository.TransaccionRepository;
import com.bancoxyz.transacciones.web.NuevaTransaccionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Service
public class TransaccionService {

    private static final Logger log = LoggerFactory.getLogger(TransaccionService.class);
    private static final Set<String> TIPOS = Set.of("credito", "debito");

    private final TransaccionRepository repository;
    private final TransaccionEventPublisher publisher;

    public TransaccionService(TransaccionRepository repository, TransaccionEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /** Guarda la transaccion como PENDIENTE y publica el evento que inicia la saga. */
    public Transaccion registrar(NuevaTransaccionRequest req) {
        if (req == null || req.cuentaId() == null || req.monto() == null || req.monto().signum() <= 0
                || req.tipo() == null || !TIPOS.contains(req.tipo().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cuentaId, monto > 0 y tipo (credito|debito) son obligatorios");
        }
        Transaccion t = guardarNueva(req);
        if (!publicar(t)) {
            t.setEstado(Transaccion.ESTADO_PENDIENTE_PUBLICACION);
            repository.save(t);
        }
        return t;
    }

    private synchronized Transaccion guardarNueva(NuevaTransaccionRequest req) {
        Transaccion t = new Transaccion(repository.maxId() + 1, LocalDate.now(), req.monto(), req.tipo().toLowerCase());
        t.setCuentaId(req.cuentaId());
        t.setEstado(Transaccion.ESTADO_PENDIENTE);
        return repository.save(t);
    }

    /** Reintenta las transacciones que no pudieron publicarse por caida de Kafka (outbox simple). */
    @Scheduled(fixedDelayString = "${app.kafka.reintento-ms:15000}", initialDelay = 15000)
    public void republicarPendientes() {
        for (Transaccion t : repository.findByEstado(Transaccion.ESTADO_PENDIENTE_PUBLICACION)) {
            if (publicar(t)) {
                t.setEstado(Transaccion.ESTADO_PENDIENTE);
                repository.save(t);
                log.info("Transaccion {} republicada tras recuperacion de Kafka", t.getId());
            }
        }
    }

    private boolean publicar(Transaccion t) {
        // Conservamos el identificador original del evento usando el ID de la transacción 
        // en lugar de UUID.randomUUID() para garantizar la idempotencia en los consumidores.
        String id = "TXN-" + t.getId();
        
        try {
            return publisher.publicar(new TransaccionRegistradaEvent(id, id, Instant.now(), t.getId(),
                    t.getCuentaId(), t.getTipo(), t.getMonto(), String.valueOf(t.getFecha())));
        } catch (Exception e) {
            log.warn("Fallo al publicar transaccion {}: {}", t.getId(), e.toString());
            return false;
        }
    }
}
