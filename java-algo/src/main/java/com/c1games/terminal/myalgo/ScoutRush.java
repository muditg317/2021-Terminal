package com.c1games.terminal.myalgo;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;

import java.util.List;
public class ScoutRush {
  private static final Coords[] UPGRADED_SUPPORT_LOCATIONS = {
          new Coords(6, 10),
          new Coords(7, 10),
          new Coords(8, 10),

          new Coords(7, 9),
          new Coords(8, 9),
          new Coords(9, 9),

          new Coords(8, 8),
          new Coords(9, 8),
          new Coords(10, 8),

          new Coords(9, 7),
          new Coords(10, 7),
          new Coords(11, 7),
  }; //TODO: Optimize this list or maybe even make a more risky layout
  private static final Coords UNUPGRADED_SUPPORT_LOCATION = new Coords(13, 3);
  /**
   * Considers the current board state and evaluates whether or not a ping rush should happen.
   * If it does decide, all MP will be used for pings and SP will be used for temporary shield bois
   * @param move
   * @return True if the attack was done. False if nothing was done.
   */
  public static boolean evaluateAndMaybeExecute(GameState move) {
    int mp = (int) move.data.p1Stats.bits;
    int sp = (int) move.data.p1Stats.cores;
    double scoutBaseHealth = move.config.unitInformation.get(UnitType.Scout.ordinal()).startHealth.orElse(15);;
    int estimatedScoutHealth = (int) (scoutBaseHealth + (sp * 3.0 / 4.0));
    int leftSurvivingScouts = calculateSurvivingScouts(move, new Coords(14, 0), mp, estimatedScoutHealth); //targeting the left side
    int rightSurvivingScouts = calculateSurvivingScouts(move, new Coords(13, 0), mp, estimatedScoutHealth);

    int bestSurvivingScouts = Math.max(leftSurvivingScouts, rightSurvivingScouts);
    if (bestSurvivingScouts < /*some threshold*/ (mp / 2) && bestSurvivingScouts < move.data.p1Stats.integrity) {
      return false; //don't do the ping attack
    }
    //then now, we shall do the attack - EXECUTE!
    Coords startCoord = leftSurvivingScouts > rightSurvivingScouts ? new Coords(14, 0) : new Coords(13, 0);
    placeSupports(move);
    SpawnUtility.spawnScouts(move, startCoord, mp);
    return true;
  }



  /**
   *
   * @param move
   * @param start
   * @param numScouts
   * @param scoutHealth
   * @return the number of scouts expected to survive and deal damage to enemy health
   */
  public static int calculateSurvivingScouts(GameState move, Coords start, int numScouts, int scoutHealth) {
    double currHealth = scoutHealth;
    int side = start.x <= 13 ? MapBounds.EDGE_TOP_RIGHT : MapBounds.EDGE_TOP_LEFT;
    List<Coords> path = move.pathfind(start, side);
    if (!StrategyUtility.pathHitsTargetEdge(path, side)) { //this explodes ;(
      return 0;
    }
    for (Coords pathPoint : path) {
      List<Unit> attackers = move.getAttackers(pathPoint);
      for (Unit attacker : attackers) {
        double attackerDamage = attacker.unitInformation.attackDamageWalker.orElse(0);
        currHealth -= attackerDamage;
        if (currHealth <= 0) {
          numScouts--;
          currHealth = scoutHealth;
          if (numScouts == 0) { //break early
            return 0;
          }
        }
      }
    }
    return numScouts;
  }

  /**
   * Uses ALL SP to place supports and marks them for immediate deletion.
   * @param move
   */
  private static void placeSupports(GameState move) {
    int sp = (int) move.data.p1Stats.cores;
    for (int i = 0; i < UPGRADED_SUPPORT_LOCATIONS.length && sp >= 8; i++) {
      Coords location = UPGRADED_SUPPORT_LOCATIONS[i];
      SpawnUtility.placeSupport(move, location);
      SpawnUtility.applyUpgrade(move, location);
      SpawnUtility.removeBuilding(move, location);
      //update sp
      sp = (int) move.data.p1Stats.cores;
    }
    //at this point we can only afford one or 0 supports (this may or may not place)
    if (SpawnUtility.placeSupport(move, UNUPGRADED_SUPPORT_LOCATION)) { //placed
      SpawnUtility.removeBuilding(move, UNUPGRADED_SUPPORT_LOCATION);
    }
  }



}
