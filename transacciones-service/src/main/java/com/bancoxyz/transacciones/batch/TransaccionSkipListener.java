package com.bancoxyz.transacciones.batch;

import com.bancoxyz.transacciones.entity.Transaccion;
import com.bancoxyz.transacciones.service.MigracionEstadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;

public class TransaccionSkipListener implements SkipListener<TransaccionCsvRow, Transaccion> {

    private static final Logger log = LoggerFactory.getLogger(TransaccionSkipListener.class);

    private final MigracionEstadoService estadoService;

    public TransaccionSkipListener(MigracionEstadoService estadoService) {
        this.estadoService = estadoService;
    }

    @Override
    public void onSkipInProcess(TransaccionCsvRow item, Throwable t) {
        estadoService.incrementarOmitidos();
        log.warn("Registro omitido en transacciones.csv ({}): {} - motivo: {}",
                item == null ? "?" : item.rawKey(), t.getClass().getSimpleName(), t.getMessage());
    }

    @Override
    public void onSkipInRead(Throwable t) {
        estadoService.incrementarOmitidos();
        log.warn("Fila ilegible en transacciones.csv - motivo: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(Transaccion item, Throwable t) {
        estadoService.incrementarOmitidos();
        log.warn("Registro omitido al escribir transacciones.csv ({}) - motivo: {}", item.getId(), t.getMessage());
    }
}
