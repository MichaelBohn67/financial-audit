package de.bohnottensen.financialaudit.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_actions")
public class ReviewAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workpaper_id", nullable = false)
    private Workpaper workpaper;

    @Column(nullable = false, length = 255)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private ReviewActionType action;

    @Column(length = 1024)
    private String comment;

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

    public Workpaper getWorkpaper() {
        return workpaper;
    }

    public void setWorkpaper(Workpaper workpaper) {
        this.workpaper = workpaper;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public ReviewActionType getAction() {
        return action;
    }

    public void setAction(ReviewActionType action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
