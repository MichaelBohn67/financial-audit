package de.bohnottensen.financialaudit.application.usecase.sampling;

import de.bohnottensen.financialaudit.domain.model.SamplingRun;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class SamplingService {

    private final BookingRepository bookingRepository;

    public SamplingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public SamplingRun generateMusSample(String runName, long populationSize, long sampleSize, long seed) {
        List<Long> population = new ArrayList<>();
        for (long idx = 0; idx < populationSize; idx++) {
            population.add(idx);
        }
        Collections.shuffle(population, new Random(seed));

        SamplingRun run = new SamplingRun();
        run.setRunName(runName);
        run.setSamplingStrategy("MUS");
        run.setSeed(seed);
        run.setPopulationSize(populationSize);
        run.setSampleSize(sampleSize);
        run.setParametersJson("{\"method\":\"MUS\",\"sampleSize\":" + sampleSize + "}");
        return run;
    }
}
