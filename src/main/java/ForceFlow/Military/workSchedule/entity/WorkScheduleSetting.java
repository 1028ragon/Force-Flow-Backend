package ForceFlow.Military.workSchedule.entity;

import ForceFlow.Military.entity.Unit;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "work_schedule_setting",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_work_schedule_setting_unit_duty_type",
                columnNames = {"unit_id", "duty_type"}
        )
)
public class WorkScheduleSetting {

    private static final String DEFAULT_EXCLUDE_STATUSES = "휴가,외출,외박,교육,훈련,입원,외진";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "duty_type", nullable = false, length = 30)
    private String dutyType;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "lookback_days", nullable = false)
    private Integer lookbackDays;

    @Column(name = "prevent_consecutive", nullable = false)
    private Boolean preventConsecutive;

    @Column(name = "max_duty_count", nullable = false)
    private Integer maxDutyCount;

    @Column(name = "exclude_statuses", nullable = false, length = 255)
    private String excludeStatuses;

    @OneToMany(mappedBy = "setting", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<WorkScheduleTimeSlot> timeSlots = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected WorkScheduleSetting() {
    }

    public WorkScheduleSetting(
            Unit unit,
            String dutyType,
            String description,
            Integer lookbackDays,
            Boolean preventConsecutive,
            Integer maxDutyCount,
            List<String> excludeStatuses
    ) {
        this.unit = unit;
        update(dutyType, description, lookbackDays, preventConsecutive, maxDutyCount, excludeStatuses);
    }

    public void update(
            String dutyType,
            String description,
            Integer lookbackDays,
            Boolean preventConsecutive,
            Integer maxDutyCount,
            List<String> excludeStatuses
    ) {
        this.dutyType = dutyType;
        this.description = description;
        this.lookbackDays = lookbackDays;
        this.preventConsecutive = preventConsecutive;
        this.maxDutyCount = maxDutyCount;
        this.excludeStatuses = joinStatuses(excludeStatuses);
    }

    public void replaceTimeSlots(List<WorkScheduleTimeSlot> slots) {
        this.timeSlots.clear();
        slots.forEach(slot -> {
            slot.assignSetting(this);
            this.timeSlots.add(slot);
        });
    }

    public Integer getRequiredCount() {
        return timeSlots.stream()
                .mapToInt(WorkScheduleTimeSlot::getRequiredCount)
                .sum();
    }

    public LocalTime getStartTime() {
        return timeSlots.stream()
                .min((left, right) -> Integer.compare(left.getSlotOrder(), right.getSlotOrder()))
                .map(WorkScheduleTimeSlot::getStartTime)
                .orElse(null);
    }

    public LocalTime getEndTime() {
        return timeSlots.stream()
                .max((left, right) -> Integer.compare(left.getSlotOrder(), right.getSlotOrder()))
                .map(WorkScheduleTimeSlot::getEndTime)
                .orElse(null);
    }

    public List<String> getExcludeStatusList() {
        if (excludeStatuses == null || excludeStatuses.isBlank()) {
            return List.of();
        }
        return Arrays.asList(excludeStatuses.split(","));
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String joinStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return DEFAULT_EXCLUDE_STATUSES;
        }
        return String.join(",", statuses);
    }
}
