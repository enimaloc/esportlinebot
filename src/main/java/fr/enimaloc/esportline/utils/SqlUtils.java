package fr.enimaloc.esportline.utils;

import fr.enimaloc.esportline.api.sql.Column;

import java.sql.ResultSet;

public class SqlUtils {
    SqlUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean hasColumn(ResultSet set, String columnName) {
        try {
            set.findColumn(columnName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasColumn(ResultSet set, Column<?> column) {
        try {
            set.findColumn(column.getName());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
