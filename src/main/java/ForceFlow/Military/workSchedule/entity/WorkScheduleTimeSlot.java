package ForceFlow.Military.workSchedule.entity;

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
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
@Entity
@Table(name = "work_schedule_time_slot")
public class WorkScheduleTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setting_id", nullable = false)
    private WorkScheduleSetting setting;

    @Column(name = "slot_order", nullable = false)
    private Integer slotOrder;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "required_count", nullable = false)
    private Integer requiredCount;

    @OneToMany(mappedBy = "timeSlot", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<WorkScheduleSlotRole> allowedRoles = new ArrayList<>();

    protected WorkScheduleTimeSlot() {
    }

    public WorkScheduleTimeSlot(
            Integer slotOrder,
            LocalTime startTime,
            LocalTime endTime,
            Integer requiredCount,
            List<WorkScheduleSlotRole> allowedRoles
    ) {
        this.slotOrder = slotOrder;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredCount = requiredCount;
        replaceAllowedRoles(allowedRoles);
    }

    void assignSetting(WorkScheduleSetting setting) {
        this.setting = setting;
    }

    private void replaceAllowedRoles(List<WorkScheduleSlotRole> roles) {
        this.allowedRoles.clear();
        roles.forEach(role -> {
            role.assignTimeSlot(this);
            this.allowedRoles.add(role);
        });
    }
}
