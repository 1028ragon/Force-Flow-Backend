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
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Entity
@Table(name = "users")
public class User {

    private static final String DEFAULT_ROLE = "SOLDIER";
    private static final String DEFAULT_CURRENT_STATUS = "부대내";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "service_number", nullable = false, unique = true, length = 30)
    private String serviceNumber;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "rank_name", nullable = false, length = 20)
    private String rankName;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @Column(name = "current_status", nullable = false, length = 20)
    private String currentStatus;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    @Builder
    public User(
            Unit unit,
            String serviceNumber,
            String name,
            String rankName,
            String role,
            String currentStatus,
            String phone
    ) {
        this.unit = unit;
        this.serviceNumber = serviceNumber;
        this.name = name;
        this.rankName = rankName;
        this.role = role != null ? role : DEFAULT_ROLE;
        this.currentStatus = currentStatus != null ? currentStatus : DEFAULT_CURRENT_STATUS;
        this.phone = phone;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void changeStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }
}