package ForceFlow.Military.workSchedule.exception;

public class OpenAiScheduleClientException extends RuntimeException {

    public OpenAiScheduleClientException(String message) {
        super(message);
    }

    public OpenAiScheduleClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
