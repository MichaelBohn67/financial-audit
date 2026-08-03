package de.bohnottensen.financialaudit.application.usecase.sampling;

import de.bohnottensen.financialaudit.domain.model.Booking;
import de.bohnottensen.financialaudit.domain.model.SamplingRun;
import de.bohnottensen.financialaudit.domain.model.SamplingRunItem;
import de.bohnottensen.financialaudit.infrastructure.persistence.BookingRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunItemRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class SamplingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int INTERVAL_SCALE = 6;

    private final BookingRepository bookingRepository;
    private final SamplingRunRepository samplingRunRepository;
    private final SamplingRunItemRepository samplingRunItemRepository;

    public SamplingService(BookingRepository bookingRepository,
                           SamplingRunRepository samplingRunRepository,
                           SamplingRunItemRepository samplingRunItemRepository) {
        this.bookingRepository = bookingRepository;
        this.samplingRunRepository = samplingRunRepository;
        this.samplingRunItemRepository = samplingRunItemRepository;
    }

    public SamplingRun generateMusSample(String runName, long populationSize, long sampleSize, long seed) {
        if (sampleSize <= 0) {
            throw new IllegalArgumentException("sampleSize must be > 0");
        }

        List<Booking> population = bookingRepository.findAll().stream()
                .filter(booking -> booking.getId() != null)
                .filter(booking -> booking.getAmount() != null && booking.getAmount().compareTo(ZERO) > 0)
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .collect(Collectors.toList());

        if (population.isEmpty()) {
            throw new IllegalArgumentException("No positive booking amounts available for MUS sampling");
        }

        BigDecimal totalAmount = population.stream()
                .map(Booking::getAmount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal interval = totalAmount.divide(BigDecimal.valueOf(sampleSize), INTERVAL_SCALE, RoundingMode.HALF_UP);
        if (interval.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Sampling interval must be > 0");
        }

        BigDecimal randomStart = interval.multiply(BigDecimal.valueOf(new Random(seed).nextDouble()))
                .setScale(INTERVAL_SCALE, RoundingMode.HALF_UP);

        SamplingRun run = new SamplingRun();
        run.setRunName(runName);
        run.setSamplingStrategy("MUS");
        run.setSeed(seed);
        run.setPopulationSize((long) population.size());
        run.setSampleSize(sampleSize);
        run.setParametersJson("{\"method\":\"MUS\",\"requestedPopulationSize\":" + populationSize
                + ",\"effectivePopulationSize\":" + population.size()
                + ",\"sampleSize\":" + sampleSize
                + ",\"totalAmount\":\"" + totalAmount + "\""
                + ",\"interval\":\"" + interval + "\""
                + ",\"startPoint\":\"" + randomStart + "\"}");

        SamplingRun savedRun = samplingRunRepository.save(run);

        BigDecimal cumulative = ZERO;
        int sampleUnit = 0;
        BigDecimal selectionPoint = randomStart.setScale(INTERVAL_SCALE, RoundingMode.HALF_UP);

        for (Booking booking : population) {
            cumulative = cumulative.add(booking.getAmount());
            while (sampleUnit < sampleSize && cumulative.compareTo(selectionPoint) >= 0) {
                SamplingRunItem item = new SamplingRunItem();
                item.setSamplingRunId(savedRun.getId());
                item.setSampleUnitIndex(sampleUnit);
                item.setSelectionPoint(selectionPoint);
                item.setBookingId(booking.getId());
                item.setBookingAmount(booking.getAmount());
                item.setCumulativeAmount(cumulative);
                samplingRunItemRepository.save(item);

                sampleUnit++;
                selectionPoint = randomStart.add(interval.multiply(BigDecimal.valueOf(sampleUnit)))
                        .setScale(INTERVAL_SCALE, RoundingMode.HALF_UP);
            }
            if (sampleUnit >= sampleSize) {
                break;
            }
        }

        if (sampleUnit < sampleSize) {
            Booking last = population.get(population.size() - 1);
            while (sampleUnit < sampleSize) {
                SamplingRunItem item = new SamplingRunItem();
                item.setSamplingRunId(savedRun.getId());
                item.setSampleUnitIndex(sampleUnit);
                item.setSelectionPoint(selectionPoint);
                item.setBookingId(last.getId());
                item.setBookingAmount(last.getAmount());
                item.setCumulativeAmount(totalAmount);
                samplingRunItemRepository.save(item);

                sampleUnit++;
                selectionPoint = randomStart.add(interval.multiply(BigDecimal.valueOf(sampleUnit)))
                        .setScale(INTERVAL_SCALE, RoundingMode.HALF_UP);
            }
        }

        return savedRun;
    }

    public SamplingRun generateRandomSample(String runName, long populationSize, long sampleSize, long seed) {
        if (sampleSize <= 0) {
            throw new IllegalArgumentException("sampleSize must be > 0");
        }

        List<Booking> population = loadSamplingPopulation();
        if (sampleSize > population.size()) {
            throw new IllegalArgumentException("sampleSize must be <= effective population size");
        }

        List<Booking> shuffled = new ArrayList<>(population);
        Random random = new Random(seed);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Booking current = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, current);
        }

        SamplingRun savedRun = saveRun(
                runName,
                "RANDOM",
                seed,
                population.size(),
                sampleSize,
                "{\"method\":\"RANDOM\",\"requestedPopulationSize\":" + populationSize
                        + ",\"effectivePopulationSize\":" + population.size()
                        + ",\"sampleSize\":" + sampleSize
                        + ",\"seed\":" + seed
                        + "}"
        );

        for (int i = 0; i < sampleSize; i++) {
            Booking booking = shuffled.get(i);
            SamplingRunItem item = new SamplingRunItem();
            item.setSamplingRunId(savedRun.getId());
            item.setSampleUnitIndex(i);
            item.setSelectionPoint(BigDecimal.valueOf(i + 1L));
            item.setBookingId(booking.getId());
            item.setBookingAmount(booking.getAmount());
            item.setCumulativeAmount(booking.getAmount());
            samplingRunItemRepository.save(item);
        }

        return savedRun;
    }

    public SamplingRun generateStratifiedSample(String runName,
                                                long populationSize,
                                                long sampleSize,
                                                long seed,
                                                int stratumCount) {
        if (sampleSize <= 0) {
            throw new IllegalArgumentException("sampleSize must be > 0");
        }
        if (stratumCount <= 0) {
            throw new IllegalArgumentException("stratumCount must be > 0");
        }

        List<Booking> population = loadSamplingPopulation();
        if (sampleSize > population.size()) {
            throw new IllegalArgumentException("sampleSize must be <= effective population size");
        }

        List<Booking> sortedByAmount = new ArrayList<>(population);
        sortedByAmount.sort(Comparator.comparing(Booking::getAmount).thenComparing(Booking::getId));

        Map<Integer, List<Booking>> strata = new HashMap<>();
        for (int i = 0; i < stratumCount; i++) {
            strata.put(i, new ArrayList<>());
        }

        for (int i = 0; i < sortedByAmount.size(); i++) {
            int stratumIndex = Math.min((int) ((long) i * stratumCount / sortedByAmount.size()), stratumCount - 1);
            strata.get(stratumIndex).add(sortedByAmount.get(i));
        }

        long assigned = 0;
        Map<Integer, Long> targetSamplesPerStratum = new HashMap<>();
        for (int i = 0; i < stratumCount; i++) {
            List<Booking> stratum = strata.get(i);
            long target = Math.min(stratum.size(), (sampleSize * stratum.size()) / population.size());
            targetSamplesPerStratum.put(i, target);
            assigned += target;
        }

        while (assigned < sampleSize) {
            int bestIndex = -1;
            long bestRemaining = Long.MIN_VALUE;
            for (int i = 0; i < stratumCount; i++) {
                long remaining = strata.get(i).size() - targetSamplesPerStratum.get(i);
                if (remaining > bestRemaining) {
                    bestRemaining = remaining;
                    bestIndex = i;
                }
            }
            if (bestIndex < 0 || bestRemaining <= 0) {
                break;
            }
            targetSamplesPerStratum.put(bestIndex, targetSamplesPerStratum.get(bestIndex) + 1);
            assigned++;
        }

        SamplingRun savedRun = saveRun(
                runName,
                "STRATIFIED",
                seed,
                population.size(),
                sampleSize,
                "{\"method\":\"STRATIFIED\",\"requestedPopulationSize\":" + populationSize
                        + ",\"effectivePopulationSize\":" + population.size()
                        + ",\"sampleSize\":" + sampleSize
                        + ",\"seed\":" + seed
                        + ",\"stratumCount\":" + stratumCount
                        + "}"
        );

        int sampleUnitIndex = 0;
        for (int i = 0; i < stratumCount; i++) {
            List<Booking> stratum = strata.get(i);
            if (stratum.isEmpty()) {
                continue;
            }

            List<Booking> shuffled = new ArrayList<>(stratum);
            Random random = new Random(seed + i);
            for (int j = shuffled.size() - 1; j > 0; j--) {
                int k = random.nextInt(j + 1);
                Booking current = shuffled.get(j);
                shuffled.set(j, shuffled.get(k));
                shuffled.set(k, current);
            }

            long target = targetSamplesPerStratum.get(i);
            for (int j = 0; j < target; j++) {
                Booking booking = shuffled.get(j);
                SamplingRunItem item = new SamplingRunItem();
                item.setSamplingRunId(savedRun.getId());
                item.setSampleUnitIndex(sampleUnitIndex);
                item.setSelectionPoint(BigDecimal.valueOf(i + 1L));
                item.setBookingId(booking.getId());
                item.setBookingAmount(booking.getAmount());
                item.setCumulativeAmount(booking.getAmount());
                samplingRunItemRepository.save(item);
                sampleUnitIndex++;
            }
        }

        return savedRun;
    }

    private List<Booking> loadSamplingPopulation() {
        return bookingRepository.findAll().stream()
                .filter(booking -> booking.getId() != null)
                .filter(booking -> booking.getAmount() != null && booking.getAmount().compareTo(ZERO) > 0)
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .collect(Collectors.toList());
    }

    private SamplingRun saveRun(String runName,
                                String strategy,
                                long seed,
                                long effectivePopulationSize,
                                long sampleSize,
                                String parametersJson) {
        SamplingRun run = new SamplingRun();
        run.setRunName(runName);
        run.setSamplingStrategy(strategy);
        run.setSeed(seed);
        run.setPopulationSize(effectivePopulationSize);
        run.setSampleSize(sampleSize);
        run.setParametersJson(parametersJson);
        return samplingRunRepository.save(run);
    }
}
