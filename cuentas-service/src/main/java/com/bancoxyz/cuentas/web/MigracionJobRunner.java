package com.bancoxyz.cuentas.web;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

@Component
public class MigracionJobRunner {

    private final JobLauncher jobLauncher;
    private final Job migrarCuentasAnualesJob;

    public MigracionJobRunner(JobLauncher jobLauncher, Job migrarCuentasAnualesJob) {
        this.jobLauncher = jobLauncher;
        this.migrarCuentasAnualesJob = migrarCuentasAnualesJob;
    }

    public void ejecutar() {
        try {
            jobLauncher.run(migrarCuentasAnualesJob, new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo re-ejecutar la migracion", e);
        }
    }
}
