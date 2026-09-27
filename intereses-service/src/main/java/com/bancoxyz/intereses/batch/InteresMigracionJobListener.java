package com.bancoxyz.intereses.batch;

import com.bancoxyz.intereses.repository.InteresRepository;
import com.bancoxyz.intereses.service.MigracionEstadoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class InteresMigracionJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(InteresMigracionJobListener.class);

    private final InteresRepository repository;
    private final MigracionEstadoService estadoService;

    public InteresMigracionJobListener(InteresRepository repository, MigracionEstadoService estadoService) {
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
        log.info("Migracion de intereses.csv finalizada: leidos={}, escritos={}, omitidos={}",
                estado.leidos(), escritos, estado.omitidos());
    }
}
