package ForceFlow.Military.workSchedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "work_schedule_slot_role")
public class WorkScheduleSlotRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_role_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private WorkScheduleTimeSlot timeSlot;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @Column(name = "required_count", nullable = false)
    private Integer requiredCount = 1;

    protected WorkScheduleSlotRole() {
    }

    public WorkScheduleSlotRole(String role) {
        this.role = role;
        this.requiredCount = 1;
    }

    void assignTimeSlot(WorkScheduleTimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }
}
