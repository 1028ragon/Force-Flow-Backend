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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Entity
@Table(name = "unit_setting")
public class UnitSetting {

    private static final String DEFAULT_DUTY_TYPE = "불침번";
    private static final int DEFAULT_REQUIRED_COUNT = 3;
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(22, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(6, 0);
    private static final int DEFAULT_LOOKBACK_DAYS = 7;
    private static final boolean DEFAULT_PREVENT_CONSECUTIVE = true;
    private static final int DEFAULT_MAX_DUTY_COUNT = 5;
    private static final String DEFAULT_EXCLUDE_STATUSES = "휴가,외출,외박,교육,훈련,입원,외진";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false, unique = true)
    private Unit unit;

    @Column(name = "duty_type", nullable = false, length = 30)
    private String dutyType;

    @Column(name = "required_count", nullable = false)
    private Integer requiredCount;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "lookback_days", nullable = false)
    private Integer lookbackDays;

    @Column(name = "prevent_consecutive", nullable = false)
    private Boolean preventConsecutive;

    @Column(name = "max_duty_count", nullable = false)
    private Integer maxDutyCount;

    @Column(name = "exclude_statuses", nullable = false, length = 255)
    private String excludeStatuses;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected UnitSetting() {
    }

    @Builder
    public UnitSetting(
            Unit unit,
            String dutyType,
            Integer requiredCount,
            LocalTime startTime,
            LocalTime endTime,
            Integer lookbackDays,
            Boolean preventConsecutive,
            Integer maxDutyCount,
            List<String> excludeStatuses
    ) {
        this.unit = unit;
        this.dutyType = dutyType != null ? dutyType : DEFAULT_DUTY_TYPE;
        this.requiredCount = requiredCount != null ? requiredCount : DEFAULT_REQUIRED_COUNT;
        this.startTime = startTime != null ? startTime : DEFAULT_START_TIME;
        this.endTime = endTime != null ? endTime : DEFAULT_END_TIME;
        this.lookbackDays = lookbackDays != null ? lookbackDays : DEFAULT_LOOKBACK_DAYS;
        this.preventConsecutive = preventConsecutive != null ? preventConsecutive : DEFAULT_PREVENT_CONSECUTIVE;
        this.maxDutyCount = maxDutyCount != null ? maxDutyCount : DEFAULT_MAX_DUTY_COUNT;
        this.excludeStatuses = joinStatuses(excludeStatuses);
    }

    public void update(
            String dutyType,
            Integer requiredCount,
            LocalTime startTime,
            LocalTime endTime,
            Integer lookbackDays,
            Boolean preventConsecutive,
            Integer maxDutyCount,
            List<String> excludeStatuses
    ) {
        this.dutyType = dutyType != null ? dutyType : DEFAULT_DUTY_TYPE;
        this.requiredCount = requiredCount != null ? requiredCount : DEFAULT_REQUIRED_COUNT;
        this.startTime = startTime != null ? startTime : DEFAULT_START_TIME;
        this.endTime = endTime != null ? endTime : DEFAULT_END_TIME;
        this.lookbackDays = lookbackDays != null ? lookbackDays : DEFAULT_LOOKBACK_DAYS;
        this.preventConsecutive = preventConsecutive != null ? preventConsecutive : DEFAULT_PREVENT_CONSECUTIVE;
        this.maxDutyCount = maxDutyCount != null ? maxDutyCount : DEFAULT_MAX_DUTY_COUNT;
        this.excludeStatuses = joinStatuses(excludeStatuses);
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getExcludeStatusList() {
        if (excludeStatuses == null || excludeStatuses.isBlank()) {
            return List.of();
        }

        return Arrays.asList(excludeStatuses.split(","));
    }

    private String joinStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return DEFAULT_EXCLUDE_STATUSES;
        }

        return String.join(",", statuses);
    }
}
