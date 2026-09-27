package com.bancoxyz.transacciones.batch;

import com.bancoxyz.transacciones.entity.Transaccion;
import com.bancoxyz.transacciones.repository.TransaccionRepository;
import com.bancoxyz.transacciones.service.MigracionEstadoService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransaccionBatchConfig {

    @Bean
    public FlatFileItemReader<TransaccionCsvRow> transaccionReader() {
        BeanWrapperFieldSetMapper<TransaccionCsvRow> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(TransaccionCsvRow.class);
        return new FlatFileItemReaderBuilder<TransaccionCsvRow>()
                .name("transaccionItemReader")
                .resource(new ClassPathResource("data/transacciones.csv"))
                .linesToSkip(1)
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .fieldSetMapper(mapper)
                .build();
    }

    @Bean
    public TransaccionItemProcessor transaccionItemProcessor(DuplicateGuard duplicateGuard,
                                                               MigracionEstadoService estadoService) {
        return new TransaccionItemProcessor(duplicateGuard, estadoService);
    }

    @Bean
    public RepositoryItemWriter<Transaccion> transaccionWriter(TransaccionRepository repository) {
        RepositoryItemWriter<Transaccion> writer = new RepositoryItemWriter<>();
        writer.setRepository(repository);
        writer.setMethodName("save");
        return writer;
    }

    @Bean
    public TransaccionSkipListener transaccionSkipListener(MigracionEstadoService estadoService) {
        return new TransaccionSkipListener(estadoService);
    }

    @Bean
    public TransaccionMigracionJobListener transaccionMigracionJobListener(TransaccionRepository repository,
                                                                             MigracionEstadoService estadoService) {
        return new TransaccionMigracionJobListener(repository, estadoService);
    }

    @Bean
    public Step migrarTransaccionesStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         FlatFileItemReader<TransaccionCsvRow> transaccionReader,
                                         TransaccionItemProcessor transaccionItemProcessor,
                                         RepositoryItemWriter<Transaccion> transaccionWriter,
                                         TransaccionSkipListener transaccionSkipListener) {
        // chunk(1): con chunks mayores, Spring Batch reprocesa el chunk entero fila por
        // fila cuando una excepcion "skippable" ocurre, para aislar al culpable. Eso
        // reinvoca el processor sobre filas ya vistas por el DuplicateGuard (stateful),
        // marcandolas como falsos duplicados. chunk(1) elimina ese reescaneo.
        return new StepBuilder("migrarTransaccionesStep", jobRepository)
                .<TransaccionCsvRow, Transaccion>chunk(1, transactionManager)
                .reader(transaccionReader)
                .processor(transaccionItemProcessor)
                .writer(transaccionWriter)
                .faultTolerant()
                .skip(RegistroInvalidoException.class)
                .skip(RegistroDuplicadoException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(1000)
                .listener(transaccionSkipListener)
                .build();
    }

    @Bean
    public Job migrarTransaccionesJob(JobRepository jobRepository,
                                       Step migrarTransaccionesStep,
                                       TransaccionMigracionJobListener transaccionMigracionJobListener) {
        return new JobBuilder("migrarTransaccionesJob", jobRepository)
                .start(migrarTransaccionesStep)
                .listener(transaccionMigracionJobListener)
                .build();
    }
}
