package fr.enimaloc.matcher.syntaxe;

public class MatcherKeyword {

    public static Keyword[] getKeywords() {
        return new Keyword[]{
                apply()
        };
    }

    public static Keyword apply() {
        return new Keyword("matcher.apply", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1) {
                throw new IllegalArgumentException("apply() takes only one argument");
            }
            String pattern = instruction.getArgs(0).mapLeft(i -> i.run(matcher)).getAny(String.class);
            return matcher.apply(pattern);
        });
    }
}
