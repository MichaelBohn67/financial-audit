package de.bohnottensen.financialaudit.infrastructure.web;

import de.bohnottensen.financialaudit.application.usecase.sampling.SamplingService;
import de.bohnottensen.financialaudit.domain.model.SamplingRun;
import de.bohnottensen.financialaudit.domain.model.SamplingRunItem;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunItemRepository;
import de.bohnottensen.financialaudit.infrastructure.persistence.SamplingRunRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sampling")
@PreAuthorize("hasAnyRole('AUDITOR','LEAD_AUDITOR','ADMIN')")
public class SamplingApiController {
    private final SamplingService samplingService;
    private final SamplingRunRepository samplingRuns;
    private final SamplingRunItemRepository samplingItems;

    public SamplingApiController(SamplingService samplingService, SamplingRunRepository samplingRuns,
                                 SamplingRunItemRepository samplingItems) {
        this.samplingService = samplingService;
        this.samplingRuns = samplingRuns;
        this.samplingItems = samplingItems;
    }

    @PostMapping("/mus")
    public SamplingRun createMus(@Valid @RequestBody MusRequest request) {
        return samplingService.generateMusSample(request.runName(), request.populationSize(),
                request.sampleSize(), request.seed());
    }

    @GetMapping("/runs")
    public List<SamplingRun> runs() {
        return samplingRuns.findAll();
    }

    @GetMapping("/runs/{id}/items")
    public List<SamplingRunItem> items(@PathVariable Long id) {
        if (!samplingRuns.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sampling run not found");
        }
        return samplingItems.findBySamplingRunIdOrderBySampleUnitIndex(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalidSamplingRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    public record MusRequest(@NotBlank String runName, @Positive long populationSize,
                             @Positive long sampleSize, long seed) {
    }

    public record ErrorResponse(String message) {
    }
}
