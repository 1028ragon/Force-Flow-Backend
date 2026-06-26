package ForceFlow.Military.unit;

import ForceFlow.Military.dto.requestDto.UnitCreateRequest;
import ForceFlow.Military.dto.responseDto.UnitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @PostMapping
    public ResponseEntity<UnitResponse> createUnit(
            @RequestBody UnitCreateRequest request
    ) {
        UnitResponse response = unitService.createUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{unitId}")
    public ResponseEntity<UnitResponse> getUnit(
            @PathVariable Long unitId
    ) {
        UnitResponse response = unitService.getUnit(unitId);
        return ResponseEntity.ok(response);
    }
}