package com.bancoxyz.intereses.batch;

import com.bancoxyz.intereses.entity.Interes;
import com.bancoxyz.intereses.service.MigracionEstadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;

public class InteresSkipListener implements SkipListener<InteresCsvRow, Interes> {

    private static final Logger log = LoggerFactory.getLogger(InteresSkipListener.class);

    private final MigracionEstadoService estadoService;

    public InteresSkipListener(MigracionEstadoService estadoService) {
        this.estadoService = estadoService;
    }

    @Override
    public void onSkipInProcess(InteresCsvRow item, Throwable t) {
        estadoService.incrementarOmitidos();
        log.warn("Registro omitido en intereses.csv ({}): {} - motivo: {}",
                item == null ? "?" : item.rawKey(), t.getClass().getSimpleName(), t.getMessage());
    }

    @Override
    public void onSkipInRead(Throwable t) {
        estadoService.incrementarOmitidos();
        log.warn("Fila ilegible en intereses.csv - motivo: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(Interes item, Throwable t) {
        estadoService.incrementarOmitidos();
        log.warn("Registro omitido al escribir intereses.csv ({}) - motivo: {}", item.getCuentaId(), t.getMessage());
    }
}
