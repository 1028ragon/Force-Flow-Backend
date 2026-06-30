package ForceFlow.Military.workSchedule.service;

import java.time.LocalTime;
import org.springframework.stereotype.Component;

@Component
public class WorkScheduleDutyFatigueCalculator {

    private static final int DAY_SCORE = 1;
    private static final int EVENING_SCORE = 2;
    private static final int NIGHT_SCORE = 3;
    private static final int DAWN_SCORE = 4;

    private static final int DAY_START = 6 * 60;
    private static final int EVENING_START = 18 * 60;
    private static final int NIGHT_START = 22 * 60;
    private static final int DAY_END = 24 * 60;

    public int calculate(LocalTime startTime, LocalTime endTime) {
        if (overlaps(startTime, endTime, 0, DAY_START)) {
            return DAWN_SCORE;
        }
        if (overlaps(startTime, endTime, NIGHT_START, DAY_END)) {
            return NIGHT_SCORE;
        }
        if (overlaps(startTime, endTime, EVENING_START, NIGHT_START)) {
            return EVENING_SCORE;
        }
        return DAY_SCORE;
    }

    private boolean overlaps(LocalTime startTime, LocalTime endTime, int windowStartMinute, int windowEndMinute) {
        int startMinute = toMinute(startTime);
        int endMinute = toMinute(endTime);
        if (endMinute <= startMinute) {
            return overlapsRange(startMinute, DAY_END, windowStartMinute, windowEndMinute)
                    || overlapsRange(0, endMinute, windowStartMinute, windowEndMinute);
        }

        return overlapsRange(startMinute, endMinute, windowStartMinute, windowEndMinute);
    }

    private boolean overlapsRange(int startMinute, int endMinute, int windowStartMinute, int windowEndMinute) {
        return startMinute < windowEndMinute && windowStartMinute < endMinute;
    }

    private int toMinute(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}
