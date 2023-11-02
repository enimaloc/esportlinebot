package fr.enimaloc.esportline.api.wakfu.json.item;

import fr.enimaloc.esportline.api.wakfu.json.Action;
import fr.enimaloc.esportline.api.wakfu.json.WakfuJSON;
import fr.enimaloc.esportline.api.wakfu.WakfuLocale;
import fr.enimaloc.esportline.api.wakfu.json.marker.Identifier;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record EffectHolder(Effect effect, EffectHolder[] subEffects) {
    public record Effect(Definition definition, Map<WakfuLocale, String> title, Map<WakfuLocale, String> description)
            implements Identifier {
        public static final Map<WakfuLocale, String> UNKNOWN_EFFECTS = Map.of(
                WakfuLocale.FRENCH, "Effets inconnus",
                WakfuLocale.ENGLISH, "Unknown effects",
                WakfuLocale.SPANISH, "Efectos desconocidos",
                WakfuLocale.PORTUGUESE, "Efeitos desconhecidos"
        );
        public static final Map<WakfuLocale, String>[] ELEMENTS = new Map[]{
                Map.of(
                        WakfuLocale.FRENCH, "Feu",
                        WakfuLocale.ENGLISH, "Fire",
                        WakfuLocale.SPANISH, "Fuego",
                        WakfuLocale.PORTUGUESE, "Fogo"
                ),
                Map.of(
                        WakfuLocale.FRENCH, "Eau",
                        WakfuLocale.ENGLISH, "Water",
                        WakfuLocale.SPANISH, "Agua",
                        WakfuLocale.PORTUGUESE, "Água"
                ),
                Map.of(
                        WakfuLocale.FRENCH, "Terre",
                        WakfuLocale.ENGLISH, "Earth",
                        WakfuLocale.SPANISH, "Tierra",
                        WakfuLocale.PORTUGUESE, "Terra"
                ),
                Map.of(
                        WakfuLocale.FRENCH, "Air",
                        WakfuLocale.ENGLISH, "Air",
                        WakfuLocale.SPANISH, "Aire",
                        WakfuLocale.PORTUGUESE, "Ar"
                ),
                null,
                Map.of(
                        WakfuLocale.FRENCH, "Lumière",
                        WakfuLocale.ENGLISH, "Light",
                        WakfuLocale.SPANISH, "Luz",
                        WakfuLocale.PORTUGUESE, "Luz"
                ),
        };
        public static final Map<WakfuLocale, String>[] CHARACTERISTIC = new Map[]{
                Map.of(
                        WakfuLocale.FRENCH, "Armmure reçus",
                        WakfuLocale.ENGLISH, "Received armor",
                        WakfuLocale.SPANISH, "Armadura recibida",
                        WakfuLocale.PORTUGUESE, "Armadura recebida"
                ),
                Map.of(
                        WakfuLocale.FRENCH, "Armmure donnés",
                        WakfuLocale.ENGLISH, "Armor given",
                        WakfuLocale.SPANISH, "Armadura dada",
                        WakfuLocale.PORTUGUESE, "Armadura concedida"
                ),
        };

        @Override
        public int id() {
            return definition().id();
        }

        public Map<WakfuLocale, String> parsedDescription(WakfuJSON client, int level) {
            return parsedDescription(client, level, ELEMENTS);
        }

        public Map<WakfuLocale, String> parsedDescription(WakfuJSON client, int level, Map<WakfuLocale, String>[] elements) {
            return Arrays.stream(WakfuLocale.values()).map(locale -> Map.entry(locale, parsedLanguageDescription(locale, client, level, elements)))
                    .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);
        }

        private static Pattern BLOCK_PATTERN = Pattern.compile("(\\d*)([#~+\\-><=])?(\\d+)");


        private String parsedLanguageDescription(WakfuLocale locale, WakfuJSON client, int level, Map<WakfuLocale, String>[] elements) {
            int actionId = definition().actionId();
            double[] params = definition().params();
            Action action = definition().action(client);
            boolean isCharacteristicAsParamId = actionId == 39 || actionId == 40;
            String description = action.description().get(locale);
            if (isCharacteristicAsParamId && description() != null && description().containsKey(locale)) {
                description = description().get(locale);
            }
//            if (description == null) {
//                return null;
//            }
//
//            Object[] newParams = new Object[params.length];
//            for (int index = 0; index < params.length; index++) {
//                double param = params[index];
//                boolean isOddIndex = index % 2 == 1;
//                if (isOddIndex) {
//                    newParams[index] = param;
//                    continue;
//                }
//                int paramNumber = (int) params[index];
//                String paramKey = "[#" + paramNumber + "]";
//                String paramValue = String.valueOf(Math.floor(param + params[index + 1] * level));
//
//                if (actionId == 304 && index == 0) {
//                    double statedId = params[0];
//                    States states = client.getState((int) statedId).orElseThrow();
//                    paramValue = states.title() != null && states.title().containsKey(locale) ? states.title().get(locale) : UNKNOWN_EFFECTS.get(locale);
//                }
//
//                if (actionId == 832 && index == 0) {
//                    double elementId = params[0];
//                    paramValue = ELEMENTS[(int) elementId].get(locale);
//                }
//
//                if (actionId == 2001 && index == 2) {
//                    double jobId = params[2];
//                    RecipeCategory job = client.getRecipeCategory((int) jobId).orElseThrow();
//                    paramValue = job.title().get(locale);
//                }
//
//                if ((actionId == 39 || actionId == 40) && index == 4) {
//                    double characteristicId = params[4];
//                    paramValue = CHARACTERISTIC[(int) characteristicId - 120].get(locale);
//                }
//
//                newParams[index] = paramValue;
//            }
            double[] newParams = new double[params.length / 2];
            for (int index = 0; index < params.length; index += 2) {
                double param = params[index];
                double paramLevel = params[index + 1];
                newParams[index / 2] = Math.floor(param + paramLevel * level);
            }
            StringBuilder builder = new StringBuilder();
            char[] chars = description.toCharArray();
            Queue<Integer> inBlockStart = new ArrayDeque<>();
            Queue<Integer> inBlockEnd = new ArrayDeque<>();
            Object stack = null;
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '[') {
                    inBlockStart.offer(i);
                    continue;
                }
                if (c == ']') {
                    inBlockEnd.offer(i);
                    if (!inBlockStart.isEmpty() && !inBlockEnd.isEmpty()) {
                        int end = inBlockEnd.poll();
                        String block = description.substring(inBlockStart.poll() + 1, end);
                        if (block.equals("ecnbi") || block.equals("ecnbr")) {
                            builder.append("[").append(block).append("]").append(chars[end + 1]);
                            continue;
                        }
                        Matcher matcher = BLOCK_PATTERN.matcher(block);
                        OptionalDouble operandLeft = OptionalDouble.empty();
                        char operator;
                        int operandRight;
                        boolean isElReference = block.startsWith("el");
                        if (isElReference) {
                            int elIndex = Integer.parseInt(block.substring(2));
                            builder.append(elements[elIndex - 1].get(locale));
                            continue;
                        }

                        if (matcher.matches()) {
                            operandLeft = matcher.group(1).isEmpty() ? OptionalDouble.empty() : OptionalDouble.of(Double.parseDouble(matcher.group(1)));
                            operator = matcher.group(2).charAt(0);
                            operandRight = Integer.parseInt(matcher.group(3));
                        } else {
                            operator = block.charAt(0);
                            operandRight = Integer.parseInt(block.substring(1));
                        }
                        switch (operator) {
                            case '#':
                                int index = operandRight / 2;
                                stack = newParams[index];
                                if (inBlockStart.isEmpty() && inBlockEnd.isEmpty()) {
                                    builder.append(stack);
                                }
                                break;
                            case '~':
                                builder.append(params.length >= operandRight);
                                break;
                            case '+':
                                builder.append(params.length > operandRight);
                                break;
                            case '-':
                                builder.append(params.length < operandRight);
                                break;
                            case '=':
                                builder.append(operandLeft.orElse((Double) stack) == operandRight);
                                break;
                            case '>':
                                builder.append(operandLeft.orElse((Double) stack) > operandRight);
                                break;
                            case '<':
                                builder.append(operandLeft.orElse((Double) stack) < operandRight);
                                break;
                        }
                        i = end;
                        continue;
                    }
                    continue;
                }
                if (inBlockStart.isEmpty()) {
                    builder.append(c);
                }
            }
            return executeTernary(builder.toString()).replaceAll("(\\d+)\\.0+([^\\d])", "$1$2");
        }

        private String executeTernary(String ternary) {
            ternary = ternary.replaceAll("\u0001", "{")
                    .replaceAll("\u0002", "?")
                    .replaceAll("\u0003", ":")
                    .replaceAll("\u0004", "}");
            char[] chars = ternary.toCharArray();
            int start = -1;
            int count = 0;
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '{') {
                    if (start == -1) {
                        start = i;
                    }
                    count++;
                    continue;
                }
                if (c == '}') {
                    count--;
                    if (count == 0 && start != -1) {
                        int last = 0;
                        String fragment = ternary.substring(start + 1, i);
                        String condition = fragment.substring(0, last = fragment.indexOf('?'));
                        String ifTrue = executeTernary(fragment.substring(last + 1, last = fragment.indexOf(':', last)));
                        String ifFalse = executeTernary(fragment.substring(last + 1));
                        builder.append(Boolean.parseBoolean(condition) ? ifTrue : ifFalse);
                        start = -1;
                        continue;
                    }
                }
                if (count > 1) {
                    chars[i] = switch (c) {
                        case '{' -> '\u0001';
                        case '?' -> '\u0002';
                        case ':' -> '\u0003';
                        case '}' -> '\u0004';
                        default -> c;
                    };
                }
                if (count == 0) {
                    builder.append(c);
                }
            }
            return builder.toString();
        }

        public record Definition(int id, int actionId, int areaShape, int[] areaSize, double[] params) {
            public Action action(WakfuJSON client) {
                return client.getAction(actionId()).orElseThrow();
            }
        }
    }
}
