package com.c1games.terminal.myalgo.attack;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.myalgo.utility.Locations;
import com.c1games.terminal.myalgo.utility.SpawnUtility;
import com.c1games.terminal.myalgo.utility.StrategyUtility;
import com.c1games.terminal.myalgo.utility.Utility;

import java.util.*;

public class ScoutRush extends Attack {
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

  static double onlineAdjustment = 1; //online learning parameter


  Coords startCoord;
  Coords exitingHole;
  int expectedDamage;
  int numScouts;
  int scoutHealth;
  int targetSide;
  double spBudget;


  public ScoutRush(Coords startCoord, Coords exitingHole, double spBudget, int numScouts, int scoutHealth) {
    this.startCoord = startCoord;
    this.exitingHole = exitingHole;
    this.spBudget = spBudget;
    this.numScouts = numScouts;
    this.scoutHealth = scoutHealth;
    this.targetSide = startCoord.x <= 13 ? MapBounds.EDGE_TOP_RIGHT : MapBounds.EDGE_TOP_LEFT;
    this.clearLocations = new HashSet<>();
    this.clearLocations.add(exitingHole);
  }

  @Override
  public double getExpectedAttackValue() {
    return expectedDamage;
  }

  /**
   * Considers the current board state and evaluates whether or not a ping rush should happen.
   * Considers two attacks, one starting on (13,0) and (14,0) and returns the better one.
   * If it does decide, all MP will be used for pings and SP will be used for temporary shield bois
   * @param move
   * @return the better scoutrush. Null if BAD.
   */
  public static ScoutRush evaluate(GameState move, double spBudget) {
    int mp = (int) move.data.p1Stats.bits;
    List<ScoutRush> potentialSrs = new ArrayList<>();
    for (Coords exit : Locations.Essentials.mainWallHookHoles) {
      GameState scoutRushState = Utility.duplicateState(move);
      int remainingSpBudget = (int) (spBudget - Utility.fillOtherHookHoles(scoutRushState, exit));
      double scoutBaseHealth = move.config.unitInformation.get(UnitType.Scout.ordinal()).startHealth.orElse(15);;
      int estimatedSupports = (int) (remainingSpBudget / 4);
      int estimatedScoutHealth = (int) (scoutBaseHealth + (estimatedSupports * 3.5));

      ScoutRush leftSr = new ScoutRush(new Coords(14, 0), exit, spBudget, mp, estimatedScoutHealth);
      ScoutRush rightSr = new ScoutRush(new Coords(13, 0), exit, spBudget, mp, estimatedScoutHealth);

      leftSr.scoutHealth = estimatedScoutHealth;
      rightSr.scoutHealth = estimatedScoutHealth;
      leftSr.calculateSurvivingScouts(scoutRushState); //targeting the left side
      rightSr.calculateSurvivingScouts(scoutRushState);
      potentialSrs.add(leftSr);
      potentialSrs.add(rightSr);
    }

    potentialSrs.sort((sr1, sr2) -> {
      return sr2.expectedDamage - sr1.expectedDamage;
    });



    if (potentialSrs.isEmpty()) {
      GameIO.debug().println("ERROR: No scout rushes evaluated???");
      return null;
    }

    ScoutRush bestSr = potentialSrs.get(0);

    GameIO.debug().printf("BEST SCOUT RUSH EVALUATION==========\n\tSpBudget:%.2f\tNumScouts:%d\tScoutHealth: %d\n\tExpected Damage:%d\tOnline Adjustment:%.2f\n",
        bestSr.spBudget, bestSr.numScouts, bestSr.scoutHealth, bestSr.expectedDamage, ScoutRush.onlineAdjustment);

    decayLearning();

    if (bestSr.expectedDamage < /*some threshold TODO: i bumped this to 3 from 4 (put it back to test something) */ (mp / 4) + (move.data.p2Stats.integrity / 10) + 3 && bestSr.expectedDamage < move.data.p2Stats.integrity) {
      return null; //don't do the ping attack
    }

    return bestSr;
  }

  /**
   * Executes this scout rush. Fills other hook holes that are not needed.
   * @param move
   */
  public void execute(GameState move) {
    Utility.fillOtherHookHoles(move, exitingHole);
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
    this.expectedDamage = (int) (onlineAdjustment * scoutsRemaining);
    return this.expectedDamage;
  }

  /**
   * Uses ALL SP to place supports and marks them for immediate deletion.
   * @param move
   */
  private void placeSupports(GameState move) {
    /*
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
    */

    for (int y = 1; y < 7; y++) {
      for (int x = startCoord.x - 3; x < startCoord.x + 3; x++) {
        Coords loc = new Coords(x, y);
        if (spBudget < 4) {
          return;
        }
        if (x == startCoord.x) {
          continue;
        }
        if (SpawnUtility.placeSupport(move, loc)) {
          SpawnUtility.removeBuilding(move, loc);
          spBudget -= 4;
        }
      }
    }
  }

  /**
   * Given the actual damage, adjust the onlineAdjustment parameter
   * @param actualDamage the actual damage done by this scout rush
   */
  public void learn(double actualDamage) {
    onlineAdjustment *= actualDamage / expectedDamage;
    GameIO.debug().printf("SR: Expected Damage was %d: Actual damage was: %.2f\n", this.expectedDamage, actualDamage);
    GameIO.debug().printf("SR: The onlineAdjustment value is now %.2f\n", onlineAdjustment);
  }

  public static void decayLearning() {
    onlineAdjustment = (onlineAdjustment + 1) / 2.0;
  }

  /**
   * Returns the evaluation or "score" of this ScoutRush
   * @return
   */
  public double evaluation(GameState move) {
    return expectedDamage / move.data.p2Stats.integrity;
  }

}
