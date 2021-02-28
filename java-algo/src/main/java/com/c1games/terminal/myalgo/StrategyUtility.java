package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Config;
import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.MapBounds;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;

import java.util.*;
import java.util.stream.Collectors;

import static com.c1games.terminal.algo.map.GameState.TowerUnitCategory;

public class StrategyUtility {

  static double mpCapacity(GameState move, int turnNumber) {
    float baseMPIncome = move.config.resources.bitsPerRound + move.config.resources.bitGrowthRate * turnNumber / move.config.resources.turnIntervalForBitSchedule;
    double mpCapacity = baseMPIncome * 4;
    return mpCapacity;
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
    double enemyMPCapacity = mpCapacity(move, turnNumber);
    double enemyMPPercentCapacity = move.data.p2Stats.bits / enemyMPCapacity;
    if (enemyMPPercentCapacity < 0.7) {
      return 0; //dont do it
    }
    int neededDefenseSpending = neededDefenseSpending(move);
    int scoutRushDefense = (neededDefenseSpending - sp) / 10; // TODO: i added /2 because inters do more damage than turrets (spending is based on turrets)
    if (move.data.p2Stats.bits < 2 * move.config.unitInformation.get(UnitType.Demolisher.ordinal()).cost2.orElse(3)) {
      scoutRushDefense = 0;
    }

    scoutRushDefense = Math.min(Math.max(scoutRushDefense, 0), mp);
    scoutRushDefense = 0; //just doing this for now.
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
    float defenseRating = (int) DefenseUtility.ourDefenseRating(move);



    //we want at least 1 rating per 1 MP they have - FOLLOW IS OLD TUNE
    // double enemyAttackRating = 1.7 * enemyMP + (0 * enemyMPIncome) - (ourHealth * 0.1) + 15;


    int maxEnemyScoutRushHealth = maxEnemyScoutRushHealth(move);
    final int bias_term = 20; //TODO: Tunable (more means we need more defense as a base)
    int possibleRemainingScoutRushHealth = (int) (maxEnemyScoutRushHealth + bias_term - 2 * defenseRating);
    /*
    We divide by 15
    since 2 SP (unupgraded) tower deals ~30 dmg
    and 6 SP tower deals ~ 95 dmg
    thats about 15 damage per SP
     */
    double turretCost = move.config.unitInformation.get(UnitType.Turret.ordinal()).cost1.orElse(2);
    int scalingFactor = move.data.turnInfo.turnNumber / 5; //scale with the number of turns
    return (int) (Math.ceil((possibleRemainingScoutRushHealth / 10.0) / turretCost) * turretCost) + scalingFactor;
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
  static double numAffordableWithBudget(GameState move, UnitType type, boolean upgrade, double budget) {
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
    //apply the conversion of 1 to 1 assuming as average
    shieldPower += enemySP;
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

  static double totalEnemySp(GameState move) {
    return enemySPOnBoard(move) + move.data.p2Stats.cores;
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
   * copied and modified from game state
   * @param move
   * @param coords
   * @return
   */
  static Map<Unit, Coords> getTowerLocations(GameState move, Coords coords, double walkerRange) {
    Map<Unit, Coords> attackers = new HashMap<>();
    if (!MapBounds.inArena(coords)) {
      GameIO.debug().println("Checking attackers out of bounds! " + coords);
    }

    float maxRange = 0;
    float maxGetHit = 0;
    for (Config.UnitInformation uinfo : move.config.unitInformation) {
      if (uinfo.unitCategory.orElse(999) == TowerUnitCategory && (uinfo.attackRange.orElse(0) > maxRange || uinfo.shieldRange.orElse(0) > maxRange)) {
        maxRange = (float) Math.max(uinfo.attackRange.orElse(0), uinfo.shieldRange.orElse(0));
      }
      if (uinfo.getHitRadius.orElse(0) > maxGetHit) {
        maxGetHit = (float)uinfo.getHitRadius.orElse(0);
      }
    }

    int max = (int)Math.ceil(maxRange);

    for (int x = coords.x - max; x <= coords.x + max; x++) {
      for (int y = coords.y - max; y <= coords.y + max; y++) {
        Coords c = new Coords(x,y);
        if (MapBounds.inArena(c)) {
          Unit unit = move.getWallAt(c);
          double distance = c.distance(coords);
          if (unit != null && (distance < walkerRange || distance <= unit.unitInformation.attackRange.orElse(0) + maxGetHit || distance <= unit.unitInformation.shieldRange.orElse(0) + maxGetHit)) {
            attackers.put(unit, c);
          }
        }
      }
    }

    return attackers;
  }

  static GameState predictGameState(GameState move, List<Unit>[][] enemyBaseHistory) {
    GameState prediction = Utility.duplicateState(move);
    for (int i = 13; i < 41; i++) {
      for (int j = 0; j <= 14 - i%2; j++) {
        int x = j + (i-13)/2;
        int y = i - x;
        List<Unit> history = enemyBaseHistory[x][y];
        boolean onCorner = Utility.onEnemyCorner(x,y);
        boolean pickBest = onCorner;
        int maxUnits = onCorner ? 5 : 2;
        List<Unit> relevantHistory = history.stream().skip(Math.max(0, history.size() - maxUnits)).limit(maxUnits).collect(Collectors.toList());
        if (y > 13 && !relevantHistory.isEmpty()) {
          if (prediction.allUnits[x][y].isEmpty()) {
            Unit newUnit = relevantHistory.get(0);
            if (newUnit != null) {
              newUnit = new Unit(newUnit.type, newUnit.health, newUnit.id + "hi", newUnit.owner, prediction.config);
              if (newUnit.upgraded) newUnit.upgrade();
            }
            Unit[] detectedUnit = new Unit[]{newUnit};
            relevantHistory.stream().skip(1).forEach(unit -> {
              if ((!pickBest && detectedUnit[0] == null) || unit == null) {
                if (!pickBest) detectedUnit[0] = null;
                return;
              }
              if (unit.type == UnitType.Turret) {
                Unit temp = new Unit(unit.type, unit.health, unit.id+"hi", unit.owner, prediction.config);
                if (unit.upgraded || (detectedUnit[0] != null && detectedUnit[0].type == UnitType.Turret && detectedUnit[0].upgraded)) temp.upgrade();
                detectedUnit[0] = temp;
              } else if (unit.type == UnitType.Support && (detectedUnit[0] == null || detectedUnit[0].type != UnitType.Turret)) {
                Unit temp = new Unit(unit.type, unit.health, unit.id+"hi", unit.owner, prediction.config);
                if (unit.upgraded || (detectedUnit[0] != null && detectedUnit[0].type == UnitType.Support && detectedUnit[0].upgraded)) temp.upgrade();
                detectedUnit[0] = temp;
              } else if ((detectedUnit[0] == null || (detectedUnit[0].type != UnitType.Turret && detectedUnit[0].type != UnitType.Support))) {
                Unit temp = new Unit(unit.type, unit.health, unit.id+"hi", unit.owner, prediction.config);
                if (unit.upgraded || (detectedUnit[0] != null && detectedUnit[0].type == UnitType.Support && detectedUnit[0].upgraded)) temp.upgrade();
                detectedUnit[0] = temp;
              }
            });
            if (detectedUnit[0] != null) {
              Unit temp = new Unit(detectedUnit[0].type, detectedUnit[0].health, detectedUnit[0].id+"hi", detectedUnit[0].owner, prediction.config);
              if (detectedUnit[0].upgraded) temp.upgrade();
              prediction.allUnits[x][y].add(temp);
            }
          } else if (!prediction.allUnits[x][y].get(0).upgraded) {
            Unit predictedUnit = prediction.allUnits[x][y].get(0);
            relevantHistory.forEach(unit -> {
              if (unit != null && unit.type == predictedUnit.type && unit.upgraded && !predictedUnit.upgraded) {
                predictedUnit.upgrade();
              }
            });
          }
        }
      }
    }
    GameIO.debug().println("PREDICTED GAME STATE=========");
    Utility.printGameBoard(prediction.allUnits);
    return prediction;
  }

}
