package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;

public class FirstTurn {
  private static MyAlgo algoState;
  private static GameState move;

  /**
   * Places the two factories, turret, and wall as first turn
   * @param move the game state dude bruh
   */
  static void execute(MyAlgo algoState, GameState move) {
    FirstTurn.algoState = algoState;
    FirstTurn.move = move;

    //place buildings
//    final Coords[] turrets = {new Coords(13,1)};
//    final Coords[] walls = {new Coords(14,1)};
    final Coords[] factories = {new Coords(13,3), new Coords(14,3)};
    final Coords[] upgrades = {new Coords(13,3)};
//    Utility.placeTurrets(move, turrets);
//    Utility.placeWalls(move, walls);
    Utility.placeFactories(move, factories);
    Utility.applyUpgrades(move, upgrades);

    //spawn mobile units
    Utility.spawnInterceptors(move, Locations.spacedInters5, 1);
  }
}
