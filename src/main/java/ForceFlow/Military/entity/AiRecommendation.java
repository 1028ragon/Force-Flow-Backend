package ForceFlow.Military.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Entity
@Table(name = "ai_recommendation")
public class AiRecommendation {

    private static final String DEFAULT_STATUS = "추천";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "duty_date", nullable = false)
    private LocalDate dutyDate;

    @Column(name = "duty_type", nullable = false, length = 30)
    private String dutyType;

    @Column(name = "required_count", nullable = false)
    private Integer requiredCount;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "warning_message", length = 1000)
    private String warningMessage;

    @Column(name = "request_json", columnDefinition = "TEXT")
    private String requestJson;

    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    @OneToMany(mappedBy = "aiRecommendation")
    private List<DutyAssignment> assignments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AiRecommendation() {
    }

    @Builder
    public AiRecommendation(
            Unit unit,
            LocalDate dutyDate,
            String dutyType,
            Integer requiredCount,
            LocalTime startTime,
            LocalTime endTime,
            String status,
            String warningMessage,
            String requestJson,
            String responseJson
    ) {
        this.unit = unit;
        this.dutyDate = dutyDate;
        this.dutyType = dutyType;
        this.requiredCount = requiredCount;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status != null ? status : DEFAULT_STATUS;
        this.warningMessage = warningMessage;
        this.requestJson = requestJson;
        this.responseJson = responseJson;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
