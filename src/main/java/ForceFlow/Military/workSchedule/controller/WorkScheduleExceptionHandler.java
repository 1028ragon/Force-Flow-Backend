package ForceFlow.Military.workSchedule.controller;

import ForceFlow.Military.workSchedule.dto.WorkScheduleErrorResponse;
import ForceFlow.Military.workSchedule.exception.InvalidAiRecommendationException;
import ForceFlow.Military.workSchedule.exception.OpenAiScheduleClientException;
import ForceFlow.Military.workSchedule.exception.WorkScheduleServiceException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "ForceFlow.Military.workSchedule")
public class WorkScheduleExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            IllegalArgumentException.class,
            InvalidAiRecommendationException.class,
            MethodArgumentNotValidException.class
    })
    public WorkScheduleErrorResponse handleBadRequest(Exception exception) {
        return new WorkScheduleErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage()
        );
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(OpenAiScheduleClientException.class)
    public WorkScheduleErrorResponse handleOpenAiError(OpenAiScheduleClientException exception) {
        return new WorkScheduleErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_GATEWAY.value(),
                HttpStatus.BAD_GATEWAY.getReasonPhrase(),
                exception.getMessage()
        );
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(WorkScheduleServiceException.class)
    public WorkScheduleErrorResponse handleServiceError(WorkScheduleServiceException exception) {
        return new WorkScheduleErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                exception.getMessage()
        );
    }
}
