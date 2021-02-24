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
    //Get the core corner turrets down
    Coords firstLeftTurret = new Coords(3, 13);
    SpawnUtility.placeTurrets(move, new Coords[]{firstLeftTurret});
    Coords firstRightTurret = new Coords(24, 13);
    SpawnUtility.placeTurrets(move, new Coords[]{firstRightTurret});
    SpawnUtility.applyUpgrades(move, new Coords[]{firstLeftTurret, firstRightTurret});
    //get the main wall down
    SpawnUtility.placeWalls(move, Locations.mainWallCoords);

    //place down left entrance turret
    Coords leftEntranceTurret = new Coords(4, 11);
    SpawnUtility.placeTurrets(move, new Coords[]{leftEntranceTurret});

    //place corner walls down


    //spawn mobile units - Right now we are doing nothing
    //Utility.spawnInterceptors(move, Locations.spacedInters5, 1);
  }
}
