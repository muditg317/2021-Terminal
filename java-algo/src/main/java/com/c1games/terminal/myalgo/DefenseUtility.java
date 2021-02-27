package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.Unit;

public class DefenseUtility {

  /**
   * Given a list of turrets, return the location of the "right-most" turret in the array.
   * This is just the last turret found before a null or end of array.
   * If the first element in spots is not a turret, it will return (spots.x - 1, spots.y)
   * @param move
   * @param spots
   * @return the right most turret. Null if there are no turrets.
   */
  static Coords findRightMostTurret(GameState move, Coords[] spots) {
    Coords lastTurretLocation = null;
    for(Coords location : spots) {
      Unit unit = move.getWallAt(location);
      if (unit != null && move.getWallAt(location).type == Utility.TURRET) {
        lastTurretLocation = location;
      } else {
        break;
      }
    }

    return lastTurretLocation;
  }

  /**
   * Returns whether or not we have been hit in either corner this game. Requires MyAlgo.scoredOnLocations to work correctly.
   * @param move
   * @return
   */
  static boolean cornerHasBeenHit(GameState move) {
    final Coords[] cornerLocations = {
        new Coords(0, 13),
        new Coords(1, 12),
        new Coords(27, 13),
        new Coords(26, 12)
    };
    int totalCornerHits = 0;
    for (Coords loc : cornerLocations) {
      totalCornerHits += MyAlgo.scoredOnLocations.getOrDefault(loc, 0);
    }
    return totalCornerHits > 0;
  }

}
