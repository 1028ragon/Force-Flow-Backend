package ForceFlow.Military.workSchedule.exception;

public class WorkScheduleServiceException extends RuntimeException {

    public WorkScheduleServiceException(String message) {
        super(message);
    }

    public WorkScheduleServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
