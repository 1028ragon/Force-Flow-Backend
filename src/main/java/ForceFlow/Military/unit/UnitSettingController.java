package ForceFlow.Military.unit;

import ForceFlow.Military.dto.requestDto.UnitSettingRequest;
import ForceFlow.Military.dto.responseDto.UnitSettingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/units/{unitId}/settings")
@RequiredArgsConstructor
public class UnitSettingController {

    private final UnitSettingService unitSettingService;

    @PutMapping
    public ResponseEntity<UnitSettingResponse> saveOrUpdateSetting(
            @PathVariable Long unitId,
            @RequestBody UnitSettingRequest request
    ) {
        UnitSettingResponse response = unitSettingService.saveOrUpdateSetting(unitId, request);
        return ResponseEntity.ok(response);
    }
}