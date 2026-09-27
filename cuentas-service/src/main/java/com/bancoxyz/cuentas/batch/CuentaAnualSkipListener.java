package com.bancoxyz.cuentas.batch;

import com.bancoxyz.cuentas.entity.CuentaAnual;
import com.bancoxyz.cuentas.service.MigracionEstadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;

public class CuentaAnualSkipListener implements SkipListener<CuentaAnualCsvRow, CuentaAnual> {

    private static final Logger log = LoggerFactory.getLogger(CuentaAnualSkipListener.class);

    private final MigracionEstadoService estadoService;

    public CuentaAnualSkipListener(MigracionEstadoService estadoService) {
        this.estadoService = estadoService;
    }

    @Override
    public void onSkipInProcess(CuentaAnualCsvRow item, Throwable t) {
        estadoService.incrementarOmitidos();
        log.warn("Registro omitido en cuentas_anuales.csv ({}): {} - motivo: {}",
                item == null ? "?" : item.rawKey(), t.getClass().getSimpleName(), t.getMessage());
    }

    @Override
    public void onSkipInRead(Throwable t) {
        estadoService.incrementarOmitidos();
        log.warn("Fila ilegible en cuentas_anuales.csv - motivo: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(CuentaAnual item, Throwable t) {
        estadoService.incrementarOmitidos();
        log.warn("Registro omitido al escribir cuentas_anuales.csv ({}) - motivo: {}", item.getCuentaId(), t.getMessage());
    }
}
