package com.c1games.terminal.myalgo.attack;

import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.myalgo.utility.SpawnUtility;

public class UnitCounts {
  int numScouts;
  int numInterceptors;
  int numDemolishers;

  int cost;

  public UnitCounts(GameState move, int numScouts, int numInterceptors, int numDemolishers) {
    this.numScouts = numScouts;
    this.numInterceptors = numInterceptors;
    this.numDemolishers = numDemolishers;
    this.cost = numScouts * SpawnUtility.getUnitCost(move, UnitType.Scout, false)
        + numInterceptors * SpawnUtility.getUnitCost(move, UnitType.Interceptor, false)
        + numDemolishers * SpawnUtility.getUnitCost(move, UnitType.Demolisher, false);
  }

  @Override
  public String toString() {
    return String.format("Scouts: %d, Inters: %d, Demos: %d. Cost: %d",numScouts,numInterceptors,numDemolishers,cost);
  }


  public void scale(float f) {
    this.numScouts *= f;
    this.numInterceptors *= f;
    this.numDemolishers *= f;
    this.cost *= f;
  }
}
