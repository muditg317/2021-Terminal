package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;

import java.util.List;

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
    return attackThreshold(move);
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
    //intersNeeded = Math.min(intersNeeded, 18);
    int scoutsNeeded = (int) Math.ceil(cornerSummary.expectedScoutDamage / 15.0 + enemyHealth);

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
          if (attacker.upgraded) {
            effectiveTurretRating += attacker.unitInformation.upgrade.orElse(new Config.UnitInformation()).attackDamageWalker.orElse(20); //TODO fix these values (i think these are damage values of the turret)
          } else {
            effectiveTurretRating += attacker.unitInformation.attackDamageWalker.orElse(6);
          }
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
      currentMP -= currentMP%0.01;
      currentMP += baseMPIncome - expectedBaseCostPerTurn;
    }
    return currentMP;
  }

  /**
   * Based on the current board state, enemy MP, and our defenses returns the number of cores it thinks we need to spend into defenses
   * @param move  the game state to inspect
   * @return the number of cores we should spend in defenses
   */
  static int defenseHeuristic(GameState move) {
    float enemyMP = move.data.p2Stats.bits;
    float ourHealth = move.data.p1Stats.integrity;
    float baseMPIncome = move.config.resources.bitsPerRound + move.config.resources.bitGrowthRate * (move.data.turnInfo.turnNumber) / move.config.resources.turnIntervalForBitSchedule;
    float enemyMPIncome = baseMPIncome;
    float defenseRating = calculateDefenseRating(move);


    // TODO: These are tunable parameters
    //we want at least 1 rating per 1 MP they have
    double enemyAttackRating = 1.7 * enemyMP + (0 * enemyMPIncome) - (ourHealth * 0.1) + 15;
    // TODO: 15 is the base expected enemy attack even if we see none (like a bias value)
    return (int) Math.ceil(enemyAttackRating - defenseRating);
  }

  /**
   * Calculates some rating of our defense...
   * @param move the game state
   * @return the defensive rating TODO: what's the range
   */
  private static float calculateDefenseRating(GameState move) {

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
          if (unit.upgraded) {
            // uses cost[0] to get SP of cost. cost[1] is MP cost (0 for structures)
            unitValue += unit.unitInformation.upgrade.orElse(new Config.UnitInformation()).cost()[0] * unit.health / unit.unitInformation.upgrade.orElse(new Config.UnitInformation()).startHealth.orElse(100);
          } else {
            unitValue += unit.unitInformation.cost()[0] * unit.health / unit.unitInformation.startHealth.orElse(20);
          }
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
}
