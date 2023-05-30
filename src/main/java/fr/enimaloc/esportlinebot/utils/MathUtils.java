package fr.enimaloc.esportlinebot.utils;

import java.math.BigDecimal;
import java.math.MathContext;

public class MathUtils {
    public static BigDecimal eval(final String str, MathContext mathContext) {
        if (str == null || str.isBlank()) {
            return null;
        }

        return new Object() {
            int index = -1;
            char c;

            void nextChar() {
                c = (++index < str.length()) ? str.charAt(index) : '\0';
            }

            boolean eat(int charToEat) {
                while (c == ' ') nextChar();
                if (c == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            BigDecimal parse() {
                nextChar();
                BigDecimal x = parseExpression();
                if (index < str.length()) throw new RuntimeException("Unexpected: " + c);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)` | number
            //        | functionName `(` expression `)` | functionName factor
            //        | factor `^` factor

            BigDecimal parseExpression() {
                BigDecimal x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x = x.add(parseTerm()); // addition
                    else if (eat('-')) x = x.subtract(parseTerm()); // subtraction
                    else return x;
                }
            }

            BigDecimal parseTerm() {
                BigDecimal x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x = x.multiply(parseFactor()); // multiplication
                    else if (eat('/')) x = x.divide(parseFactor(), mathContext); // division
                    else return x;
                }
            }

            BigDecimal parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return parseFactor().negate(); // unary minus

                BigDecimal x;
                int startPos = this.index;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    if (!eat(')')) throw new RuntimeException("Missing ')'");
                } else if ((c >= '0' && c <= '9') || c == '.') { // numbers
                    while ((c >= '0' && c <= '9') || c == '.') nextChar();
                    x = new BigDecimal(str.substring(startPos, this.index));
                } else if (c >= 'a' && c <= 'z') { // functions
                    while (c >= 'a' && c <= 'z') nextChar();
                    String func = str.substring(startPos, this.index);
                    if (eat('(')) {
                        x = parseExpression();
                        if (!eat(')')) throw new RuntimeException("Missing ')' after argument to " + func);
                    } else {
                        x = parseFactor();
                    }
                    if ("sqrt".equals(func)) {
                        x = x.sqrt(mathContext);
                    } else {
                        throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected: " + c);
                }

                if (eat('^')) x = x.pow(parseFactor().intValue(), mathContext); // exponentiation

                return x;
            }
        }.parse();
    }
}
