package fr.enimaloc.matcher.syntaxe;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.ICommandReference;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class JDAKeyword {

    public static final String JDA_KEY = "internal.jda";

    public static Keyword[] getKeywords() {
        return new Keyword[] {
                getCommandMention()
        };
    }

    public static Keyword getCommandMention() {
        return new Keyword("jda.commandMention", (matcher, instruction) -> {
            JDA jda = (JDA) matcher.getenv().get(JDA_KEY);
            String[] split = instruction.getArgs(0).mapLeft(instru -> instru.run(matcher)).getAny(String.class).split("\\.");
            try {
                ICommandReference cmdRef = jda.retrieveCommands()
                        .submit()
                        .get()
                        .stream()
                        .filter(cmd -> cmd.getName().equals(split[0]))
                        .findFirst()
                        .orElseThrow();
                for (var ref = new Object() {
                    int i = 1;
                }; ref.i < split.length; ref.i++) {
                    if (cmdRef instanceof Command cmd) {
                        if (split.length > 2) {
                            cmdRef = cmd.getSubcommandGroups()
                                    .stream()
                                    .filter(sub -> sub.getName().equals(split[ref.i]))
                                    .findFirst()
                                    .orElseThrow();
                        } else {
                            cmdRef = cmd.getSubcommands()
                                    .stream()
                                    .filter(sub -> sub.getName().equals(split[ref.i]))
                                    .findFirst()
                                    .orElseThrow();
                        }
                    } else if (cmdRef instanceof Command.SubcommandGroup group) {
                        cmdRef = group.getSubcommands()
                                .stream()
                                .filter(sub -> sub.getName().equals(split[ref.i]))
                                .findFirst()
                                .orElseThrow();
                    }
                }
                return cmdRef.getAsMention();
            } catch (InterruptedException | ExecutionException e) {
                return instruction.getArgsCount() > 1 ? instruction.getArgs(1).mapLeft(instru -> instru.run(matcher)).getAny(String.class) : "";
            }
        });
    }
}
