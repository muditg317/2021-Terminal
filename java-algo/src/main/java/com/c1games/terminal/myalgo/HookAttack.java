package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;

public class HookAttack {
  Coords[] walls;
  Coords[] supportTowers;
  Coords[] turrets;

  UnitCounts units;

  Coords[] scouts;
  Coords[] interceptors;
  Coords[] demolishers;

  ExpectedDefense expectedDefense;

  public HookAttack(GameState move, Coords[] walls, Coords[] supportTowers, Coords[] turrets, Coords[] scouts, Coords[] interceptors, Coords[] demolishers, ExpectedDefense expectedDefense) {
    this.walls = walls;
    this.supportTowers = supportTowers;
    this.turrets = turrets;
    this.units = new UnitCounts(move, scouts.length, interceptors.length, demolishers.length);
    this.scouts = scouts;
    this.interceptors = interceptors;
    this.demolishers = demolishers;
    this.expectedDefense = expectedDefense;
  }
}
