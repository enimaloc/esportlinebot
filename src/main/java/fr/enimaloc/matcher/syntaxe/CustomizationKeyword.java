package fr.enimaloc.matcher.syntaxe;

import com.electronwill.nightconfig.core.CommentedConfig;

public class CustomizationKeyword {

    public static final String TOML_PATH_KEY = "customization.toml";

    CustomizationKeyword() {
    }

    public static Keyword[] getKeywords() {
        return new Keyword[]{
                get()
        };
    }

    public static Keyword get() {
        return new Keyword("customization.get", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1) {
                throw new IllegalArgumentException("get() takes only one argument");
            }
            String key = instruction.getArgs(0).mapLeft(i -> i.run(matcher)).getAny(String.class);
            return ((CommentedConfig) matcher.getenv().get(TOML_PATH_KEY)).get(key);
        });
    }
}
