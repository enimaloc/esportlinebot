package fr.enimaloc.esportline.utils;

import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;

public class BundleUtils {
    public static String getOr(ResourceBundle bundle, String key, String or) {
        return bundle.containsKey(key) ? bundle.getString(key) : or;
    }
    public static String getOrKey(ResourceBundle bundle, String key, String orKey) {
        return getOr(bundle, key, bundle.getString(orKey));
    }

    public static String get(ResourceBundle bundle,  String key) {
        return getOr(bundle, key, key);
    }
}
