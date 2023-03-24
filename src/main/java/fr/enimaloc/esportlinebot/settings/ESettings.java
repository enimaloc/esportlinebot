package fr.enimaloc.esportlinebot.settings;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ESettings {

    @Nullable
    public final String      prefixPath;
    @Nullable
    public final ESettings   parent;
    public final FileConfig  config;
    private      List<Field> settingsEntry;
    private      List<Field> subSettings;

    ESettings(FileConfig config) {
        this(null, config);
    }

    ESettings(@Nullable String prefixPath, FileConfig config) {
        this(prefixPath, null, config);
    }

    ESettings(@Nullable String prefixPath, ESettings parent) {
        this(prefixPath, parent, parent.config);
    }

    ESettings(@Nullable String prefixPath, @Nullable ESettings parent, FileConfig config) {
        this.prefixPath = prefixPath != null ? (parent != null ? parent.getPrefixPathWithDot() : "")+prefixPath : null;
        this.parent = parent;
        this.config = config;
        this.settingsEntry = new ArrayList<>();
        this.subSettings = new ArrayList<>();
        List<Field> tmp = null;
        for (Field field : this.getClass().getFields()) {
            if (field.isAnnotationPresent(SettingsEntry.class) && ESettings.class.isAssignableFrom(field.getType())) {
                tmp = subSettings;
            } else if (field.isAnnotationPresent(SettingsEntry.class)) {
                tmp = settingsEntry;
            }
            if (tmp != null && field.trySetAccessible()) {
                tmp.add(field);
            } else if (tmp != null) {
                Settings.LOGGER.error("Error while setting {} accessible in {}, ignoring it", field.getName(), this.getClass().getSimpleName());
            }
            tmp = null;
        }
    }

    ConfigSpec spec(ConfigSpec base) {
        for (Field field : settingsEntry) {
            try {
                Object        defaultValue = field.get(this);
                base.define(getKey(field), defaultValue, o -> {
                    Class<?> fieldType         = field.getType();
                    if (fieldType == boolean.class) {
                        fieldType = Boolean.class;
                    } else if (fieldType == int.class || fieldType == long.class) {
                        long l = Long.parseLong(o + "");
                        if ((int) l == l) {
                            fieldType = Integer.class;
                        } else {
                            fieldType = Long.class;
                        }
                    } else if (fieldType == float.class) {
                        fieldType = Float.class;
                    } else if (fieldType == double.class) {
                        fieldType = Double.class;
                    } else if (fieldType == char.class) {
                        fieldType = Character.class;
                    } else if (fieldType == byte.class) {
                        fieldType = Byte.class;
                    } else if (fieldType == short.class) {
                        fieldType = Short.class;
                    }
                    return o != null && fieldType.isAssignableFrom(o.getClass());
                });
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while defining %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
        for (Field field : subSettings) {
            try {
                ((ESettings) field.get(this)).spec(base);
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while defining %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
        return base;
    }

    void load() {
        load(config);
    }

    void load(FileConfig config) {
        for (Field field : settingsEntry) {
            try {
                Object value = config.get(getKey(field));
                if (value != null) {
                    field.set(this, value);
                }
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while loading %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
        for (Field field : subSettings) {
            try {
                ((ESettings) field.get(this)).load(config);
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while loading %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
    }

    public void save() {
        save(config);
    }

    public void save(FileConfig config) {
        (parent == null ? this : parent).save0(config);
    }

    void save0(FileConfig config) {
        for (Field field : settingsEntry) {
            try {
                config.set(getKey(field), field.get(this));
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while saving %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
        for (Field field : subSettings) {
            try {
                ((ESettings) field.get(this)).save0(config);
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while saving %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
    }

    Map<String, Object> values() {
        HashMap<String, Object> values = new HashMap<>();
        for (Field field : settingsEntry) {
            try {
                values.put(getKey(field), field.get(this));
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while getting %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
        for (Field field : subSettings) {
            try {
                values.putAll(((ESettings) field.get(this)).values());
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while getting %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
        return values;
    }

    List<String> keys() {
        return values().keySet().stream().map(this::getKey).collect(Collectors.toList());
    }

    String getKey(Field key) {
        return getKey(key.getName());
    }

    String getKey(String key) {
        return getPrefixPathWithDot() + key;
    }

    String getPrefixPathWithDot() {
        return prefixPath == null ? "" : prefixPath + ".";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface SettingsEntry {
    }
}
