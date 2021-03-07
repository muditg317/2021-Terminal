package com.c1games.terminal.myalgo.utility;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.myalgo.MyAlgo;

import java.util.*;

public class DefenseUtility {

  /**
   * Given a list of turrets, return the location of the "right-most" turret in the array.
   * This is just the last turret found before a null or end of array.
   * If the first element in spots is not a turret, it will return (spots.x - 1, spots.y)
   * @param move
   * @param spots
   * @return the right most turret. Null if there are no turrets.
   */
  public static Coords findRightMostTurret(GameState move, Coords[] spots) {
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
   * Returns the amount of damage taken in either corner this game. Requires MyAlgo.scoredOnLocations to work correctly.
   * @param move
   * @return
   */
  public static int cornerDamageTaken(GameState move) {
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
//    GameIO.debug().printf("cornerHasBeenHit is %d\n", totalCornerHits);
    return totalCornerHits;
  }

  /**
   * Returns the walls that need to be upgraded based on prior damage. Requires MyAlgo.wallDamage to work correctly.
   *
   */
  public static List<Coords> getHighPriorityWalls(GameState move) {
    final int MAX_PRIORITY_WALLS = 5;
    List<Coords> highPriorityWalls = new ArrayList<Coords>();

    //sort the map first into most damage to least damage
    LinkedHashMap<Coords, Double> sortedWallDamage = new LinkedHashMap<>();
    MyAlgo.wallDamage.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        .forEachOrdered(x -> sortedWallDamage.put(x.getKey(), x.getValue()));


    for (Map.Entry<Coords, Double> entry : sortedWallDamage.entrySet()) {
      double totalDamage = entry.getValue();
      double damagePerTurn = totalDamage / (move.data.turnInfo.turnNumber + 1);
      if (damagePerTurn > 3 && totalDamage > 30) { //some criteria that will upgrade the wall
        highPriorityWalls.add(entry.getKey());
      }
      if (highPriorityWalls.size() > MAX_PRIORITY_WALLS) {
        break;
      }
    }
//    GameIO.debug().println("Sorted Wall Damages: " + sortedWallDamage);

    return highPriorityWalls;
  }

  /**
   * Calculates some rating of our defense..
   * Cuts the board in half and counts the total SP value of the structures on the left and right side and then
   * returns the value of the lowest side
   * @param move the game state
   * @return the defensive rating
   */
  public static float ourDefenseRating(GameState move) {

    float leftRating = 0;
    float rightRating = 0;

    List<Unit>[][] allUnits = move.allUnits;
    for (int x = 0; x < allUnits.length; x++) {
      List<Unit>[] row = allUnits[x];
      for (List<Unit> units : row) {
        if (units.isEmpty() || move.isInfo(units.get(0).type)) {
          continue;
        }
        Unit unit = units.get(0);
        if (unit.owner == PlayerId.Player1) { //this is our structure
          float unitValue = 0;
          unitValue += unit.unitInformation.cost1.getAsDouble() * unit.health / unit.unitInformation.startHealth.getAsDouble();
          if (x < 13) {
            leftRating += unitValue;
          } else {
            rightRating += unitValue;
          }
        }
      }
    }
    return 2 * Math.min(leftRating, rightRating);
  }

}
