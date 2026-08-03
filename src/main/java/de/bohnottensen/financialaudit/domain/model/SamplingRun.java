package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sampling_runs")
public class SamplingRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_name", nullable = false, length = 255)
    private String runName;

    @Column(name = "sampling_strategy", nullable = false, length = 100)
    private String samplingStrategy;

    private Long seed;

    @Column(name = "population_size")
    private Long populationSize;

    @Column(name = "sample_size")
    private Long sampleSize;

    @Column(name = "parameters_json", length = 4000)
    private String parametersJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public String getSamplingStrategy() {
        return samplingStrategy;
    }

    public void setSamplingStrategy(String samplingStrategy) {
        this.samplingStrategy = samplingStrategy;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public Long getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(Long populationSize) {
        this.populationSize = populationSize;
    }

    public Long getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Long sampleSize) {
        this.sampleSize = sampleSize;
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public void setParametersJson(String parametersJson) {
        this.parametersJson = parametersJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
