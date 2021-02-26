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
    Coords[] firstTurrets = {Locations.Essentials.firstLeftTurret, Locations.Essentials.firstRightTurret};
    Coords[] firstWalls = {Locations.Essentials.firstLeftTurretWall, Locations.Essentials.firstRightTurretWall};
    //Get the core corner turrets downs
    SpawnUtility.placeTurrets(move, firstTurrets);

    SpawnUtility.applyUpgrades(move, firstTurrets);
    SpawnUtility.placeWalls(move, firstWalls);
    //get the main wall down (this won't place all main walls down but that's okay).
    SpawnUtility.placeWalls(move, Locations.Essentials.mainWallCoords);

    //place corner walls down


    //spawn mobile units - Right now we are doing nothing
    //Hailmary destructor
    SpawnUtility.spawnDemolishers(move, new Coords(4, 9), 1);
    //Utility.spawnInterceptors(move, Locations.spacedInters5, 1);
  }
}
