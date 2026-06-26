package ForceFlow.Military.user;

import ForceFlow.Military.dto.requestDto.UserStatusUpdateRequest;
import ForceFlow.Military.dto.responseDto.UserStatusResponse;
import ForceFlow.Military.entity.User;
import ForceFlow.Military.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserStatusService {

    private final UserRepository userRepository;

    @Transactional
    public UserStatusResponse updateStatus(
            Long userId,
            UserStatusUpdateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("병사를 찾을 수 없습니다."));

        user.changeStatus(request.currentStatus());

        return new UserStatusResponse(
                user.getId(),
                user.getName(),
                user.getCurrentStatus()
        );
    }
}