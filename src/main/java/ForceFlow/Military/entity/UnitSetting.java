package ForceFlow.Military.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.StringJoiner;

@Entity
@Table(name = "unit_setting")
public class UnitSetting {

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
        this.dutyType = dutyType;
        this.requiredCount = requiredCount;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lookbackDays = lookbackDays;
        this.preventConsecutive = preventConsecutive;
        this.maxDutyCount = maxDutyCount;
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
        this.dutyType = dutyType;
        this.requiredCount = requiredCount;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}

