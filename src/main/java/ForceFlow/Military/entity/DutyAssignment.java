package ForceFlow.Military.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "duty_assignment",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_duty_date_type",
                columnNames = {"user_id", "duty_date", "duty_type"}
        )
)
public class DutyAssignment {

    private static final String DEFAULT_STATUS = "추천";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "duty_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private AiRecommendation aiRecommendation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "duty_date", nullable = false)
    private LocalDate dutyDate;

    @Column(name = "duty_type", nullable = false, length = 30)
    private String dutyType;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "ai_reason", length = 500)
    private String aiReason;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DutyAssignment() {
    }

    @Builder
    public DutyAssignment(
            AiRecommendation aiRecommendation,
            User user,
            Unit unit,
            LocalDate dutyDate,
            String dutyType,
            LocalTime startTime,
            LocalTime endTime,
            String status,
            String aiReason
    ) {
        this.aiRecommendation = aiRecommendation;
        this.user = user;
        this.unit = unit;
        this.dutyDate = dutyDate;
        this.dutyType = dutyType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status != null ? status : DEFAULT_STATUS;
        this.aiReason = aiReason;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void approve() {
        this.status = "승인";
        this.approvedAt = LocalDateTime.now();
    }
}
