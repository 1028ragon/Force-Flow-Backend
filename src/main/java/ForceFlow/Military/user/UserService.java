package ForceFlow.Military.user;

import ForceFlow.Military.dto.requestDto.UserCreateRequest;
import ForceFlow.Military.dto.responseDto.UserResponse;
import ForceFlow.Military.entity.Unit;
import ForceFlow.Military.entity.User;
import ForceFlow.Military.repository.UnitRepository;
import ForceFlow.Military.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UnitRepository unitRepository;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByServiceNumber(request.serviceNumber())) {
            throw new IllegalArgumentException("이미 존재하는 군번입니다.");
        }

        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new IllegalArgumentException("부대를 찾을 수 없습니다."));

        User user = User.builder()
                .unit(unit)
                .serviceNumber(request.serviceNumber())
                .name(request.name())
                .rankName(request.rankName())
                .role(request.role())
                .currentStatus(request.currentStatus())
                .phone(request.phone())
                .build();

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public List<UserResponse> getUsersByUnit(Long unitId) {
        List<Long> unitIds = unitRepository.findAllSubUnitIds(unitId);

        return userRepository.findByUnitIdIn(unitIds)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUnit().getId(),
                user.getServiceNumber(),
                user.getName(),
                user.getRankName(),
                user.getRole(),
                user.getCurrentStatus(),
                user.getPhone(),
                user.getCreatedAt()
        );
    }
}