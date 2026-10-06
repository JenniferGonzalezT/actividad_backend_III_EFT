package com.bancoxyz.cuentas.batch;

import com.bancoxyz.cuentas.entity.CuentaAnual;
import com.bancoxyz.cuentas.repository.CuentaAnualRepository;
import com.bancoxyz.cuentas.service.MigracionEstadoService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CuentaAnualBatchConfig {

    @Bean
    public FlatFileItemReader<CuentaAnualCsvRow> cuentaAnualReader() {
        BeanWrapperFieldSetMapper<CuentaAnualCsvRow> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(CuentaAnualCsvRow.class);
        return new FlatFileItemReaderBuilder<CuentaAnualCsvRow>()
                .name("cuentaAnualItemReader")
                .resource(new ClassPathResource("data/cuentas_anuales.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuentaId", "fecha", "transaccion", "monto", "descripcion")
                .fieldSetMapper(mapper)
                .build();
    }

    @Bean
    public CuentaAnualItemProcessor cuentaAnualItemProcessor(DuplicateGuard duplicateGuard,
                                                               MigracionEstadoService estadoService) {
        return new CuentaAnualItemProcessor(duplicateGuard, estadoService);
    }

    @Bean
    public RepositoryItemWriter<CuentaAnual> cuentaAnualWriter(CuentaAnualRepository repository) {
        RepositoryItemWriter<CuentaAnual> writer = new RepositoryItemWriter<>();
        writer.setRepository(repository);
        writer.setMethodName("save");
        return writer;
    }

    @Bean
    public CuentaAnualSkipListener cuentaAnualSkipListener(MigracionEstadoService estadoService) {
        return new CuentaAnualSkipListener(estadoService);
    }

    @Bean
    public CuentaAnualMigracionJobListener cuentaAnualMigracionJobListener(CuentaAnualRepository repository,
                                                                            MigracionEstadoService estadoService) {
        return new CuentaAnualMigracionJobListener(repository, estadoService);
    }

    @Bean
    public Step migrarCuentasAnualesStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          FlatFileItemReader<CuentaAnualCsvRow> cuentaAnualReader,
                                          CuentaAnualItemProcessor cuentaAnualItemProcessor,
                                          RepositoryItemWriter<CuentaAnual> cuentaAnualWriter,
                                          CuentaAnualSkipListener cuentaAnualSkipListener) {
        return new StepBuilder("migrarCuentasAnualesStep", jobRepository)
                .<CuentaAnualCsvRow, CuentaAnual>chunk(10, transactionManager)
                .reader(cuentaAnualReader)
                .processor(cuentaAnualItemProcessor)
                .writer(cuentaAnualWriter)
                .faultTolerant()
                .skip(RegistroInvalidoException.class)
                .skip(RegistroDuplicadoException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(1000)
                .listener(cuentaAnualSkipListener)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Job migrarCuentasAnualesJob(JobRepository jobRepository,
                                        Step migrarCuentasAnualesStep,
                                        CuentaAnualMigracionJobListener cuentaAnualMigracionJobListener) {
        return new JobBuilder("migrarCuentasAnualesJob", jobRepository)
                .start(migrarCuentasAnualesStep)
                .listener(cuentaAnualMigracionJobListener)
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("Batch-Thread-");
        executor.initialize();
        return executor;
    }
}
