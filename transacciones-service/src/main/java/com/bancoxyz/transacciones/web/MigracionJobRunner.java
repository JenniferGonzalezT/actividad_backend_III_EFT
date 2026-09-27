package com.bancoxyz.transacciones.web;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

@Component
public class MigracionJobRunner {

    private final JobLauncher jobLauncher;
    private final Job migrarTransaccionesJob;

    public MigracionJobRunner(JobLauncher jobLauncher, Job migrarTransaccionesJob) {
        this.jobLauncher = jobLauncher;
        this.migrarTransaccionesJob = migrarTransaccionesJob;
    }

    public void ejecutar() {
        try {
            jobLauncher.run(migrarTransaccionesJob, new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo re-ejecutar la migracion", e);
        }
    }
}
