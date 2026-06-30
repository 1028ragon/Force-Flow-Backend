package ForceFlow.Military.schedule;

import ForceFlow.Military.dto.requestDto.ScheduleCreateRequest;
import ForceFlow.Military.dto.responseDto.ScheduleResponse;
import ForceFlow.Military.entity.Schedule;
import ForceFlow.Military.entity.User;
import ForceFlow.Military.repository.ScheduleRepository;
import ForceFlow.Military.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    @Transactional
    public ScheduleResponse createSchedule(ScheduleCreateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("병사를 찾을 수 없습니다."));

        Schedule schedule = Schedule.builder()
                .user(user)
                .type(request.type())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .status(request.status())
                .reason(request.reason())
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);

        return toResponse(savedSchedule);
    }

    public List<ScheduleResponse> getSchedulesByUser(Long userId) {
        return scheduleRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ScheduleResponse toResponse(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getUser().getId(),
                schedule.getType(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getStatus(),
                schedule.getReason(),
                schedule.getCreatedAt()
        );
    }
}