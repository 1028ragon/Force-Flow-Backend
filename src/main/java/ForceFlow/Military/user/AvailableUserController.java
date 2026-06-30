package ForceFlow.Military.user;

import ForceFlow.Military.dto.responseDto.UserResponse;
import ForceFlow.Military.repository.UnitRepository;
import ForceFlow.Military.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AvailableUserController {

    private final UserRepository userRepository;
    private final UnitRepository unitRepository;

    @GetMapping("/api/units/{unitId}/in-unit-users")
    public ResponseEntity<List<UserResponse>> getAvailableUsers(
            @PathVariable Long unitId
    ) {
        List<Long> unitIds = unitRepository.findAllSubUnitIds(unitId);
        List<UserResponse> response = userRepository.findByUnitIdInAndCurrentStatus(unitIds, "부대내")
                .stream()
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getUnit().getId(),
                        u.getServiceNumber(),
                        u.getName(),
                        u.getRankName(),
                        u.getRole(),
                        u.getCurrentStatus(),
                        u.getPhone(),
                        u.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }
}
