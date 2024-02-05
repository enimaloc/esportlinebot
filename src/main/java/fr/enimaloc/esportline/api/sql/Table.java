package fr.enimaloc.esportline.api.sql;

import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Table {
    private final Connection sql;
    private final String name;

    public Table(Connection sql, String name) {
        this.sql = sql;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ResultSet select(@Nullable Column<?>[] columns, @Nullable WhereStatement where, int limit, int offset) throws SQLException {
        if (limit < -1 || offset < -1) throw new IllegalArgumentException("Limit and offset must be positive or -1 for no limit or offset.");
        String col = columns == null ? "*" : Arrays.stream(columns).map(Column::getName).collect(Collectors.joining(", "));
        String wh = where == null ? "" : " WHERE " + where.build();
        String lim = limit == -1 ? "" : " LIMIT " + limit;
        String off = offset == -1 ? "" : " OFFSET " + offset;
        return sql.createStatement().executeQuery("SELECT " + col + " FROM " + name + wh + lim + off);
    }

    public ResultSet select(@Nullable Column<?>[] columns, @Nullable WhereStatement where) throws SQLException {
        return select(columns, where, -1, -1);
    }

    public ResultSet select(@Nullable Column<?>[] columns, int limit, int offset) throws SQLException {
        return select(columns, null, limit, offset);
    }

    public ResultSet select(@Nullable Column<?>[] columns) throws SQLException {
        return select(columns, null, -1, -1);
    }

    public ResultSet select(int limit, int offset) throws SQLException {
        return select(null, null, limit, offset);
    }

    public ResultSet select(WhereStatement where) throws SQLException {
        return select(null, where, -1, -1);
    }

    public ResultSet select() throws SQLException {
        return select(null, null, -1, -1);
    }

    public void close() throws SQLException {
        sql.close();
    }
}
