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
      currentMP -= currentMP%0.01;
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
    final int bias_term = 30; //TODO: Tunable
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
  /**
   * finds the optimal hook attack
   * @param move
   * @param availableSP
   * @param availableMP
   * @return
   */
  static HookAttack optimalDemolisherHook(GameState move, double availableSP, double availableMP, int minX, int maxX, int minY, int maxY, float minDamage) {
    final int xIncrement = 1;
    final int yIncrement = 1;
    Map<Utility.Pair<Coords, Integer>, Utility.Pair<Coords[], ExpectedDefense>> damages = new HashMap<>();

    Config.UnitInformation demolisherInfo = move.config.unitInformation.get(UnitType.Demolisher.ordinal());
    int numDemolishers = (int) (availableMP / demolisherInfo.cost2.orElse(3));

    if (numDemolishers == 0) {
      return null;
    }


    double demolisherDamage = demolisherInfo.attackDamageTower.orElse(8);
//    double totalDemoHealth = numDemolishers * demolisherHealth;

    for (int x = minX; x <= maxX; x = x == maxX ? maxX+1 : Math.min(x+xIncrement,maxX)) {
      for (int y = minY; y <= maxY; y = y == maxY ? maxY + 1 : Math.min(y + yIncrement, maxY)) {
        if (!MapBounds.ARENA[x][y]) {
          continue;
        }
        if (y == 11) {
          continue;
        }
        double wallCost = move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1);
        int wallsAvailable = (int) (availableSP / wallCost);
        if (wallsAvailable <= 0) {
          continue;
        }
        // Build list of walls we need to place for the hook
        List<Coords> neededWalls = new ArrayList<>();

        // Walls of the V
        for (int wallY = 2; wallY <= 13 && wallsAvailable > 0; ++wallY) {
          if (wallY == y-1) {
            wallY++;
          }
          Coords leftWall = new Coords(15-wallY,wallY);
          if (move.getWallAt(leftWall) == null) {
            neededWalls.add(leftWall);
            --wallsAvailable;
          }
          Coords rightWall = new Coords(12+wallY, wallY);
          if (move.getWallAt(rightWall) == null) {
            neededWalls.add(rightWall);
            --wallsAvailable;
          }
        }
        if (wallsAvailable <= 0) {
          continue;
        }

        // Walls in the Corners
        for (int wallX = 0; wallX <= 1 && wallsAvailable > 0; ++wallX) {
          Coords leftWall = new Coords(wallX,13);
          if (move.getWallAt(leftWall) == null) {
            neededWalls.add(leftWall);
            --wallsAvailable;
          }
          Coords rightWall = new Coords(27-wallX, 13);
          if (move.getWallAt(rightWall) == null) {
            neededWalls.add(rightWall);
            --wallsAvailable;
          }
        }
        if (wallsAvailable <= 0) {
          continue;
        }
        int placedWalls = neededWalls.size();
        for (int side = 0; side <= 1; ++side) {//0: target RIGHT, 1: target LEFT

          wallsAvailable += neededWalls.size() - placedWalls;
          neededWalls.subList(placedWalls, neededWalls.size()).clear();

          int topWallStartX = side == 0 ? (12+y) : (15-y);
          int wallBuildDir = side * 2 - 1;

          // Walls of the actual hook bar
          for (int wallX = topWallStartX; (side == 0 ? wallX > x : wallX < x) && wallsAvailable > 0; wallX += wallBuildDir) {
            Coords topWall = new Coords(wallX, y);
            if (move.getWallAt(topWall) == null) {
              neededWalls.add(topWall);
              --wallsAvailable;
            }
          }
          if (wallsAvailable <= 0) {
            continue;
          }

          Coords start = new Coords(x, y);
          List<Coords> path;
          try {
            path = move.pathfind(start, side);
          } catch (IllegalPathStartException e) {
            continue;
          }

          // add the path points along the horizontal travel of the hook path since path planner skips to end of hook
          List<Coords> initialPath = new ArrayList<>();
          for (int pathX = topWallStartX + wallBuildDir; side == 0 ? pathX >= x : pathX <= x; pathX += wallBuildDir) {
            initialPath.add(new Coords(pathX, y-1));
          }
          path.addAll(0, initialPath);

          double remainingSP = availableSP - neededWalls.size() * move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(1);
          double supportAmount = remainingSP > move.config.unitInformation.get(UnitType.Wall.ordinal()).cost1.orElse(4) ? remainingSP * 3 / 4 : 0;
          supportAmount += move.data.p1Units.support.size() * move.config.unitInformation.get(UnitType.Support.ordinal()).shieldPerUnit.orElse(3);
          double demolisherHealth = demolisherInfo.startHealth.orElse(5) + supportAmount;

          float spTaken = 0;
          float expectedDamage = 0;
          for (Coords pathPoint : path) {
            List<Unit> attackers = move.getAttackers(pathPoint);
            float damageDoneByTowers = 0;
            for (Unit attacker : attackers) {
              if (attacker.owner == PlayerId.Player2) {
                float towerDamage = (float) attacker.unitInformation.attackDamageWalker.orElse(6);
                expectedDamage += towerDamage;
                if (numDemolishers > 0) {
                  float damageDone = (float) Math.min(numDemolishers * demolisherDamage, attacker.health);
                  spTaken += (float) (damageDone / attacker.unitInformation.startHealth.orElse(2) * attacker.unitInformation.cost1.orElse(2) * 0.97f);
                  damageDoneByTowers += towerDamage;
                  if (damageDoneByTowers > demolisherHealth) {
                    numDemolishers--;
                    damageDoneByTowers = 0;
                  }
                }
              }
            }
          }

          damages.put(new Utility.Pair<>(start, side), new Utility.Pair<>((Coords[]) neededWalls.toArray(), new ExpectedDefense(move, (Coords[]) path.toArray(), spTaken, expectedDamage)));
        }
      }
    }


    Utility.Pair<Coords, Integer> bestAttack = null;
    ExpectedDefense bestED = new ExpectedDefense(move, null, minDamage, 0);

    for (Map.Entry<Utility.Pair<Coords, Integer>, Utility.Pair<Coords[], ExpectedDefense>> entry : damages.entrySet()) {
      if (entry.getValue().value.structureHealth > bestED.structureHealth) {
        bestAttack = entry.getKey();
        bestED = entry.getValue().value;
      }
    }
    if (bestAttack == null) {
      return null;
    }



    Coords[] demolisherLocations = new Coords[(int) (availableMP / demolisherInfo.cost2.orElse(3))];
    Coords demoLoc = new Coords(13 + bestAttack.value, 0);
    Arrays.fill(demolisherLocations, demoLoc);

    return new HookAttack(move, damages.get(bestAttack).key,null,new Coords[]{}, new Coords[]{},new Coords[]{}, demolisherLocations, bestED);
  }
}
