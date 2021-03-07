package com.c1games.terminal.myalgo.attack;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.units.UnitType;

public class ExpectedDefense {
  /**
   * The path that has this expected defense
   */
  Coords[] path;
  /**
   * The total structure health that needs to be killed (or expected.. depending on use case)
   */
  float structureHealth;
  /**
   * The expected damage that each unit type would receive if it followed the related path for this defense
   */
  double expectedScoutDamage;
  double expectedDemolisherDamage;
  double expectedIntercepterDamage;

  public ExpectedDefense(GameState move, Coords[] path, float structureHealth, float expectedDamage) {
    this.path = path;
    this.structureHealth = structureHealth;
    this.expectedScoutDamage = expectedDamage * (1 / move.config.unitInformation.get(UnitType.Scout.ordinal()).speed.orElse(1));
    this.expectedDemolisherDamage = expectedDamage * (1 / move.config.unitInformation.get(UnitType.Demolisher.ordinal()).speed.orElse(2));
    this.expectedIntercepterDamage = expectedDamage * (1 / move.config.unitInformation.get(UnitType.Interceptor.ordinal()).speed.orElse(4));
  }
}
