package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.pathfinding.IllegalPathStartException;
import com.c1games.terminal.algo.units.UnitType;

import java.util.*;

public class StrategyUtility {
  /**
   * Returns the weaker side ("LEFT", "RIGHT") and the num of estimated bits needed to break the wall
   * @param move  the game state for which we're predicting
   * @return AttackBreakdown with where to attack and the related units needed
   */
  static AttackBreakdown attackThreshold(GameState move) {
    UnitCounts leftUnitCounts = enemyDefenseHeuristic(move, "LEFT");
    int leftCost = leftUnitCounts.cost;

    UnitCounts rightUnitCounts = enemyDefenseHeuristic(move, "RIGHT");
    int rightCost = rightUnitCounts.cost;

    String weakerSide = leftCost < rightCost ? "LEFT" : "RIGHT";
    UnitCounts correctUnitCounts = leftCost < rightCost ? leftUnitCounts : rightUnitCounts;
    return new AttackBreakdown(weakerSide, correctUnitCounts);
  }

  /**
   * Returns the estimated bits needed to break the wall in X turns based on the weaker side.
   * @param move   the game state we are predicting for
   * @param turns  the number of turns we will look into the future
   * @return AttackBreakdown with where to attack and the related units needed
   */
  static AttackBreakdown futureAttackThreshold(GameState move, int turns) {
    // TODO: FIX ME
    // TODO: apply some scaling if they have more turns available
    AttackBreakdown curr = attackThreshold(move);
    curr.units.scale(1.4f);
    return curr;
  }

  /**
   * Guess of how strong their corner defense is on a certain side
   * @param move the game state to inspect
   * @param side which side we want to measure
   * @return UnitCounts with the number of scouts, interceptors, and demolishers needed
   */
  static UnitCounts enemyDefenseHeuristic(GameState move, String side) {
    int enemyHealth = (int) move.data.p2Stats.integrity; //min amount we need to hit them by
    //walls only matter in a few locations -> we include turret healths since they are "walls"

    ExpectedDefense cornerSummary = enemyCornerSummary(move, side);
    int intersNeeded = (int) Math.ceil((cornerSummary.structureHealth + cornerSummary.expectedIntercepterDamage) / move.config.unitInformation.get(UnitType.Interceptor.ordinal()).startHealth.orElse(40));
    if (cornerSummary.structureHealth == 0) {
      intersNeeded = 0;
    }
    intersNeeded = (int) Math.ceil(0.9 * intersNeeded);
    //intersNeeded = Math.max(intersNeeded, cornerSummary.expectedIntercepterDamage < 1 ? 6 : 3); // use at least some inters
    int scoutBaseHealth = (int) move.config.unitInformation.get(UnitType.Interceptor.ordinal()).startHealth.orElse(15);
    int expectedShielding = (int) (move.data.p1Stats.cores * 3.0 / 4.0);
    int scoutHealth = scoutBaseHealth + expectedShielding;
    int minimumDamage = (int) Math.min(enemyHealth, enemyHealth / 10 + 5);
    int scoutsNeeded = (int) Math.ceil(cornerSummary.expectedScoutDamage / 2 / scoutHealth + minimumDamage);

    return new UnitCounts(move, scoutsNeeded, intersNeeded, 0);
  }

  /**
   * Returns an array of the wall value and the turret value
   * Wall Value = return[0] = sum of the building healths in the corner with some discounts for edge walls
   * Turret Value = return[1] = total damage we will taking moving at 1 speed (scout)
   * @param side the side we are inspecting
   * @return Expected Defense for the path we will take
   */
  private static ExpectedDefense enemyCornerSummary(GameState move, String side) {
    int effectiveWallHealth;
    //walls only matter in a few locations
    int x;
//    effectiveWallHealth += move.getWallAt(new Coords(x, 14)).health;
//    x = side.equals("RIGHT") ? 26 : 1;
//    effectiveWallHealth += move.getWallAt(new Coords(x, 14)).health;
//    //these walls are discounted since they are worth less
//    effectiveWallHealth += 0.5 * move.getWallAt(new Coords(x, 15)).health;
//    x = side.equals("RIGHT") ? 25 : 2;
//    effectiveWallHealth += 0.5 * move.getWallAt(new Coords(x, 13)).health;

    //in the case we use interceptors to bomb then only two walls really matters....
    x = side.equals("RIGHT") ? 27 : 0;
    int x2= side.equals("RIGHT") ? 26 : 1;
    float wall1Health = move.getWallAt(new Coords(x, 14)) != null ? move.getWallAt(new Coords(x, 14)).health : 0;
    float wall2Health = move.getWallAt(new Coords(x2, 14)) != null ? move.getWallAt(new Coords(x2, 14)).health : 0;
    effectiveWallHealth = (int) Math.max(wall1Health, wall2Health);

    final Coords[] leftPath = {
        new Coords(3, 11),
        new Coords(2, 11),
        new Coords(2, 12),
        new Coords(1, 12),
        new Coords(1, 13),
        new Coords(1, 14),
        new Coords(0 ,14)
    };
    final Coords[] rightPath = {
        new Coords(24, 11),
        new Coords(25, 11),
        new Coords(25, 12),
        new Coords(26, 12),
        new Coords(26, 13),
        new Coords(26, 14),
        new Coords(27 ,14)
    };
    Coords[] path = side.equals("RIGHT") ? rightPath : leftPath;
    int effectiveTurretRating = 0; //how much damage we will take in total moving at 1 coord per tick
    for (Coords pathPoint : path) {
      List<Unit> attackers = move.getAttackers(pathPoint);
      for (Unit attacker : attackers) {
        if (attacker.owner == PlayerId.Player2) {
          effectiveTurretRating += attacker.unitInformation.attackDamageWalker.orElse(6);
        }
      }
    }
    return new ExpectedDefense(move, path, effectiveWallHealth, effectiveTurretRating);
  }

  /**
   * Returns the expected amount of MP we will have in X turns if we use none of it and keep all of our factories
   * @param move the game state we are extrapolating from
   * @param turns the number of turn for which we extrapolate
   * @return the MP we will have after some number of turns
   */
  static float extrapolateFutureMP(GameState move, int turns, int expectedBaseCostPerTurn) {
    float currentMP = move.data.p1Stats.bits;
    for(int i = 0; i < turns; i++) {
      float baseMPIncome = move.config.resources.bitsPerRound + move.config.resources.bitGrowthRate * (move.data.turnInfo.turnNumber + i) / move.config.resources.turnIntervalForBitSchedule;
      currentMP *= (1 - move.config.resources.bitDecayPerRound);
      //currentMP -= currentMP%0.01; //TODO: What was this line doing? I commented it out for now...
      currentMP += baseMPIncome - expectedBaseCostPerTurn;
    }
    return currentMP;
  }

  /**
   *
   * @param move
   * @return the estimated number of inters needed to defend a scout rush this turn given our defenses
   */
  static int neededScoutRushDefense(GameState move) {
    int mp = (int) move.data.p1Stats.bits;
    int sp = (int) move.data.p1Stats.cores;
    int turnNumber = move.data.turnInfo.turnNumber;
    int enemyMPCapacity = (int) move.data.p2Stats.bits * 5;
    double enemyMPPercentCapacity = move.data.p2Stats.bits / enemyMPCapacity;
    int neededDefenseSpending = neededDefenseSpending(move);
    int scoutRushDefense = neededDefenseSpending - sp;
    if (enemyMPPercentCapacity < 0.5) {
      scoutRushDefense = 0;
    }

    scoutRushDefense = Math.max(0, scoutRushDefense);
    scoutRushDefense = Math.min(mp, scoutRushDefense);
    return scoutRushDefense;
  }

  /**
   * Based on the current board state, enemy MP, and our defenses returns the number of cores it thinks we need to spend into defenses
   * @param move  the game state to inspect
   * @return the number of cores we should spend in defenses
   */
  static int neededDefenseSpending(GameState move) {
    float enemyMP = move.data.p2Stats.bits;
    float ourHealth = move.data.p1Stats.integrity;
    float baseMPIncome = move.config.resources.bitsPerRound + move.config.resources.bitGrowthRate * (move.data.turnInfo.turnNumber) / move.config.resources.turnIntervalForBitSchedule;
    float enemyMPIncome = baseMPIncome;
    int defenseRating = calculateTotalDefenseDamage(move);



    //we want at least 1 rating per 1 MP they have - FOLLOW IS OLD TUNE
    // double enemyAttackRating = 1.7 * enemyMP + (0 * enemyMPIncome) - (ourHealth * 0.1) + 15;


    int maxEnemyScoutRushHealth = maxEnemyScoutRushHealth(move);
    final int bias_term = 40; //TODO: Tunable (more means we need more defense as a base)
    int possibleRemainingScoutRushHealth = maxEnemyScoutRushHealth + bias_term - defenseRating;
    /*
    We divide by 15
    since 2 SP (unupgraded) tower deals ~30 dmg
    and 6 SP tower deals ~ 95 dmg
    thats about 15 damage per SP
     */
    double baseTowerCost = move.config.unitInformation.get(UnitType.Turret.ordinal()).cost1.orElse(2);
    return (int) (Math.ceil((possibleRemainingScoutRushHealth / 10.0) / baseTowerCost) * baseTowerCost);
  }

  /**
   * Calculates the maximum total health of the enemy scout rush if based on current shielding capabilities, SP, and enemy MP
   * @param move
   * @return the predicted max enemy scout rush health this turn
   */
  static int maxEnemyScoutRushHealth(GameState move) {
    int enemyMP = (int) move.data.p2Stats.bits;
    double scoutBaseHealth = move.config.unitInformation.get(UnitType.Scout.ordinal()).startHealth.orElse(15);;
    return (int) (enemyMP * (scoutBaseHealth + potentialEnemyShieldPower(move)));
  }

  /**
   * Calculates some rating of our defense by estimating the total amount of damage our defenses can do to scouts rushing through our
   * left side entrance...
   * We estimate this by taking all the turrets on the left side of our map and make the following assumptions:
   * - Upgraded towers will have an average of 7 hits (can hit 7 spots along the scout path)
   * - Unupgraded towers will have an average of 5 hits
   * - No towers die to the pings
   * - All damage rolls over perfectly (no wasted damage on overkill although this is not true)
   * @param move the game state
   * @return the defensive rating = the estimated total amount of damage it can do to scouts rushings through
   */
  private static int calculateTotalDefenseDamage(GameState move) {

   double totalDamage = 0;

    List<Unit>[][] allUnits = move.allUnits;
    for (int x = 0; x < allUnits.length; x++) {
      List<Unit>[] row = allUnits[x];
      for (List<Unit> units : row) {
        if (units.isEmpty() || move.isInfo(units.get(0).type)) {
          continue;
        }
        Unit unit = units.get(0);
        if (unit.owner == PlayerId.Player1 && unit.type == UnitType.Turret) { //this is our turret
          if (x <= 13) {
            double unitTotalDamage = unit.unitInformation.attackDamageWalker.getAsDouble(); //TODO: attackDamageWalker or attackDamageTower??
            if (unit.upgraded) {
              unitTotalDamage *= 7;
            } else {
              unitTotalDamage *= 5;
            }
            totalDamage += unitTotalDamage;
          }
        }
      }
    }
    return (int) totalDamage;
  }



  /**
   * How many of a certain unit type we can afford. Copied and modified from GameState.java
   * @param move     the game state we are checking
   * @param type     the unit type we want to spawn
   * @param upgrade  whether the unit should be upgraded
   * @param budget   the MP we have available
   * @return the number of units we can spawn
   */
  static int numAffordableWithBudget(GameState move, UnitType type, boolean upgrade, int budget) {
    if (type == UnitType.Remove) {
      throw new IllegalArgumentException("Cannot query number affordable of remove unit type use removeFirewall");
    }
    if (type == UnitType.Upgrade) {
      throw new IllegalArgumentException("Cannot query number affordable of upgrades this way, put type of unit to upgrade and upgrade=true");
    }

    if (budget <= 0) {
      return 0;
    }

    return budget / SpawnUtility.getUnitCost(move, type, upgrade);
  }

  /**
   * Calculates the current enemy shield power based on the number of support towers they have down right now and how much shield
   * they provide
   * @move the boi
   * @return the total shield power on the enemy field right now
   */
  static double currentEnemyShieldPower(GameState move) {
    double shieldPower = 0;
    List<Unit>[][] allUnits = move.allUnits;
    for (int x = 0; x < allUnits.length; x++) {
      List<Unit>[] row = allUnits[x];
      for (int y = 0; y < row.length; y++) {
        List<Unit> units = row[y];
        if (units.isEmpty() || move.isInfo(units.get(0).type)) {
          continue;
        }
        // there is a structure here
        Unit unit = units.get(0);
        if (unit.owner == PlayerId.Player2) {
          if (unit.type == Utility.SUPPORT) {
            Config.UnitInformation info = unit.unitInformation;
            shieldPower += info.shieldPerUnit.orElse(0);
            shieldPower += info.shieldBonusPerY.orElse(0) * (27 - y);
          }
        }
      }
    }
    return shieldPower;
  }

  /**
   * Calculates an estimate of the potential enemy shield power this immediate succeeding attack phase
   * if they keep all of their current shields and put all of their SP
   * into support bois (does not calculate an upper bound but a good estimate at 3 shield power per 4 SP)
   * @param move the boi
   * @return the potential total shield power the enemy can have next turn
   */
  static double potentialEnemyShieldPower(GameState move) {
    double shieldPower = currentEnemyShieldPower(move);
    double enemySP = move.data.p2Stats.bits;
    //apply the conversion of 3 shield power per 4 SP
    shieldPower += enemySP * 3.0 / 4.0;
    return shieldPower;
  }

  /**
   *
   * @param move the boi
   * @return the current SP on the enemy has on the board (what they would get if they refunded everything)
   */
  static double enemySPOnBoard(GameState move) {
    double enemySP = 0;
    List<Unit>[][] allUnits = move.allUnits;
    for (int x = 0; x < allUnits.length; x++) {
      List<Unit>[] row = allUnits[x];
      for (int y = 0; y < row.length; y++) {
        List<Unit> units = row[y];
        if (units.isEmpty() || move.isInfo(units.get(0).type)) {
          continue;
        }
        // there is a structure here
        Unit unit = units.get(0);
        if (unit.owner == PlayerId.Player2) {
          Config.UnitInformation info = unit.unitInformation;
          double cost = info.cost1.orElse(0);
          if (unit.upgraded) {
            enemySP += 0.90 * cost * unit.health / info.startHealth.getAsDouble();
          } else {
            enemySP += 0.97 * cost * unit.health / info.startHealth.getAsDouble();
          }
        }
      }
    }
    return enemySP;
  }

  static boolean pathHitsTargetEdge(List<Coords> path, int targetEdge) {
    for (Coords coord : path) {
      if (MapBounds.IS_ON_EDGE[targetEdge][coord.x][coord.y]) {
        return true;
      }
    }
    return false;
  }
}
