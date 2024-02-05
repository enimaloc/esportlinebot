package fr.enimaloc.esportline.api.sql;

import org.intellij.lang.annotations.MagicConstant;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class WhereStatement<T> {
    public static final int WILDCARD_START_ZERO_OR_MORE = 0x1;
    public static final int WILDCARD_START_ONE = 0x2;
    public static final int WILDCARD_END_ZERO_OR_MORE = 0x01;
    public static final int WILDCARD_END_ONE = 0x02;
    private boolean notStatement = false;
    private String column;
    private String operator;
    private T value;
    private String valueStr;
    private WhereStatement[] and;
    private WhereStatement[] or;

    WhereStatement(String column, Class<T> type) {
        this.column = column;
    }

    public static <T> WhereStatement of(String column, Class<T> type) {
        return new WhereStatement(column, type);
    }

    public static <T> WhereStatement of(Column<T> column) {
        return new WhereStatement(column.getName(), column.getType());
    }

    public static <T> WhereStatement not(String column, Class<T> type) {
        return new WhereStatement(column, type).not();
    }

    public static <T> WhereStatement not(Column<T> column) {
        return new WhereStatement(column.getName(), column.getType()).not();
    }

    public WhereStatement eq(T value) {
        this.operator = "=";
        this.value = value;
        return this;
    }

    public WhereStatement ne(T value) {
        this.operator = "!=";
        this.value = value;
        return this;
    }

    public WhereStatement gt(T value) {
        this.operator = ">";
        this.value = value;
        return this;
    }

    public WhereStatement lt(T value) {
        this.operator = "<";
        this.value = value;
        return this;
    }

    public WhereStatement gte(T value) {
        this.operator = ">=";
        this.value = value;
        return this;
    }

    public WhereStatement lte(T value) {
        this.operator = "<=";
        this.value = value;
        return this;
    }

    public WhereStatement like(T value) {
        return like(value, 0);
    }

    public WhereStatement like(T value, @MagicConstant(intValues = {
            WILDCARD_START_ZERO_OR_MORE,
            WILDCARD_START_ONE,
            WILDCARD_END_ZERO_OR_MORE,
            WILDCARD_END_ONE
    }) int... wildcard) {
        int wild = Arrays.stream(wildcard).reduce(0, (a, b) -> a | b);
        return like(value, wild);
    }

    public WhereStatement like(T value, @MagicConstant(flags = {
            WILDCARD_START_ZERO_OR_MORE,
            WILDCARD_START_ONE,
            WILDCARD_END_ZERO_OR_MORE,
            WILDCARD_END_ONE
    }) int wildcard) {
        this.operator = "LIKE\0"+wildcard;
        this.value = value;
        return this;
    }

    public WhereStatement between(T min, T max) {
        this.operator = "BETWEEN";
        this.valueStr = min + " AND " + max;
        return this;
    }

    public WhereStatement in(T... values) {
        this.operator = "IN";
        this.valueStr = Arrays.stream(values).map(Objects::toString).collect(Collectors.joining(", ", "(", ")"));
        return this;
    }

    public WhereStatement isNull() {
        this.operator = "IS";
        this.valueStr = "NULL";
        return this;
    }

    public WhereStatement isNotNull() {
        this.operator = "IS NOT";
        this.valueStr = "NULL";
        return this;
    }

    public WhereStatement and(WhereStatement and) {
        if (this.and == null) {
            this.and = new WhereStatement[]{and};
        } else {
            WhereStatement[] newAnd = new WhereStatement[this.and.length + 1];
            System.arraycopy(this.and, 0, newAnd, 0, this.and.length);
            newAnd[this.and.length] = and;
            this.and = newAnd;
        }
        return this;
    }

    public WhereStatement and(WhereStatement... and) {
        this.and = and;
        return this;
    }

    public WhereStatement or(WhereStatement or) {
        if (this.or == null) {
            this.or = new WhereStatement[]{or};
        } else {
            WhereStatement[] newOr = new WhereStatement[this.or.length + 1];
            System.arraycopy(this.or, 0, newOr, 0, this.or.length);
            newOr[this.or.length] = or;
            this.or = newOr;
        }
        return this;
    }

    public WhereStatement or(WhereStatement... or) {
        this.or = or;
        return this;
    }

    public WhereStatement not() {
        notStatement = true;
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        if (or != null && or.length > 0) {
            sb.append("(");
        }
        String val = null;
        if (value == null && valueStr != null) {
            val = valueStr;
        } else {
            val = value.toString();
        }
        if (operator.contains("\0")) {
            int wildcard = Integer.parseInt(operator.split("\0")[1]);
            operator = operator.split("\0")[0];
            if ((wildcard & WILDCARD_START_ZERO_OR_MORE) == WILDCARD_START_ZERO_OR_MORE) {
                val = "%" + val;
            } else if ((wildcard & WILDCARD_START_ONE) == WILDCARD_START_ONE) {
                val = "_" + val;
            }
            if ((wildcard & WILDCARD_END_ZERO_OR_MORE) == WILDCARD_END_ZERO_OR_MORE) {
                val += "%";
            } else if ((wildcard & WILDCARD_END_ONE) == WILDCARD_END_ONE) {
                val += "_";
            }
        }
        val = value instanceof CharSequence ? "'" + val + "'" : val;
        if (operator.equals("LIKE") || operator.equals("BETWEEN") || operator.equals("IN")) {
            sb.append(column).append(" ")
                    .append(notStatement ? "NOT " : "")
                    .append(operator).append(" ").append(val);
        } else {
            sb.append(notStatement ? "NOT " : "")
                    .append(column).append(" ").append(operator).append(" ").append(val);
        }
        if (or != null && or.length > 0) {
            for (WhereStatement statement : or) {
                sb.append(" OR ").append(statement.build());
            }
            sb.append(")");
        }
        if (and != null && and.length > 0) {
            for (WhereStatement statement : and) {
                sb.append(" AND ").append(statement.build());
            }
        }
        return sb.toString();
    }
}
