package com.c1games.terminal.myalgo;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;

import java.util.List;
public class ScoutRush {
  private static final Coords[] UPGRADED_SUPPORT_LOCATIONS = {


//          new Coords(7, 9),
//          new Coords(8, 9),
//          new Coords(9, 9),

      new Coords(8, 8),
      new Coords(9, 8),
      new Coords(10, 8),

      new Coords(9, 7),
      new Coords(10, 7),
      new Coords(11, 7),

      new Coords(10, 6),
      new Coords(11, 6),
      new Coords(12, 6),
  }; //TODO: Optimize this list or maybe even make a more risky layout
  private static final Coords UNUPGRADED_SUPPORT_LOCATION = new Coords(13, 3);


  Coords startCoord;
  int expectedDamage;
  int numScouts;
  int scoutHealth;
  int targetSide;
  int spBudget;

  public ScoutRush(Coords startCoord, int spBudget, int numScouts, int scoutHealth) {
    this.startCoord = startCoord;
    this.spBudget = spBudget;
    this.numScouts = numScouts;
    this.scoutHealth = scoutHealth;
    this.targetSide = startCoord.x <= 13 ? MapBounds.EDGE_TOP_RIGHT : MapBounds.EDGE_TOP_LEFT;
  }

  /**
   * Considers the current board state and evaluates whether or not a ping rush should happen.
   * Considers two attacks, one starting on (13,0) and (14,0) and returns the better one.
   * If it does decide, all MP will be used for pings and SP will be used for temporary shield bois
   * @param move
   * @return the better scoutrush. Null if BAD.
   */
  public static ScoutRush evaluate(GameState move, int spBudget) {
    int mp = (int) move.data.p1Stats.bits;
    double scoutBaseHealth = move.config.unitInformation.get(UnitType.Scout.ordinal()).startHealth.orElse(15);;
    int estimatedScoutHealth = (int) (scoutBaseHealth + (spBudget * 3.0 / 4.0));
    ScoutRush leftSr = new ScoutRush(new Coords(14, 0), spBudget, mp, estimatedScoutHealth);
    ScoutRush rightSr = new ScoutRush(new Coords(13, 0), spBudget, mp, estimatedScoutHealth);
    leftSr.calculateSurvivingScouts(move); //targeting the left side
    rightSr.calculateSurvivingScouts(move);

    ScoutRush bestSr = leftSr.expectedDamage >= rightSr.expectedDamage ? leftSr : rightSr;
    if (bestSr.expectedDamage < /*some threshold TODO: i bumped this to 3 from 4 (put it back to test something) */ (mp / 4) + (move.data.p2Stats.integrity / 10) + 3 && bestSr.expectedDamage < move.data.p2Stats.integrity) {
      return null; //don't do the ping attack
    }
    //then now, we shall do the attack - EXECUTE!

    return bestSr;
  }

  /**
   * Executes this scout rush
   * @param move
   */
  public void execute(GameState move) {
    placeSupports(move);
    SpawnUtility.spawnScouts(move, startCoord, numScouts);
  }



  /**
   *
   * @param move
   * @return the number of scouts expected to survive and deal damage to enemy health
   */
  public int calculateSurvivingScouts(GameState move) {
    double currHealth = scoutHealth;
    int scoutsRemaining = numScouts; // used to just change numScouts which seems wrong
    List<Coords> path = move.pathfind(startCoord, targetSide);
    if (!StrategyUtility.pathHitsTargetEdge(path, targetSide)) { //this explodes ;(
      return 0;
    }
    for (Coords pathPoint : path) {
      List<Unit> attackers = move.getAttackers(pathPoint);
      for (Unit attacker : attackers) {
        double attackerDamage = attacker.unitInformation.attackDamageWalker.orElse(0);
        currHealth -= attackerDamage;
        if (currHealth <= 0) {
          scoutsRemaining--;
          currHealth = scoutHealth;
          if (scoutsRemaining == 0) { //break early
            return 0;
          }
        }
      }
    }
    this.expectedDamage = scoutsRemaining;
    return scoutsRemaining; // TODO: were you intentionally changing the numScouts value which is why we always sent less scouts than we actually had
  }

  /**
   * Uses ALL SP to place supports and marks them for immediate deletion.
   * @param move
   */
  private void placeSupports(GameState move) {

    for (int i = 0; i < UPGRADED_SUPPORT_LOCATIONS.length && spBudget >= 8; i++) {
      Coords location = UPGRADED_SUPPORT_LOCATIONS[i];
      if (i < 3) {
        //place the wall down
        Coords wallLocation = new Coords(location.x, location.y + 1);
        SpawnUtility.placeWall(move, wallLocation);
        SpawnUtility.removeBuilding(move, wallLocation);
        spBudget--;
      }
      if (spBudget < 8) {
        break;
      }
      SpawnUtility.placeSupport(move, location);
      SpawnUtility.applyUpgrade(move, location);
      SpawnUtility.removeBuilding(move, location);
      //update sp
      spBudget -= 8;
    }
    //at this point we can only afford one or 0 supports (this may or may not place)
    if (spBudget >= 4 && SpawnUtility.placeSupport(move, UNUPGRADED_SUPPORT_LOCATION)) { //placed
      SpawnUtility.removeBuilding(move, UNUPGRADED_SUPPORT_LOCATION);
    }
  }



}
