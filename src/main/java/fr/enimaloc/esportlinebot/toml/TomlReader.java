package fr.enimaloc.esportlinebot.toml;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TableWriter;
import com.electronwill.nightconfig.toml.ValueWriter;
import fr.enimaloc.esportlinebot.toml.settings.Settings;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class TomlReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomlReader.class);

    static {
        ValueWriter.register(EnumMap.class, (value, output, writer) -> {
            Config memory = Config.inMemory();
            value.forEach((k, v) -> memory.add(k.toString(), v));
            TableWriter.writeInline(memory, output, writer);
        });
        ValueWriter.register(Map.class, (value, output, writer) -> {
            Config memory = Config.inMemory();
            value.forEach((k, v) -> memory.add(k.toString(), v));
            TableWriter.writeInline(memory, output, writer);
        });
    }

    @Nullable
    public final String prefixPath;
    @Nullable
    public final TomlReader parent;
    public final FileConfig config;
    private final List<Field> settingsEntry;
    private final List<Field> subSettings;

    protected TomlReader(FileConfig config) {
        this(null, config);
    }

    protected TomlReader(@Nullable String prefixPath, FileConfig config) {
        this(prefixPath, null, config);
    }

    protected TomlReader(@Nullable String prefixPath, TomlReader parent) {
        this(prefixPath, parent, parent.config);
    }

    protected TomlReader(@Nullable String prefixPath, @Nullable TomlReader parent, FileConfig config) {
        this.prefixPath = prefixPath != null ? (parent != null ? parent.getPrefixPathWithDot() : "") + prefixPath : null;
        this.parent = parent;
        this.config = config;
        this.settingsEntry = new ArrayList<>();
        this.subSettings = new ArrayList<>();
        List<Field> tmp = null;
        for (Field field : this.getClass().getFields()) {
            if (field.isAnnotationPresent(SettingsEntry.class) && TomlReader.class.isAssignableFrom(field.getType())) {
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

    protected ConfigSpec spec(ConfigSpec base) {
        for (Field field : settingsEntry) {
            try {
                Object defaultValue = field.get(this);
                base.define(getKey(field), defaultValue, o -> {
                    Class<?> fieldType = field.getType();
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
                ((TomlReader) field.get(this)).spec(base);
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while defining %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
        return base;
    }

    public void load() {
        config.load();

        ConfigSpec spec = new ConfigSpec();
        spec = spec(spec);
        int correct = spec.correct(config, (action, path, incorrectValue, correctedValue) -> LOGGER.warn("Corrected {} from {} to {}", path, incorrectValue, correctedValue));

        load(config);
        if (correct > 0) {
            save(config);
        }
    }

    protected void load(FileConfig config) {
        for (Field field : settingsEntry) {
            try {
                if (config instanceof CommentedFileConfig cfc) {
                    Arrays.stream(field.getAnnotationsByType(SettingsComment.class))
                            .map(SettingsComment::value)
                            .toList()
                            .forEach(comment -> cfc.setComment(getKey(field), comment));
                }
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
                if (config instanceof CommentedFileConfig cfc) {
                    Arrays.stream(field.getAnnotationsByType(SettingsComment.class))
                            .map(SettingsComment::value)
                            .toList()
                            .forEach(comment -> cfc.setComment(getKey(field), comment));
                }
                ((TomlReader) field.get(this)).load(config);
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

    @SuppressWarnings("unchecked")
    protected void save0(FileConfig config) {
        for (Field field : settingsEntry) {
            try {
                config.set(getKey(field), field.get(this));
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while saving %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
        for (Field field : subSettings) {
            try {
                ((TomlReader) field.get(this)).save0(config);
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while saving %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
    }

    protected Map<String, Object> values() {
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
                values.putAll(((TomlReader) field.get(this)).values());
            } catch (IllegalAccessException e) {
                Settings.LOGGER.error("Error while getting %s from %s in config".formatted(field.getName(), this.getClass().getSimpleName()), e);
            }
        }
        return values;
    }

    protected List<String> keys() {
        return values().keySet().stream().map(this::getKey).collect(Collectors.toList());
    }

    protected String getKey(Field key) {
        return getKey(key.getName());
    }

    protected String getKey(String key) {
        return getPrefixPathWithDot() + key;
    }

    protected String getPrefixPathWithDot() {
        return prefixPath == null ? "" : prefixPath + ".";
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface SettingsEntry {

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(SettingsComments.class)
    protected @interface SettingsComment {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface SettingsComments {
        SettingsComment[] value();

    }
}
