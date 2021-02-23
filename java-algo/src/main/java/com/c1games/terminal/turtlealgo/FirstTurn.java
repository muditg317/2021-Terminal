package com.c1games.terminal.turtlealgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.FrameData;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.io.GameLoop;
import com.c1games.terminal.algo.io.GameLoopDriver;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FirstTurn {
  private static TurtleAlgo algoState;
  private static GameState move;

  /**
   * Places the two factories, turret, and wall as first turn
   * @param move the game state dude bruh
   */
  static void execute(TurtleAlgo algoState, GameState move) {
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
