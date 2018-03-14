package net.minespree.feather.repository.types;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import net.minespree.feather.repository.types.util.Toggleable;
import net.minespree.feather.repository.types.util.TypePreconditions;

import java.lang.reflect.Field;
import java.util.Map;

public class EnumType<T extends Enum> implements Type, Toggleable {
    private final String name;
    private final Class<T> enumClass;
    private final BiMap<T, String> nameMapping;

    public EnumType(String name, Class<T> enumClass) {
        Preconditions.checkNotNull(name, "name may not be null");
        Preconditions.checkNotNull(enumClass, "enum may not be null");
        Preconditions.checkArgument(enumClass.isEnum(), "enum must be enum");
        Preconditions.checkArgument(enumClass.getEnumConstants().length > 0, "enum must have at least one constant");

        this.name = name;
        this.enumClass = enumClass;

        BiMap<T, String> tempMapping = HashBiMap.create();

        for (Field field : enumClass.getFields()) {
            if (field.isEnumConstant()) {
                T value = (T) Enum.valueOf(enumClass, field.getName());

                Name declaredEnumName = field.getAnnotation(Name.class);

                if (declaredEnumName != null) {
                    tempMapping.put(value, declaredEnumName.value());
                } else {
                    tempMapping.put(value, field.getName());
                }
            }
        }

        this.nameMapping = ImmutableBiMap.copyOf(tempMapping);
    }

    @Override
    public String getName() {
        return "Enum of " + this.name;
    }

    @Override
    public boolean isInstance(Object obj) {
        return enumClass.isInstance(obj);
    }

    @Override
    public Object getDefault() {
        return null;
    }

    @Override
    public Object parse(String raw) throws TypeParseException {
        Preconditions.checkNotNull(raw);

        T obj = findByName(raw);

        if (obj != null) {
            return obj;
        }

        throw new TypeParseException("Unknown option '" + raw + "'");
    }

    private T findByName(String name) {
        for (Map.Entry<T, String> entry : nameMapping.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }

        return null;
    }

    @Override
    public String toString(Object obj) throws IllegalArgumentException {
        Preconditions.checkArgument(isInstance(obj));

        return ((T) obj).name();
    }

    @Override
    public Object getNextState(Object previous) throws IllegalArgumentException {
        TypePreconditions.checkInstance(this, previous);

        T[] constants = enumClass.getEnumConstants();
        int index = -1;

        for (int i = 0; i < constants.length; i++) {
            if (previous.equals(constants[i])) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            throw new IllegalArgumentException("previous is not an enum constant");
        }

        int newIndex = index + 1;

        if (newIndex >= constants.length) {
            newIndex = 0;
        }

        return constants[newIndex];
    }
}
