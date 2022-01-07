package innerwell;

import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SrtOffsetter {

    public static void main(String[] args) {
        Config config = initialize(args);

        if (!new File(config.input).exists()) {
            System.out.println("Input path provided does not exist! " + config.input);
            System.exit(2);
        }

        try {
            new SrtParser(config.duration).parse(Paths.get(config.input), Paths.get(config.output));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(3);
        }
    }

    private static Config initialize(String[] args) {
        Options options = new Options()
                .addOption(Option.builder("f")
                        .required()
                        .longOpt("file")
                        .desc("Input srt file to tweak")
                        .hasArg()
                        .build())
                .addOption(Option.builder("o")
                        .required(false)
                        .longOpt("output")
                        .desc("Output srt filename")
                        .hasArg()
                        .build())
                .addOption(Option.builder("t")
                        .required()
                        .longOpt("offset")
                        .desc("Amount of time to offset. m=minutes, s=seconds, ss=fraction of second. ie '2m', '-3s', '234ss'")
                        .hasArg()
                        .build());

        try {
            final CommandLine commandLine = new DefaultParser().parse(options, args);

            final String input = commandLine.getOptionValue("file");
            final String output = commandLine.getOptionValue("output");
            final String durationString = commandLine.getOptionValue("offset");

            Duration duration = parseDuration(durationString);
            return new Config(input, output, duration);

        } catch (ParseException e) {
            new HelpFormatter().printHelp(me(), options, true);
            System.out.println(e.getMessage());
        }

        System.exit(1);
        return null;
    }

    private static Duration parseDuration(String durationString) throws ParseException {
        Pattern pattern = Pattern.compile("(-?\\d+)(\\w)");
        Matcher matcher = pattern.matcher("-20s");

        if (!matcher.find() || matcher.groupCount() != 2) {
            throw new ParseException("Unable to parse duration string=" + durationString + ". Expected ie 1m, -2m, 20ss");
        }

        final long amount = Long.parseLong(matcher.group(1));
        final String units = matcher.group(2);

        switch (units) {
            case "m":
                return Duration.of(amount, ChronoUnit.MINUTES);
            case "s":
                return Duration.of(amount, ChronoUnit.SECONDS);
            case "ss":
                return Duration.of(amount, ChronoUnit.NANOS);
            default:
                throw new ParseException("Unsupported time unit. Only supported units are m, s and ss: " + durationString);
        }
    }

    private static String me() {
        return new File(SrtOffsetter.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
    }

    private static class Config {
        public final String input;
        public final String output;
        public final Duration duration;

        Config(String input, String output, Duration duration) {
            this.input = input;
            this.output = output;
            this.duration = duration;
        }
    }
}