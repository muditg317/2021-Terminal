package com.c1games.terminal.myalgo;

import com.c1games.terminal.algo.Coords;
import com.c1games.terminal.algo.GameIO;
import com.c1games.terminal.algo.PlayerId;
import com.c1games.terminal.algo.map.CanSpawn;
import com.c1games.terminal.algo.map.GameState;
import com.c1games.terminal.algo.map.Unit;
import com.c1games.terminal.algo.units.UnitType;

import java.util.ArrayList;
import java.util.List;

public class Boom {
  public static boolean awaitingBoom = false;
  public static int turnsUntilBoom = -1;

  static void evaluate(GameState move, int expectedMpSpentPerTurn) {
    GameIO.debug().println("BOOM DECISION: ===========");
    float mp = move.data.p1Stats.bits;
    int MAX_EXTRAPOLATION_TURNS = Math.min(3, move.data.turnInfo.turnNumber / 5);
    if (Boom.turnsUntilBoom != 0) { //we WILL boom this turn. no need to check.
      boolean shouldStillBoom = false;
      int turns = 1;
      if (Boom.turnsUntilBoom == -99) {
        Boom.turnsUntilBoom = -1;
        turns = 0;
      }
      for (; turns <= MAX_EXTRAPOLATION_TURNS; turns++) {
        AttackBreakdown futureAttack = futureAttackThreshold(move, turns);
        float futureAttackThreshold = futureAttack.units.cost;
        float futureMP = extrapolateFutureMP(move, turns, expectedMpSpentPerTurn);
        GameIO.debug().println("futureAttackThreshold: " + futureAttackThreshold);
        GameIO.debug().println("futureMP: " + futureMP);
        if (futureMP >= futureAttackThreshold) {
          GameIO.debug().println("about to boom..." + mp + " / " + futureAttackThreshold + " reached -- expecting: " + futureMP +" in " + turns +" turns ||| started turn with: " + mp);
          Boom.awaitingBoom = true;
          Boom.turnsUntilBoom = turns;
          shouldStillBoom = true;
          break;
        }
      }

      if (!shouldStillBoom) {
        Boom.awaitingBoom = false;
        Boom.turnsUntilBoom = -1;
      }
    } //end boom decision
    GameIO.debug().println("awaitingBoom:" + Boom.awaitingBoom);
    GameIO.debug().println("turnsUntilBoom" + Boom.turnsUntilBoom);
  }

  /**
   * Does the Boom and sets awaitingBoom to false and turnsUntilBoom to -99
   * @return True if executed, false if not
   */
  static boolean execute(GameState move) {
    AttackBreakdown attackData = attackThreshold(move);
    String boomSide = attackData.location;
    UnitCounts attackUnits = attackData.units;
    int attackPoints = attackUnits.cost;
    int numInters = attackUnits.numInterceptors; //these are actuall scouts lul
    GameIO.debug().println("Going to boom right now");
    GameIO.debug().println("Cores:" + move.data.p1Stats.cores);
    Boom.clearBoomPath(move, boomSide);
    Boom.placeBoomLid(move, boomSide);

//    SpawnUtility.spawnInterceptors(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 23 : 4, 9)}, numInters);
//    SpawnUtility.spawnScouts(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 6 : 21, 7)}, (int) move.data.p1Stats.bits);

    SpawnUtility.spawnScouts(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 7 : 20, 6)}, numInters);
    SpawnUtility.spawnScouts(move, new Coords[]{new Coords(boomSide.equals("RIGHT") ? 6 : 21, 7)}, (int) move.data.p1Stats.bits);

    SpawnUtility.removeBuilding(move, new Coords(boomSide.equals("RIGHT") ? 6 : 21, 8));
    SpawnUtility.removeBuilding(move, new Coords(4, 11));
    Boom.awaitingBoom = false;
    Boom.turnsUntilBoom = -99;
    return true;
  }
  /**
   * returns true if the lid is successfully placed
   * @param boomSide
   * @return
   */
  static boolean placeBoomLid(GameState move, String boomSide) {
    List<Coords> toPlace = new ArrayList<>();
    for (int i = 0; i < Locations.boomLid_right.length; i++) {
      Coords closeLocation = Locations.boomLid_right[i];
      int x = boomSide.equals("RIGHT") ? closeLocation.x : (27 - closeLocation.x);
      Coords toClose = new Coords(x, closeLocation.y);
      if (toClose.y < 8) {
        if (move.canSpawn(toClose, Utility.WALL, 1) == CanSpawn.Yes) {
          toPlace.add(toClose);
        }
      } else if (toClose.y == 13) {
        if (move.canSpawn(toClose, Utility.WALL, 1) == CanSpawn.Yes) {
          toPlace.add(toClose);
        }
      } else {
        SpawnUtility.placeWalls(move, new Coords[]{toClose});
      }
    }
    final int[] numFactories = {0}; //TODO: I'm a bit confused about this legacy code... going to just make it place walls
    //final int[] numFactories = {(int) ((move.data.p1Stats.bits - toPlace.size()) / 9)};
    toPlace.stream().sorted((o1, o2) -> o1.y - o2.y).forEach(location -> {
      if (numFactories[0] > 0 && location.y < 11) {
        SpawnUtility.placeSupports(move, new Coords[]{location});
        numFactories[0]--;
      } else {
        SpawnUtility.placeWalls(move, new Coords[]{location});
      }
    });

    //now put down supports
    SpawnUtility.placeSupports(move, Locations.safeSupportLocations);
    SpawnUtility.removeBuildings(move, Locations.safeSupportLocations);
    return true;
  }

  /**
   * clears an attack path for a boom attack
   * @param side the side which to hit
   * @return whether the path was already clear
   */
  static boolean clearBoomPath(GameState move, String side) {
    boolean alreadyReady = true;
    for (int i = 0; i < Locations.boomPath_right.length; i++) {
      Coords openLocation = Locations.boomPath_right[i];
      int x = side.equals("RIGHT") ? openLocation.x : (27 - openLocation.x);
      Coords toOpen = new Coords(x, openLocation.y);
      alreadyReady = SpawnUtility.removeBuilding(move, toOpen) == 0 && alreadyReady;
    }
    return alreadyReady;
  }

  static void debugPrint() {
    GameIO.debug().println("awaitingBoom:" + Boom.awaitingBoom);
    GameIO.debug().println("turnsUntilBoom" + Boom.turnsUntilBoom);
  }

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

    int enemyHealth = (int) move.data.p2Stats.integrity;
    int minDamage = Math.min(10, enemyHealth);


    String weakerSide = leftCost < rightCost ? "LEFT" : "RIGHT";
    UnitCounts correctUnitCounts = leftCost < rightCost ? leftUnitCounts : rightUnitCounts;
    correctUnitCounts.numScouts = minDamage;
    correctUnitCounts.cost += minDamage;
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
   * @return UnitCounts with the number of scouts needed to only breach their wall
   */
  static UnitCounts enemyDefenseHeuristic(GameState move, String side) {
    int enemyHealth = (int) move.data.p2Stats.integrity; //min amount we need to hit them by
    //walls only matter in a few locations -> we include turret healths since they are "walls"

    ExpectedDefense cornerSummary = enemyCornerSummary(move, side);
    int scoutDamage = (int) move.config.unitInformation.get(UnitType.Scout.ordinal()).attackDamageTower.orElse(2);
    int scoutBaseHealth = (int) move.config.unitInformation.get(UnitType.Scout.ordinal()).startHealth.orElse(15);
    int expectedShielding = (int) (move.data.p1Stats.cores * 3.0 / 4.0);
    int scoutHealth = scoutBaseHealth + expectedShielding;

    int estScoutsKilled = (int) Math.ceil(cornerSummary.expectedScoutDamage / scoutHealth);
    int survivingScoutsNeeded = (int) cornerSummary.structureHealth / (scoutBaseHealth + 2 * scoutDamage);
    int boomScoutsNeeded = estScoutsKilled + survivingScoutsNeeded;
    GameIO.debug().println(String.format("cornerSummary structureHealth: %s\n cornerSummary expectedDamage: %s\n",
        cornerSummary.structureHealth, cornerSummary.expectedIntercepterDamage));
  //TODO: Fix boomScoutsNeeded. right now it is numInterceptors since i am too lazy :D
    return new UnitCounts(move, 0, boomScoutsNeeded, 0);
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
    GameIO.debug().println(String.format("enemyCornerSummary:  Side: %s\nEffective Wall Health: %s \nDamage taken: %d\n",
        side,effectiveWallHealth, effectiveTurretRating));
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
}