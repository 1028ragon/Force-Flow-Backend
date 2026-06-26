package ForceFlow.Military.schedule;

import ForceFlow.Military.dto.requestDto.ScheduleCreateRequest;
import ForceFlow.Military.dto.responseDto.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/api/schedules")
    public ResponseEntity<ScheduleResponse> createSchedule(
            @RequestBody ScheduleCreateRequest request
    ) {
        ScheduleResponse response = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/users/{userId}/schedules")
    public ResponseEntity<List<ScheduleResponse>> getSchedulesByUser(
            @PathVariable Long userId
    ) {
        List<ScheduleResponse> response = scheduleService.getSchedulesByUser(userId);
        return ResponseEntity.ok(response);
    }
}