package de.bohnottensen.financialaudit.application.usecase.finding;

import de.bohnottensen.financialaudit.application.usecase.audit.AuditTrailWriter;
import de.bohnottensen.financialaudit.domain.model.Finding;
import de.bohnottensen.financialaudit.domain.model.Workpaper;
import de.bohnottensen.financialaudit.infrastructure.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
public class FindingManagementService {
    private final FindingRepository findings; private final WorkpaperRepository workpapers; private final AuditTrailWriter audit;
    public FindingManagementService(FindingRepository findings, WorkpaperRepository workpapers, AuditTrailWriter audit) { this.findings=findings; this.workpapers=workpapers; this.audit=audit; }
    public Finding get(Long id) { return findings.findById(id).orElseThrow(); }
    @Transactional public Finding linkWorkpaper(Long id, Long workpaperId, String actor) { Finding f=get(id); Workpaper w=workpapers.findById(workpaperId).orElseThrow(); f.setWorkpaper(w); return change(f, actor, "LINK_WORKPAPER", f.getRemediationStatus()); }
    @Transactional public Finding assign(Long id, String owner, LocalDate dueDate, String actor) { Finding f=get(id); f.setRemediationOwner(owner); f.setRemediationDueDate(dueDate); return change(f, actor, "ASSIGN_REMEDIATION", "OPEN"); }
    @Transactional public Finding updatePlan(Long id, String plan, String actor) { Finding f=get(id); f.setRemediationPlan(plan); return change(f, actor, "UPDATE_REMEDIATION_PLAN", f.getRemediationStatus()); }
    @Transactional public Finding transition(Long id, String target, String comment, String actor) { Finding f=get(id); String from=f.getRemediationStatus(); if (!allowed(from,target)) throw new IllegalStateException("Invalid remediation transition: "+from+" -> "+target); f.setRemediationStatus(target); f.setResolutionComment(comment); if ("RESOLVED".equals(target)) { f.setResolvedAt(java.time.LocalDateTime.now()); f.setResolvedBy(actor); } return change(f, actor, "REMEDIATION_"+target, target); }
    private Finding change(Finding f,String actor,String event,String current) { Finding saved=findings.save(f); audit.record("Finding",saved.getId(),event,actor,"Finding remediation changed",null,current); return saved; }
    private boolean allowed(String from,String to) { return switch(from) { case "OPEN" -> to.equals("IN_PROGRESS"); case "IN_PROGRESS" -> to.equals("READY_FOR_REVIEW"); case "READY_FOR_REVIEW" -> to.equals("RESOLVED") || to.equals("REJECTED"); case "REJECTED" -> to.equals("IN_PROGRESS"); case "RESOLVED" -> to.equals("CLOSED"); default -> false; }; }
}
