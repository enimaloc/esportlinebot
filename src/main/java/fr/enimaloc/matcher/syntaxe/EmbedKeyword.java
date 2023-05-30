package fr.enimaloc.matcher.syntaxe;

import fr.enimaloc.matcher.utils.Either;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.OffsetDateTime;

import static net.dv8tion.jda.api.EmbedBuilder.URL_PATTERN;
import static net.dv8tion.jda.api.EmbedBuilder.ZERO_WIDTH_SPACE;
import static net.dv8tion.jda.api.entities.MessageEmbed.*;

public class EmbedKeyword {

    public static final String EMBED_KEY = "internal.embed";
    public static final String EMBED_BUILDER_KEY = "internal.embedBuilder";

    EmbedKeyword() {
    }

    public static Keyword[] getKeywords() {
        return new Keyword[]{
                create(),
                title(),
                description(),
                color(),
                footer(),
                thumbnail(),
                image(),
                author(),
                field(),
                timestamp(),
                clear(),
                build()
        };
    }

    public static Keyword create() {
        return new Keyword("embed.create", (matcher, instruction) -> {
            EmbedBuilder builder = new EmbedBuilder();
            if (instruction.getArgsCount() > 0) {
                for (Either<Instruction, String> arg : instruction.getArgs()) {
                    String s = arg.mapLeft(instruct -> instruct.run(matcher))
                            .map(String.class, String::toLowerCase)
                            .getAny(String.class);
                    String key = s.split("=")[0];
                    String val = s.split("=")[1];
                    switch (key) {
                        case "title":
                            builder.setTitle(val);
                            break;
                        case "description":
                            builder.setDescription(val);
                            break;
                        case "color":
                            builder.setColor(Integer.parseInt(val));
                            break;
                        case "footer":
                            builder.setFooter(val);
                            break;
                        case "thumbnail":
                            builder.setThumbnail(val);
                            break;
                        case "image":
                            builder.setImage(val);
                            break;
                        case "author":
                            builder.setAuthor(val);
                            break;
                        case "field":
                            String[] field = val.split(";");
                            builder.addField(field[0], field[1], Boolean.parseBoolean(field[2]));
                            break;
                        case "timestamp":
                            builder.setTimestamp(OffsetDateTime.parse(val));
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown argument: " + key);
                    }
                }
            }
            matcher.getenv().put(EMBED_BUILDER_KEY, builder);
            return "";
        });
    }

    public static Keyword title() {
        return new Keyword("embed.title", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1 && instruction.getArgsCount() != 2) {
                throw new IllegalArgumentException("title() takes one or two arguments");
            }
            String title = instruction.getArgs(0).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class);
            if (title.length() >= TITLE_MAX_LENGTH) {
                throw new IllegalArgumentException("title() argument is too long");
            }
            String url = instruction.getArgsCount() == 2 ? instruction.getArgs(1).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class) : null;
            if (url != null && url.length() >= URL_MAX_LENGTH) {
                throw new IllegalArgumentException("title() argument is too long");
            }
            if (url != null && !URL_PATTERN.matcher(url).matches()) {
                throw new IllegalArgumentException("title() argument is not a valid URL");
            }
            ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).setTitle(title, url);
            return "";
        });
    }

    public static Keyword description() {
        return new Keyword("embed.description", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1) {
                throw new IllegalArgumentException("description() takes one argument");
            }
            String description = instruction.getArgs(0).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class);
            if (description.length() >= TEXT_MAX_LENGTH) {
                throw new IllegalArgumentException("description() argument is too long");
            }
            ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).setDescription(description);
            return "";
        });
    }

    public static Keyword color() {
        return new Keyword("embed.color", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1) {
                throw new IllegalArgumentException("color() takes one argument");
            }
            String color = instruction.getArgs(0).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class);
            if (color.length() != 6) {
                throw new IllegalArgumentException("color() argument is not a valid color");
            }
            ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).setColor(Integer.parseInt(color, 16));
            return "";
        });
    }

    public static Keyword footer() {
        return new Keyword("embed.footer", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1 && instruction.getArgsCount() != 2) {
                throw new IllegalArgumentException("footer() takes one or two arguments");
            }
            String text = instruction.getArgs(0).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class);
            if (text.length() >= TEXT_MAX_LENGTH) {
                throw new IllegalArgumentException("footer() argument is too long");
            }
            String iconUrl = instruction.getArgsCount() == 2 ? instruction.getArgs(1).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class) : null;
            if (iconUrl != null && iconUrl.length() >= URL_MAX_LENGTH) {
                throw new IllegalArgumentException("footer() argument is too long");
            }
            if (iconUrl != null && !URL_PATTERN.matcher(iconUrl).matches()) {
                throw new IllegalArgumentException("footer() argument is not a valid URL");
            }
            ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).setFooter(text, iconUrl);
            return "";
        });
    }

    public static Keyword image() {
        return new Keyword("embed.image", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1) {
                throw new IllegalArgumentException("image() takes one argument");
            }
            String url = instruction.getArgs(0).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class);
            if (url.length() >= URL_MAX_LENGTH) {
                throw new IllegalArgumentException("image() argument is too long");
            }
            if (!URL_PATTERN.matcher(url).matches()) {
                throw new IllegalArgumentException("image() argument is not a valid URL");
            }
            ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).setImage(url);
            return "";
        });
    }

    public static Keyword thumbnail() {
        return new Keyword("embed.thumbnail", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1) {
                throw new IllegalArgumentException("thumbnail() takes one argument");
            }
            String url = instruction.getArgs(0).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class);
            if (url.length() >= URL_MAX_LENGTH) {
                throw new IllegalArgumentException("thumbnail() argument is too long");
            }
            if (!URL_PATTERN.matcher(url).matches()) {
                throw new IllegalArgumentException("thumbnail() argument is not a valid URL");
            }
            ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).setThumbnail(url);
            return "";
        });
    }

    public static Keyword author() {
        return new Keyword("embed.author", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 1 && instruction.getArgsCount() != 2 && instruction.getArgsCount() != 3) {
                throw new IllegalArgumentException("author() takes one, two or three arguments");
            }
            String name = instruction.getArgs(0).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class);
            if (name.length() >= AUTHOR_MAX_LENGTH) {
                throw new IllegalArgumentException("author() argument is too long");
            }
            String url = instruction.getArgsCount() >= 2 ? instruction.getArgs(1).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class) : null;
            if (url != null && url.length() >= URL_MAX_LENGTH) {
                throw new IllegalArgumentException("author() argument is too long");
            }
            if (url != null && !URL_PATTERN.matcher(url).matches()) {
                throw new IllegalArgumentException("author() argument is not a valid URL");
            }
            String iconUrl = instruction.getArgsCount() == 3 ? instruction.getArgs(2).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class) : null;
            if (iconUrl != null && iconUrl.length() >= URL_MAX_LENGTH) {
                throw new IllegalArgumentException("author() argument is too long");
            }
            if (iconUrl != null && !URL_PATTERN.matcher(iconUrl).matches()) {
                throw new IllegalArgumentException("author() argument is not a valid URL");
            }
            ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).setAuthor(name, url, iconUrl);
            return "";
        });
    }

    public static Keyword field() {
        return new Keyword("embed.field", (matcher, instruction) -> {
            if (instruction.getArgsCount() < -1 && instruction.getArgsCount() > 3) {
                throw new IllegalArgumentException("field() takes one to three arguments");
            }
            String name = instruction.getArgsCount() == 1
                    ? ZERO_WIDTH_SPACE
                    : instruction
                    .getArgs(0)
                    .mapLeft(instruct -> instruct.run(matcher))
                    .getAny(String.class);
            if (name.length() >= TITLE_MAX_LENGTH) {
                throw new IllegalArgumentException("field() argument is too long");
            }
            String value = instruction.getArgs(1).mapLeft(instruct -> instruct.run(matcher)).getAny(String.class);
            if (value.length() >= VALUE_MAX_LENGTH) {
                throw new IllegalArgumentException("field() argument is too long");
            }
            boolean inline = instruction.getArgsCount() == 3
                    && instruction.getArgs(2).mapLeft(instruct -> instruct.run(matcher)).getAny(Boolean.class);
            ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).addField(name, value, inline);
            return "";
        });
    }

    public static Keyword timestamp() {
        return new Keyword("embed.timestamp", (matcher, instruction) -> {
            if (instruction.getArgsCount() > 1) {
                throw new IllegalArgumentException("timestamp() takes at most one argument");
            }
            OffsetDateTime timestamp = instruction.getArgsCount() == 1
                    ? instruction.getArgs(0)
                    .mapLeft(instruct -> instruct.run(matcher))
                    .map(String.class, OffsetDateTime::parse)
                    .getAny(OffsetDateTime.class)
                    : OffsetDateTime.now();
            ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).setTimestamp(timestamp);
            return "";
        });
    }

    public static Keyword clear() {
        return new Keyword("embed.clear", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 0) {
                throw new IllegalArgumentException("clear() takes no arguments");
            }
            ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).clear();
            return "";
        });
    }

    public static Keyword build() {
        return new Keyword("embed.build", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 0) {
                throw new IllegalArgumentException("build() takes no arguments");
            }
            matcher.getenv().put(EMBED_KEY, matcher.getenv().containsKey(EMBED_BUILDER_KEY) ? ((EmbedBuilder) matcher.getenv().get(EMBED_BUILDER_KEY)).build() : new EmbedBuilder().build());
            return "";
        });
    }
}
