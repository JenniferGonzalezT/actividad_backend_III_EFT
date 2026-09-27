package com.bancoxyz.transacciones.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class MigracionEstadoService {

    private final AtomicLong leidos = new AtomicLong();
    private final AtomicLong escritos = new AtomicLong();
    private final AtomicLong omitidos = new AtomicLong();

    public void reset() {
        leidos.set(0);
        escritos.set(0);
        omitidos.set(0);
    }

    public void incrementarLeidos() {
        leidos.incrementAndGet();
    }

    public void incrementarEscritos(long cantidad) {
        escritos.addAndGet(cantidad);
    }

    public void incrementarOmitidos() {
        omitidos.incrementAndGet();
    }

    public record Estado(long leidos, long escritos, long omitidos) {
    }

    public Estado snapshot() {
        return new Estado(leidos.get(), escritos.get(), omitidos.get());
    }
}
