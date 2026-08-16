package de.bohnottensen.financialaudit.application.usecase.dashboard;
import de.bohnottensen.financialaudit.infrastructure.persistence.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
@Service public class DashboardService {
 private final BookingRepository bookings; private final FindingRepository findings; private final WorkpaperRepository workpapers; private final SamplingRunRepository sampling;
 public DashboardService(BookingRepository b,FindingRepository f,WorkpaperRepository w,SamplingRunRepository s){bookings=b;findings=f;workpapers=w;sampling=s;}
 public Metrics metrics(){return new Metrics(bookings.count(),findings.count(),findings.countByRemediationStatusIn(List.of("OPEN","IN_PROGRESS","READY_FOR_REVIEW","REJECTED")),findings.findByRemediationDueDateBeforeAndRemediationStatusNot(LocalDate.now(),"CLOSED").size(),workpapers.count(),sampling.count());}
 public record Metrics(long totalBookings,long totalFindings,long openRemediation,long overdueRemediation,long workpapers,long samplingRuns){}
}
