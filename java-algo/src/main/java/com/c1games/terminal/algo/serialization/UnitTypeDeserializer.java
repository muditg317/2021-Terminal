package com.c1games.terminal.algo.serialization;

import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.algo.units.UnitTypeAtlas;
import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * GSON deserializer for unit types from their shorthand strings as defined in the config.
 */
public class UnitTypeDeserializer implements JsonDeserializer<UnitType>, JsonSerializer<UnitType> {
    private final UnitTypeAtlas atlas;

    public UnitTypeDeserializer(UnitTypeAtlas atlas) {
        this.atlas = atlas;
    }

    @Override
    public JsonElement serialize(UnitType src, Type typeOfSrc, JsonSerializationContext context) {

        return new JsonPrimitive(atlas.getString(src));

    }

    @Override
    public UnitType deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            String string = jsonElement.getAsString();
            UnitType unitType = atlas.getUnitType(string);
            if (unitType == null) {
                int index;
                try {
                    index = (int) jsonElement.getAsDouble();
                } catch (ClassCastException e) {
                    throw new JsonParseException("Invalid unit name: " + string);
                }
                if (index >= 0 && index < UnitType.values().length) {
                    return UnitType.values()[index];
                } else {
                    throw new JsonParseException("Invalid unit name: " + string);
                }
            }
            return unitType;
        } catch (Exception e) {
            throw new JsonParseException(e);
        }
    }
}
