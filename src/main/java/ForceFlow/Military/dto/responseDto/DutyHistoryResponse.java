package ForceFlow.Military.dto.responseDto;

import ForceFlow.Military.dto.DutyHistoryItem;

import java.time.LocalDate;
import java.util.List;

public record DutyHistoryResponse(
        Long unitId,
        LocalDate startDate,
        LocalDate endDate,
        List<DutyHistoryItem> histories
) {}