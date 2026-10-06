package com.bancoxyz.intereses.batch;

import com.bancoxyz.intereses.entity.Interes;
import com.bancoxyz.intereses.repository.InteresRepository;
import com.bancoxyz.intereses.service.MigracionEstadoService;
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
public class InteresBatchConfig {

    @Bean
    public FlatFileItemReader<InteresCsvRow> interesReader() {
        BeanWrapperFieldSetMapper<InteresCsvRow> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(InteresCsvRow.class);
        return new FlatFileItemReaderBuilder<InteresCsvRow>()
                .name("interesItemReader")
                .resource(new ClassPathResource("data/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .fieldSetMapper(mapper)
                .build();
    }

    @Bean
    public InteresItemProcessor interesItemProcessor(DuplicateGuard duplicateGuard,
                                                       MigracionEstadoService estadoService) {
        return new InteresItemProcessor(duplicateGuard, estadoService);
    }

    @Bean
    public RepositoryItemWriter<Interes> interesWriter(InteresRepository repository) {
        RepositoryItemWriter<Interes> writer = new RepositoryItemWriter<>();
        writer.setRepository(repository);
        writer.setMethodName("save");
        return writer;
    }

    @Bean
    public InteresSkipListener interesSkipListener(MigracionEstadoService estadoService) {
        return new InteresSkipListener(estadoService);
    }

    @Bean
    public InteresMigracionJobListener interesMigracionJobListener(InteresRepository repository,
                                                                     MigracionEstadoService estadoService) {
        return new InteresMigracionJobListener(repository, estadoService);
    }

    @Bean
    public Step migrarInteresesStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     FlatFileItemReader<InteresCsvRow> interesReader,
                                     InteresItemProcessor interesItemProcessor,
                                     RepositoryItemWriter<Interes> interesWriter,
                                     InteresSkipListener interesSkipListener) {
        return new StepBuilder("migrarInteresesStep", jobRepository)
                .<InteresCsvRow, Interes>chunk(10, transactionManager)
                .reader(interesReader)
                .processor(interesItemProcessor)
                .writer(interesWriter)
                .faultTolerant()
                .skip(RegistroInvalidoException.class)
                .skip(RegistroDuplicadoException.class)
                .skip(FlatFileParseException.class)
                .skipLimit(1000)
                .listener(interesSkipListener)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Job migrarInteresesJob(JobRepository jobRepository,
                                   Step migrarInteresesStep,
                                   InteresMigracionJobListener interesMigracionJobListener) {
        return new JobBuilder("migrarInteresesJob", jobRepository)
                .start(migrarInteresesStep)
                .listener(interesMigracionJobListener)
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
