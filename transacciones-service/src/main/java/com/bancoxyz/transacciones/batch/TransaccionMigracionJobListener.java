package com.bancoxyz.transacciones.batch;

import com.bancoxyz.transacciones.repository.TransaccionRepository;
import com.bancoxyz.transacciones.service.MigracionEstadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class TransaccionMigracionJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(TransaccionMigracionJobListener.class);

    private final TransaccionRepository repository;
    private final MigracionEstadoService estadoService;

    public TransaccionMigracionJobListener(TransaccionRepository repository, MigracionEstadoService estadoService) {
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
        log.info("Migracion de transacciones.csv finalizada: leidos={}, escritos={}, omitidos={}",
                estado.leidos(), escritos, estado.omitidos());
    }
}
