package com.c1games.terminal.algo.map;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.units.UnitType;

/**
 * A unit which is at a certain position on the map.
 */
public class Unit {
    public final UnitType type;
    public float health;
    public final String id;
    public final PlayerId owner;
    public final Config.UnitInformation unitInformation;
    public boolean removing = false;
    public boolean upgraded = false;

    public Unit(UnitType type, float health, String id, PlayerId owner, Config.UnitInformation unitConfig) {
        unitInformation = new Config.UnitInformation(unitConfig);
        this.type = type;
        this.health = health;
        this.id = id;
        this.owner = owner;
    }

    public Unit(UnitType type, float health, String id, PlayerId owner, Config config) {
        this(type, health, id, owner, config.unitInformation.get(type.ordinal()));
    }

    public Unit(UnitType type, PlayerId owner, Config config) {
        this(type, (float) config.unitInformation.get(type.ordinal()).startHealth.orElseThrow(), "new unit id", owner, config);
    }

    public Unit(Unit toCopy) {
        this(toCopy.type, toCopy.health, toCopy.id, toCopy.owner, toCopy.unitInformation);
        if (toCopy.upgraded) upgraded = true;
        if (toCopy.removing) removing = true;
    }

    public void upgrade() {
        upgraded = true;
        unitInformation.upgrade();
    }


}
