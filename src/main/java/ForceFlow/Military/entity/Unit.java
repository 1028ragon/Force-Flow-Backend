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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "unit")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_unit_id")
    private Unit parentUnit;

    @OneToMany(mappedBy = "parentUnit")
    private List<Unit> childUnits = new ArrayList<>();

    @Column(name = "unit_name", nullable = false, length = 100)
    private String unitName;

    @Column(name = "unit_type", nullable = false, length = 30)
    private String unitType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Unit() {
    }

    public Unit(Unit parentUnit, String unitName, String unitType) {
        this.parentUnit = parentUnit;
        this.unitName = unitName;
        this.unitType = unitType;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Unit getParentUnit() {
        return parentUnit;
    }

    public List<Unit> getChildUnits() {
        return childUnits;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getUnitType() {
        return unitType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

