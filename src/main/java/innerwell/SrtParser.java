package innerwell;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class SrtParser {

    private final static String TIME_PATTERN = "HH:mm:ss,SSS";
    private final static String TIME_RANGE_SPLITTER = " --> ";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

    private final Duration offset;

    public SrtParser(long offset, ChronoUnit unit) {
        this(Duration.of(offset, unit));
    }

    public SrtParser(Duration duration) {
        this.offset = duration;
    }

    public void parse(Path input, Path output) throws IOException {
        State state = State.COUNTER;

        try (BufferedReader reader = new BufferedReader(new FileReader(input.toFile()));
             BufferedWriter writer = output != null ? new BufferedWriter(new FileWriter(output.toFile())) : null ) {

            String line = reader.readLine();

            // remove UTF-8 BOM (Begin of File Marker)
            // String firstLine = line.replaceFirst("^\uFEFF", "");

            while (line != null) {
                Result result = next(state, line);

                if (writer != null) {
                    writer.write(result.result + '\n');
                }

                state = result.state;
                line = reader.readLine();
            }
        }
    }

    private Result next(State current, String line) throws IOException {
        switch (current) {
            case COUNTER:
                System.out.printf("State=%s value=%s\n", current, line);
                return Result.of(State.TIME_RANGE, line);

            case TIME_RANGE:
                String adjusted = adjustTimeLine(line);
                System.out.println("State=" + current);
                System.out.println("\tOld=" + line);
                System.out.println("\tNew=" + adjusted);
                return Result.of(State.SUBTITLE, adjusted);

            case SUBTITLE:
                if (!line.isEmpty()) {
                    System.out.printf("State=%s value=%s\n", current, line);
                } else {
                    System.out.println();
                }
                return Result.of(line.isEmpty() ? State.COUNTER : current, line);
        }

        throw new IOException("Unrecognized state=" + current);
    }

    private String adjustTimeLine(String line) throws IOException {
        String[] timeRange = line.split(TIME_RANGE_SPLITTER);
        if (timeRange.length != 2) {
            throw new IOException("Time range is unrecognized: " + line);
        }

        LocalTime fromTime = LocalTime.parse(timeRange[0].trim(), formatter);
        LocalTime toTime = LocalTime.parse(timeRange[1].trim(), formatter);
        Duration duration = Duration.between(fromTime, toTime);

        LocalTime adjustedFromTime = fromTime.plus(offset);
        LocalTime adjustedToTime = adjustedFromTime.plus(duration);

        String adjustedFrom = adjustedFromTime.format(formatter);
        String adjustedTo = adjustedToTime.format(formatter);

        return String.format("%s%s%s", adjustedFrom, TIME_RANGE_SPLITTER, adjustedTo);
    }

    static class Result {
        final State state;
        final String result;

        Result(State state, String result) {
            this.state = state;
            this.result = result;
        }

        static Result of(State state, String result) {
            return new Result(state, result);
        }
    }

    private enum State {
        COUNTER,
        TIME_RANGE,
        SUBTITLE
    }
}
