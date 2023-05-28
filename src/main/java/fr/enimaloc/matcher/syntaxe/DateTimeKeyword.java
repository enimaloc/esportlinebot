package fr.enimaloc.matcher.syntaxe;

import java.util.StringJoiner;

public class DateTimeKeyword {

    public static Keyword[] getKeywords() {
        return new Keyword[]{
                now(),
                format(),
                until(),
                since()
        };
    }

    public static Keyword format() {
        return new Keyword("time.format", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1) {
                throw new IllegalArgumentException("format() takes only one argument");
            }
            long time = Long.parseLong(instruction.getArgs(0).mapLeft(i -> i.run(matcher)).getAny(String.class));
            StringJoiner joiner = new StringJoiner(":");
            long hours = time / 3600000;
            if (hours > 0) {
                joiner.add(String.format("%02d", hours));
            }
            long minutes = (time % 3600000) / 60000;
            joiner.add(String.format("%02d", minutes));
            long seconds = (time % 60000) / 1000;
            joiner.add(String.format("%02d", seconds));
            return joiner.toString();
        });
    }

    public static Keyword now() {
        return new Keyword("date.now", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 0) {
                throw new IllegalArgumentException("now() takes no arguments");
            }
            return String.valueOf(System.currentTimeMillis());
        });
    }

    public static Keyword since() {
        return new Keyword("date.since", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1) {
                throw new IllegalArgumentException("since() takes only one argument");
            }
            long time = Long.parseLong(instruction.getArgs(0).mapLeft(i -> i.run(matcher)).getAny(String.class));
            return String.valueOf(System.currentTimeMillis() - time);
        });
    }

    public static Keyword until() {
        return new Keyword("date.until", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1) {
                throw new IllegalArgumentException("until() takes only one argument");
            }
            long time = Long.parseLong(instruction.getArgs(0).mapLeft(i -> i.run(matcher)).getAny(String.class));
            return String.valueOf(time - System.currentTimeMillis());
        });
    }
}
