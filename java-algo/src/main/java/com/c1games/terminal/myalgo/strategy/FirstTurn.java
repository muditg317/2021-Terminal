package com.c1games.terminal.myalgo.strategy;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.myalgo.MyAlgo;
import com.c1games.terminal.myalgo.utility.Locations;
import com.c1games.terminal.myalgo.utility.SpawnUtility;

public class FirstTurn {
  private static MyAlgo algoState;
  private static GameState move;

  /**
   * Places the two factories, turret, and wall as first turn
   * @param move the game state dude bruh
   */
  public static void execute(MyAlgo algoState, GameState move) {
    FirstTurn.algoState = algoState;
    FirstTurn.move = move;

    //place buildings
    Coords[] firstTurrets = {Locations.Essentials.firstLeftTurret, Locations.Essentials.firstRightTurret};
    Coords[] firstWalls = {Locations.Essentials.firstLeftTurretWall, Locations.Essentials.firstRightTurretWall};
    //Get the core corner turrets downs
    SpawnUtility.placeTurrets(move, Locations.Essentials.mainTurrets);
    SpawnUtility.applyUpgrade(move, Locations.Essentials.mainTurrets[0]);
    SpawnUtility.applyUpgrade(move, Locations.Essentials.mainTurrets[2]);

    SpawnUtility.placeWalls(move, Locations.Essentials.cornerWalls);
    //get more walls down infront of turrets
    SpawnUtility.placeWalls(move, Locations.Essentials.mainTurretWalls);

    //place corner walls down


    //spawn mobile units - Right now we are doing nothing
    //Hailmary destructor
//    SpawnUtility.spawnDemolishers(move, new Coords(4, 9), 1);
    //Utility.spawnInterceptors(move, Locations.spacedInters5, 1);
  }
}
