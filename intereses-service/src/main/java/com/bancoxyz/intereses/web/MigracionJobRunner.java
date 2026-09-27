package com.bancoxyz.intereses.web;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

@Component
public class MigracionJobRunner {

    private final JobLauncher jobLauncher;
    private final Job migrarInteresesJob;

    public MigracionJobRunner(JobLauncher jobLauncher, Job migrarInteresesJob) {
        this.jobLauncher = jobLauncher;
        this.migrarInteresesJob = migrarInteresesJob;
    }

    public void ejecutar() {
        try {
            jobLauncher.run(migrarInteresesJob, new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo re-ejecutar la migracion", e);
        }
    }
}
