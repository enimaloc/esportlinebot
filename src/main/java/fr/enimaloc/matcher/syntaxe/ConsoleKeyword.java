package fr.enimaloc.matcher.syntaxe;

import fr.enimaloc.matcher.utils.Either;

public class ConsoleKeyword {

    public static Keyword[] getKeywords() {
        return new Keyword[]{
                print(),
                println(),
                printf()
        };
    }

    private static Keyword print() {
        return new Keyword("console.print", (matcher, instruction) -> {
            for (Either<Instruction, String> arg : instruction.getArgs()) {
                System.out.print(arg.mapLeft(ins -> ins.run(matcher)).getAny(String.class));
            }
            return "";
        });
    }

    private static Keyword println() {
        return new Keyword("console.println", (matcher, instruction) -> {
            for (Either<Instruction, String> arg : instruction.getArgs()) {
                System.out.println(arg.mapLeft(ins -> ins.run(matcher)).getAny(String.class));
            }
            return "";
        });
    }

    private static Keyword printf() {
        return new Keyword("console.printf", (matcher, instruction) -> {
            Either<Instruction, String>[] args = instruction.getArgs();
            if (args.length == 0) {
                throw new RuntimeException("printf need at least one argument");
            }
            String format = args[0].mapLeft(ins -> ins.run(matcher)).getAny(String.class);
            int i = 1;
            for (Either<Instruction, String> arg : args) {
                if (i == 1) {
                    i++;
                    continue;
                }
                format = format.replaceFirst("\\{}", arg.mapLeft(ins -> ins.run(matcher)).getAny(String.class));
            }
            System.out.print(format);
            return "";
        });
    }
}
