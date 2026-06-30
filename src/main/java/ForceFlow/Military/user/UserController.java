package ForceFlow.Military.user;

import ForceFlow.Military.dto.requestDto.UserCreateRequest;
import ForceFlow.Military.dto.requestDto.UserStatusUpdateRequest;
import ForceFlow.Military.dto.responseDto.UserResponse;
import ForceFlow.Military.dto.responseDto.UserStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserStatusService userStatusService;

    @PostMapping("/api/users")
    public ResponseEntity<UserResponse> createUser(
            @RequestBody UserCreateRequest request
    ) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/units/{unitId}/users")
    public ResponseEntity<List<UserResponse>> getUsersByUnit(
            @PathVariable Long unitId
    ) {
        List<UserResponse> response = userService.getUsersByUnit(unitId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/users/{userId}/status")
    public ResponseEntity<UserStatusResponse> updateStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusUpdateRequest request
    ) {
        UserStatusResponse response = userStatusService.updateStatus(userId, request);
        return ResponseEntity.ok(response);
    }
}