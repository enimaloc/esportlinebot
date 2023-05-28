package fr.enimaloc.matcher.syntaxe;

public class StringUtilsKeyword {

    public static Keyword[] getKeywords() {
        return new Keyword[]{
                progressBar()
        };
    }

    public static Keyword progressBar() {
        return new Keyword("strutils.progressBar", (matcher, instruction) -> {
            if (instruction.getArgsCount() < 1 || instruction.getArgsCount() > 7) {
                throw new IllegalArgumentException("progressBar() takes only one to seven arguments");
            }
            int size = instruction.getArgs(0)
                    .mapLeft(i -> i.run(matcher))
                    .map(String.class, Integer::parseInt)
                    .getAny(Integer.class);
            String left = instruction.getArgsCount() >= 2
                    ? instruction.getArgs(1)
                    .mapLeft(i -> i.run(matcher))
                    .getAny(String.class)
                    : "▮";
            String center = instruction.getArgsCount() >= 3
                    ? instruction.getArgs(2)
                    .mapLeft(i -> i.run(matcher))
                    .getAny(String.class)
                    : left;
            String right = instruction.getArgsCount() >= 4
                    ? instruction.getArgs(3)
                    .mapLeft(i -> i.run(matcher))
                    .getAny(String.class)
                    : "▯";
            long max = instruction.getArgsCount() >= 5
                    ? instruction.getArgs(4)
                    .mapLeft(i -> i.run(matcher))
                    .map(String.class, Long::parseLong)
                    .getAny(Long.class)
                    : 0;
            long value = instruction.getArgsCount() >= 6
                    ? instruction.getArgs(5)
                    .mapLeft(i -> i.run(matcher))
                    .map(String.class, s -> s.contains(".") ? s.substring(0, s.indexOf('.')) : s)
                    .map(String.class, Long::parseLong)
                    .getAny(Long.class)
                    : 10;
            boolean undetermined = instruction.getArgsCount() == 7
                    && instruction.getArgs(6)
                    .mapLeft(i -> i.run(matcher))
                    .map(String.class, Boolean::parseBoolean)
                    .getAny(Boolean.class);
            return buildProgressBar(undetermined, value, max, size, left, center, right);
        });
    }

    private static String buildProgressBar(boolean undetermined, long value, long max, int size, String left, String center, String right) {
        size -= 1;
        if (undetermined) {
            return left.repeat(size) + center;
        }
        int progress = (int) (value / (double) max * size);
        return left.repeat(progress) + center + right.repeat(size - progress);
    }
}
