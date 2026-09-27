package com.bancoxyz.cuentas.batch;

import com.bancoxyz.cuentas.repository.CuentaAnualRepository;
import com.bancoxyz.cuentas.service.MigracionEstadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class CuentaAnualMigracionJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(CuentaAnualMigracionJobListener.class);

    private final CuentaAnualRepository repository;
    private final MigracionEstadoService estadoService;

    public CuentaAnualMigracionJobListener(CuentaAnualRepository repository, MigracionEstadoService estadoService) {
        this.repository = repository;
        this.estadoService = estadoService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        estadoService.reset();
        repository.deleteAllInBatch();
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long escritos = repository.count();
        estadoService.incrementarEscritos(escritos);
        var estado = estadoService.snapshot();
        log.info("Migracion de cuentas_anuales.csv finalizada: leidos={}, escritos={}, omitidos={}",
                estado.leidos(), escritos, estado.omitidos());
    }
}
