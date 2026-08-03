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
import java.util.List;
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
}
