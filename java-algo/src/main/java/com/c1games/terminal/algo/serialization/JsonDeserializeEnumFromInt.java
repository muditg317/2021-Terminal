package com.c1games.terminal.algo.serialization;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * GSON deserializer which deserializes an enum from an integer, simply producing the variant of the enum with the index that corresponds to
 * the JSON iterator.
 */
public class JsonDeserializeEnumFromInt<T> implements JsonDeserializer<T>, JsonSerializer<T> {
    private final Class<T> typeClass;
    private int offset;

    public JsonDeserializeEnumFromInt(Class<T> typeClass) {
        this.typeClass = typeClass;
        StartIndexAt[] startIndexAt = typeClass.getAnnotationsByType(StartIndexAt.class);
        if (startIndexAt.length > 0)
            offset = startIndexAt[0].value();
        else
            offset = 0;
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(Arrays.stream(typeClass.getEnumConstants()).collect(Collectors.toList()).indexOf(src) + offset);
    }

    @Override
    public T deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            return typeClass.getEnumConstants()[jsonElement.getAsInt() - offset];
        } catch (Exception e) {
            throw new JsonParseException(e);
        }
    }
}
